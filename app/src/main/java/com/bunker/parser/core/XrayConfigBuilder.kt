package com.bunker.parser.core

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.URLDecoder

object XrayConfigBuilder {
    private val gson = Gson()

    fun buildConfig(rawUri: String): String? {
        val trimmed = rawUri.trim()
        val scheme = trimmed.substringBefore("://").lowercase()
        val outbound = when (scheme) {
            "vless" -> buildVlessOutbound(trimmed)
            "vmess" -> buildVmessOutbound(trimmed)
            "trojan" -> buildTrojanOutbound(trimmed)
            "ss" -> buildShadowsocksOutbound(trimmed)
            else -> null
        } ?: return null

        val root = JsonObject()
        val log = JsonObject().apply {
            addProperty("loglevel", "none")
        }
        root.add("log", log)

        val outbounds = JsonArray().apply {
            add(outbound)
        }
        root.add("outbounds", outbounds)

        return gson.toJson(root)
    }

    private fun buildVlessOutbound(uri: String): JsonObject? {
        return try {
            val (base, _) = splitBaseAndFrag(uri)
            val withoutScheme = base.substringAfter("://")
            val userInfo = withoutScheme.substringBefore("@")
            val hostAndRest = withoutScheme.substringAfter("@")
            val hostPortStr = hostAndRest.substringBefore("?").substringBefore("/")
            val queryStr = if (hostAndRest.contains("?")) hostAndRest.substringAfter("?").substringBefore("#") else ""

            val (host, port) = extractHostAndPort(hostPortStr, 443)
            val userId = URLDecoder.decode(userInfo, "UTF-8").trim()
            if (userId.isEmpty() || host.isEmpty()) return null

            val q = parseQuery(queryStr)
            val transport = (q["type"] ?: q["net"] ?: "tcp").lowercase()
            val security = (q["security"] ?: "none").lowercase()
            val flow = q["flow"].orEmpty()

            val outbound = JsonObject().apply {
                addProperty("tag", "proxy")
                addProperty("protocol", "vless")
            }

            // Xray Core VLESS requires vnext -> users
            val user = JsonObject().apply {
                addProperty("id", userId)
                addProperty("encryption", q["encryption"] ?: "none")
                if (flow.isNotBlank()) {
                    addProperty("flow", flow)
                }
            }

            val server = JsonObject().apply {
                addProperty("address", host)
                addProperty("port", port)
                val users = JsonArray().apply { add(user) }
                add("users", users)
            }

            val settings = JsonObject().apply {
                val vnext = JsonArray().apply { add(server) }
                add("vnext", vnext)
            }
            outbound.add("settings", settings)

            val stream = buildStreamSettings(
                transport = transport,
                security = security,
                host = host,
                port = port,
                q = q
            )
            outbound.add("streamSettings", stream)

            outbound
        } catch (_: Exception) {
            null
        }
    }

    private fun buildVmessOutbound(uri: String): JsonObject? {
        return try {
            val b64 = uri.substringAfter("://").substringBefore("#").trim()
            val decoded = decodeBase64Safe(b64) ?: return null
            val obj = gson.fromJson(decoded, JsonObject::class.java) ?: return null

            val host = obj.get("add")?.asString?.trim().orEmpty()
            val port = obj.get("port")?.asString?.toIntOrNull() ?: 443
            val id = obj.get("id")?.asString?.trim().orEmpty()
            if (host.isEmpty() || id.isEmpty()) return null

            val net = obj.get("net")?.asString?.trim()?.lowercase() ?: "tcp"
            val tls = obj.get("tls")?.asString?.trim()?.lowercase() ?: "none"
            val path = obj.get("path")?.asString?.trim().orEmpty()
            val hostHeader = obj.get("host")?.asString?.trim().orEmpty()
            val sni = obj.get("sni")?.asString?.trim().orEmpty().ifEmpty { hostHeader }.ifEmpty { host }

            val outbound = JsonObject().apply {
                addProperty("tag", "proxy")
                addProperty("protocol", "vmess")
            }

            // Xray Core VMess requires vnext -> users
            val user = JsonObject().apply {
                addProperty("id", id)
                addProperty("alterId", obj.get("aid")?.asString?.toIntOrNull() ?: 0)
                addProperty("security", obj.get("scy")?.asString ?: "auto")
            }

            val server = JsonObject().apply {
                addProperty("address", host)
                addProperty("port", port)
                val users = JsonArray().apply { add(user) }
                add("users", users)
            }

            val settings = JsonObject().apply {
                val vnext = JsonArray().apply { add(server) }
                add("vnext", vnext)
            }
            outbound.add("settings", settings)

            val q = HashMap<String, String>()
            if (path.isNotEmpty()) q["path"] = path
            if (hostHeader.isNotEmpty()) q["host"] = hostHeader
            if (sni.isNotEmpty()) q["sni"] = sni

            val stream = buildStreamSettings(
                transport = net,
                security = if (tls == "tls") "tls" else "none",
                host = host,
                port = port,
                q = q
            )
            outbound.add("streamSettings", stream)

            outbound
        } catch (_: Exception) {
            null
        }
    }

    private fun buildTrojanOutbound(uri: String): JsonObject? {
        return try {
            val (base, _) = splitBaseAndFrag(uri)
            val withoutScheme = base.substringAfter("://")
            val password = withoutScheme.substringBefore("@")
            val hostAndRest = withoutScheme.substringAfter("@")
            val hostPortStr = hostAndRest.substringBefore("?").substringBefore("/")
            val queryStr = if (hostAndRest.contains("?")) hostAndRest.substringAfter("?").substringBefore("#") else ""

            val (host, port) = extractHostAndPort(hostPortStr, 443)
            val pass = URLDecoder.decode(password, "UTF-8").trim()
            if (pass.isEmpty() || host.isEmpty()) return null

            val q = parseQuery(queryStr)
            val transport = (q["type"] ?: q["net"] ?: "tcp").lowercase()
            val security = (q["security"] ?: "tls").lowercase()

            val outbound = JsonObject().apply {
                addProperty("tag", "proxy")
                addProperty("protocol", "trojan")
            }

            // Xray Core Trojan requires servers array
            val server = JsonObject().apply {
                addProperty("address", host)
                addProperty("port", port)
                addProperty("password", pass)
            }

            val settings = JsonObject().apply {
                val servers = JsonArray().apply { add(server) }
                add("servers", servers)
            }
            outbound.add("settings", settings)

            val stream = buildStreamSettings(
                transport = transport,
                security = security,
                host = host,
                port = port,
                q = q
            )
            outbound.add("streamSettings", stream)

            outbound
        } catch (_: Exception) {
            null
        }
    }

    private fun buildShadowsocksOutbound(uri: String): JsonObject? {
        return try {
            val (base, _) = splitBaseAndFrag(uri)
            val body = base.substringAfter("://")
            val host: String
            val port: Int
            val method: String
            val password: String

            if (body.contains("@")) {
                val userinfo = body.substringBefore("@")
                val hostPort = body.substringAfter("@").substringBefore("/").substringBefore("?")
                val hp = extractHostAndPort(hostPort, 8388)
                host = hp.first
                port = hp.second

                val creds = decodeBase64Safe(userinfo) ?: userinfo
                if (!creds.contains(":")) return null
                method = creds.substringBefore(":")
                password = creds.substringAfter(":")
            } else {
                val decoded = decodeBase64Safe(body) ?: return null
                if (!decoded.contains("@")) return null
                val userinfo = decoded.substringBefore("@")
                val hostPort = decoded.substringAfter("@").substringBefore("/").substringBefore("?")
                val hp = extractHostAndPort(hostPort, 8388)
                host = hp.first
                port = hp.second

                if (!userinfo.contains(":")) return null
                method = userinfo.substringBefore(":")
                password = userinfo.substringAfter(":")
            }

            if (host.isEmpty() || method.isEmpty()) return null

            val outbound = JsonObject().apply {
                addProperty("tag", "proxy")
                addProperty("protocol", "shadowsocks")
            }

            val server = JsonObject().apply {
                addProperty("address", host)
                addProperty("port", port)
                addProperty("method", method)
                addProperty("password", password)
            }

            val settings = JsonObject().apply {
                val servers = JsonArray().apply { add(server) }
                add("servers", servers)
            }
            outbound.add("settings", settings)

            val stream = JsonObject().apply {
                addProperty("network", "tcp")
            }
            outbound.add("streamSettings", stream)

            outbound
        } catch (_: Exception) {
            null
        }
    }

    private fun buildStreamSettings(
        transport: String,
        security: String,
        host: String,
        port: Int,
        q: Map<String, String>
    ): JsonObject {
        val stream = JsonObject()
        val network = when (transport) {
            "ws", "websocket" -> "ws"
            "httpupgrade" -> "httpupgrade"
            "grpc" -> "grpc"
            "xhttp", "splithttp" -> "xhttp"
            "kcp", "mkcp" -> "kcp"
            else -> "tcp"
        }
        stream.addProperty("network", network)

        val rawSni = q["sni"]?.trim().orEmpty()
        val rawHost = q["host"]?.trim().orEmpty()
        val sni = if (rawSni.isNotEmpty()) rawSni else if (rawHost.isNotEmpty()) rawHost else host
        val path = q["path"]?.ifEmpty { "/" } ?: "/"
        val hostHeader = if (rawHost.isNotEmpty()) rawHost else if (rawSni.isNotEmpty()) rawSni else host

        when (network) {
            "ws" -> {
                val ws = JsonObject().apply {
                    addProperty("path", path)
                    if (hostHeader.isNotEmpty()) {
                        addProperty("host", hostHeader)
                        val headers = JsonObject().apply {
                            addProperty("Host", hostHeader)
                        }
                        add("headers", headers)
                    }
                }
                stream.add("wsSettings", ws)
            }
            "httpupgrade" -> {
                val hu = JsonObject().apply {
                    addProperty("path", path)
                    if (hostHeader.isNotEmpty()) addProperty("host", hostHeader)
                }
                stream.add("httpupgradeSettings", hu)
            }
            "grpc" -> {
                val serviceName = q["servicename"] ?: q["serviceName"] ?: path.trim('/')
                val grpc = JsonObject().apply {
                    if (serviceName.isNotEmpty()) addProperty("serviceName", serviceName)
                    if (hostHeader.isNotEmpty()) addProperty("authority", hostHeader)
                    if (q["mode"] == "multi") addProperty("multiMode", true)
                }
                stream.add("grpcSettings", grpc)
            }
            "xhttp" -> {
                val xhttp = JsonObject().apply {
                    addProperty("path", path)
                    addProperty("mode", q["mode"] ?: "auto")
                    if (hostHeader.isNotEmpty()) addProperty("host", hostHeader)
                }
                stream.add("xhttpSettings", xhttp)
            }
            "tcp" -> {
                if (q["headertype"] == "http") {
                    val tcp = JsonObject()
                    val hdr = JsonObject().apply {
                        addProperty("type", "http")
                        val req = JsonObject().apply {
                            val pArr = JsonArray().apply { add(path) }
                            add("path", pArr)
                            if (hostHeader.isNotEmpty()) {
                                val hObj = JsonObject().apply {
                                    val hArr = JsonArray().apply { add(hostHeader) }
                                    add("Host", hArr)
                                }
                                add("headers", hObj)
                            }
                        }
                        add("request", req)
                    }
                    tcp.add("header", hdr)
                    stream.add("tcpSettings", tcp)
                }
            }
        }

        when (security) {
            "reality" -> {
                stream.addProperty("security", "reality")
                val reality = JsonObject().apply {
                    addProperty("serverName", sni)
                    addProperty("fingerprint", q["fp"] ?: "chrome")
                    addProperty("show", false)
                    addProperty("publicKey", q["pbk"].orEmpty())
                    addProperty("shortId", q["sid"].orEmpty())
                    if (q["spx"]?.isNotEmpty() == true) {
                        addProperty("spiderX", q["spx"])
                    }
                }
                stream.add("realitySettings", reality)
            }
            "tls", "xtls" -> {
                stream.addProperty("security", "tls")
                val tls = JsonObject().apply {
                    addProperty("serverName", sni)
                    addProperty("allowInsecure", true)
                    val fp = q["fp"] ?: "chrome"
                    if (fp.isNotEmpty()) addProperty("fingerprint", fp)
                    val alpnStr = q["alpn"].orEmpty()
                    if (alpnStr.isNotEmpty()) {
                        val alpnArr = JsonArray()
                        alpnStr.split(",").forEach { alpnArr.add(it.trim()) }
                        add("alpn", alpnArr)
                    }
                }
                stream.add("tlsSettings", tls)
            }
            else -> {
                stream.addProperty("security", "none")
            }
        }

        return stream
    }

    private fun splitBaseAndFrag(raw: String): Pair<String, String> {
        val hashIdx = raw.indexOf('#')
        return if (hashIdx >= 0) {
            raw.substring(0, hashIdx) to raw.substring(hashIdx + 1)
        } else {
            raw to ""
        }
    }

    private fun extractHostAndPort(hostPortStr: String, defaultPort: Int): Pair<String, Int> {
        val trimmed = hostPortStr.trim()
        return if (trimmed.startsWith("[")) {
            val closeIdx = trimmed.indexOf("]")
            val host = trimmed.substring(1, closeIdx)
            val rest = trimmed.substring(closeIdx + 1)
            val port = rest.substringAfter(":", "").toIntOrNull() ?: defaultPort
            host to port
        } else if (trimmed.contains(":")) {
            val host = trimmed.substringBeforeLast(":")
            val port = trimmed.substringAfterLast(":").toIntOrNull() ?: defaultPort
            host to port
        } else {
            trimmed to defaultPort
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        query.split("&").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].lowercase().trim()
                val rawVal = parts[1].trim()
                val value = if (key in listOf("pbk", "sid", "password", "key", "cert", "ech")) {
                    rawVal
                } else {
                    try { URLDecoder.decode(rawVal, "UTF-8") } catch (_: Exception) { rawVal }
                }
                result[key] = value
            }
        }
        return result
    }

    private fun decodeBase64Safe(input: String): String? {
        val sanitized = input.trim().replace('-', '+').replace('_', '/')
        val pad = sanitized.length % 4
        val padded = if (pad > 0) sanitized + "=".repeat(4 - pad) else sanitized
        return try {
            val bytes = Base64.decode(padded, Base64.DEFAULT)
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
