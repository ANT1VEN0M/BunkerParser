package com.bunker.parser.core

import android.util.Base64
import com.bunker.parser.model.CheckItem
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URLDecoder
import java.net.URLEncoder

object UriParser {
    private val gson = Gson()

    fun extractItems(text: String): List<CheckItem> {
        return text.lines()
            .mapIndexedNotNull { index, line -> parseLine(index + 1, line) }
    }

    fun parseLine(lineNo: Int, rawLine: String): CheckItem? {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty() || (trimmed.startsWith("#") && !trimmed.contains("://"))) {
            return null
        }

        val scheme = trimmed.substringBefore("://").lowercase()
        return when (scheme) {
            "vless", "trojan", "hysteria2", "hy2" -> parseStandardUri(lineNo, trimmed, scheme)
            "vmess" -> parseVmessUri(lineNo, trimmed)
            "ss" -> parseShadowsocksUri(lineNo, trimmed)
            else -> null
        }
    }

    private fun parseStandardUri(lineNo: Int, raw: String, scheme: String): CheckItem? {
        return try {
            val (base, frag) = splitBaseAndFrag(raw)
            val uriWithoutScheme = base.substringAfter("://")
            val userInfoAndRest = uriWithoutScheme.substringAfter("@")
            val hostAndPort = userInfoAndRest.substringBefore("?").substringBefore("/")

            val (host, port) = extractHostAndPort(hostAndPort, defaultPort = 443)
            if (host.isEmpty() || port <= 0) return null

            val remark = if (frag.isNotEmpty()) try { URLDecoder.decode(frag, "UTF-8") } catch (_: Exception) { frag } else ""
            CheckItem(
                seq = lineNo,
                lineNo = lineNo,
                rawUri = raw,
                scheme = scheme,
                host = host,
                port = port,
                baseUri = base,
                origRemark = remark
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseVmessUri(lineNo: Int, raw: String): CheckItem? {
        return try {
            val b64 = raw.substringAfter("://").trim()
            val decoded = decodeBase64Safe(b64) ?: return null
            val obj = gson.fromJson(decoded, JsonObject::class.java) ?: return null

            val host = obj.get("add")?.asString?.trim().orEmpty()
            val port = obj.get("port")?.asString?.toIntOrNull() ?: 443
            val ps = obj.get("ps")?.asString.orEmpty()

            if (host.isEmpty()) return null

            CheckItem(
                seq = lineNo,
                lineNo = lineNo,
                rawUri = raw,
                scheme = "vmess",
                host = host,
                port = port,
                baseUri = raw,
                origRemark = ps
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseShadowsocksUri(lineNo: Int, raw: String): CheckItem? {
        return try {
            val (base, frag) = splitBaseAndFrag(raw)
            val body = base.substringAfter("://")
            val remark = if (frag.isNotEmpty()) try { URLDecoder.decode(frag, "UTF-8") } catch (_: Exception) { frag } else ""

            val host: String
            val port: Int

            if (body.contains("@")) {
                val hostPort = body.substringAfter("@").substringBefore("/").substringBefore("?")
                val hp = extractHostAndPort(hostPort, defaultPort = 8388)
                host = hp.first
                port = hp.second
            } else {
                val decoded = decodeBase64Safe(body) ?: return null
                if (decoded.contains("@")) {
                    val hp = extractHostAndPort(decoded.substringAfter("@"), defaultPort = 8388)
                    host = hp.first
                    port = hp.second
                } else {
                    return null
                }
            }

            if (host.isEmpty()) return null
            CheckItem(
                seq = lineNo,
                lineNo = lineNo,
                rawUri = raw,
                scheme = "ss",
                host = host,
                port = port,
                baseUri = base,
                origRemark = remark
            )
        } catch (_: Exception) {
            null
        }
    }

    fun rename(item: CheckItem, newRemark: String): String {
        return if (item.scheme == "vmess") {
            try {
                val b64 = item.baseUri.substringAfter("://").trim()
                val decoded = decodeBase64Safe(b64) ?: return item.rawUri
                val obj = gson.fromJson(decoded, JsonObject::class.java) ?: return item.rawUri
                obj.addProperty("ps", newRemark)
                val jsonStr = gson.toJson(obj)
                val encoded = Base64.encodeToString(jsonStr.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                "vmess://$encoded"
            } catch (_: Exception) {
                item.rawUri
            }
        } else {
            val sanitizedBase = sanitizeBaseUri(item.baseUri)
            val encodedFrag = URLEncoder.encode(newRemark, "UTF-8").replace("+", "%20")
            "$sanitizedBase#$encodedFrag"
        }
    }

    private fun sanitizeBaseUri(baseUri: String): String {
        if (!baseUri.contains("extra=")) return baseUri
        return try {
            val prefix = baseUri.substringBefore("extra=")
            val rest = baseUri.substringAfter("extra=")
            val rawExtraEncoded = rest.substringBefore("&")
            val suffix = if (rest.contains("&")) "&" + rest.substringAfter("&") else ""

            val decodedExtra = URLDecoder.decode(rawExtraEncoded, "UTF-8")
            val sanitized = XrayConfigBuilder.sanitizeExtraJson(decodedExtra)
            if (sanitized != null) {
                val newExtra = URLEncoder.encode(gson.toJson(sanitized), "UTF-8")
                "$prefix" + "extra=" + newExtra + suffix
            } else {
                // If extra was invalid, remove it
                if (prefix.endsWith("?") || prefix.endsWith("&")) {
                    prefix.dropLast(1) + suffix
                } else {
                    prefix + suffix
                }
            }
        } catch (_: Exception) {
            baseUri
        }
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
