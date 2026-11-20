package com.bunker.parser.core

import com.bunker.parser.model.CheckItem
import com.bunker.parser.model.CheckResult
import com.bunker.parser.model.CheckerStats
import com.bunker.parser.model.ParseMode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class CheckEngine {
    private val _stats = MutableStateFlow(CheckerStats())
    val stats: StateFlow<CheckerStats> = _stats.asStateFlow()

    private val _results = MutableStateFlow<List<CheckResult>>(emptyList())
    val results: StateFlow<List<CheckResult>> = _results.asStateFlow()

    private val isRunningFlag = AtomicBoolean(false)
    private val isPausedFlag = AtomicBoolean(false)
    private var engineJob: Job? = null

    // DNS Cache to eliminate repeated DNS queries across thousands of nodes
    private val dnsCache = ConcurrentHashMap<String, InetAddress?>()

    fun isRunning(): Boolean = isRunningFlag.get()
    fun isPaused(): Boolean = isPausedFlag.get()

    fun pause() {
        if (isRunningFlag.get()) {
            isPausedFlag.set(true)
            _stats.value = _stats.value.copy(isPaused = true)
        }
    }

    fun resume() {
        if (isRunningFlag.get()) {
            isPausedFlag.set(false)
            _stats.value = _stats.value.copy(isPaused = false)
        }
    }

    fun stop() {
        isRunningFlag.set(false)
        isPausedFlag.set(false)
        engineJob?.cancel()
        _stats.value = _stats.value.copy(isRunning = false, isPaused = false)
    }

    private fun resolveHostSafely(host: String): InetAddress? {
        if (host.isEmpty()) return null
        return dnsCache.computeIfAbsent(host) {
            try {
                InetAddress.getByName(it)
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun probeTcpPing(host: String, port: Int, timeoutMs: Int): Long {
        val inetAddr = resolveHostSafely(host) ?: return -1L
        val started = System.currentTimeMillis()
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.reuseAddress = true
            socket.tcpNoDelay = true
            // Immediately reclaim file descriptors upon close, preventing TIME_WAIT exhaustion in kernel
            socket.setSoLinger(true, 0)
            socket.connect(InetSocketAddress(inetAddr, port), timeoutMs)
            System.currentTimeMillis() - started
        } catch (_: Throwable) {
            -1L
        } finally {
            try { socket?.close() } catch (_: Throwable) {}
        }
    }

    suspend fun start(
        items: List<CheckItem>,
        mode: ParseMode,
        minServices: Int = 1,
        workers: Int = 32,
        timeoutSec: Float = 2.5f,
        onlyFactoryPorts: Boolean = false
    ) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        stop()

        val allowedPorts = setOf(443, 53, 80, 8443)
        val targetItems = if (onlyFactoryPorts) {
            items.filter { it.port in allowedPorts }
        } else {
            items
        }

        if (targetItems.isEmpty()) {
            _stats.value = CheckerStats(
                total = items.size,
                processed = 0,
                alive = 0,
                dead = items.size,
                isRunning = false,
                currentBatch = "Отсеяно: ни один узел не использует порты 443 или 53"
            )
            return@withContext
        }

        isRunningFlag.set(true)
        isPausedFlag.set(false)
        val verifiedList = mutableListOf<CheckResult>()
        _results.value = emptyList()

        val total = targetItems.size
        val processedCounter = AtomicInteger(0)
        val aliveCounter = AtomicInteger(0)
        val deadCounter = AtomicInteger(0)

        val startTime = System.currentTimeMillis()
        val workerPoolSize = workers.coerceIn(4, 64)

        _stats.value = CheckerStats(
            total = total,
            processed = 0,
            alive = 0,
            dead = 0,
            ratePerSec = 0f,
            isRunning = true,
            isPaused = false,
            currentBatch = "Старт Turbo проверки Xray Core ($workerPoolSize потоков, режим ${mode.title})..."
        )

        engineJob = launch {
            val channel = Channel<CheckItem>(capacity = Channel.BUFFERED)
            val listLock = Any()
            var lastUiUpdateMs = System.currentTimeMillis()

            // Producer: sends items into channel sequentially
            launch(Dispatchers.IO) {
                for (item in targetItems) {
                    if (!isRunningFlag.get()) break
                    channel.send(item)
                }
                channel.close()
            }

            // Fixed worker pool: only workerPoolSize coroutines run simultaneously
            val workerJobs = (1..workerPoolSize).map {
                launch(Dispatchers.IO) {
                    for (item in channel) {
                        if (!isRunningFlag.get()) break

                        // Pause gate: gently suspend if user clicked pause
                        while (isPausedFlag.get()) {
                            if (!isRunningFlag.get()) break
                            delay(250)
                        }
                        if (!isRunningFlag.get()) break

                        val configJson = XrayConfigBuilder.buildConfig(item.rawUri)
                        var isAlive = false
                        var finalPing = 0L
                        val services = mutableListOf<String>()

                        if (configJson != null) {
                            // ── ШАГ 1: Сверхбыстрый TCP pre-check (400ms) ──
                            val preTcp = probeTcpPing(item.host, item.port, 400)
                            if (preTcp > 0) {
                                val pings = mutableListOf<Long>()

                                if (mode == ParseMode.BS) {
                                    // ── БС РЕЖИМ (Белый список / Заводы): проверка через внутренние сервисы RU + TG + Выход за рубеж ──
                                    val yaPing = CoreNativeManager.measureOutboundDelay(configJson, "https://ya.ru/")
                                    if (yaPing > 0) {
                                        isAlive = true
                                        services.add("YA")
                                        pings.add(yaPing)
                                    }

                                    coroutineScope {
                                        val vkDef = async(Dispatchers.IO) {
                                            CoreNativeManager.measureOutboundDelay(configJson, "https://vk.com/robots.txt")
                                        }
                                        val tgDef = async(Dispatchers.IO) {
                                            CoreNativeManager.measureOutboundDelay(configJson, "https://telegram.org/")
                                        }
                                        val intlDef = async(Dispatchers.IO) {
                                            CoreNativeManager.measureOutboundDelay(configJson, "https://cp.cloudflare.com/generate_204")
                                        }

                                        val vkPing = vkDef.await()
                                        if (vkPing > 0) {
                                            isAlive = true
                                            services.add("VK")
                                            pings.add(vkPing)
                                        }

                                        val tgPing = tgDef.await()
                                        if (tgPing > 0) {
                                            isAlive = true
                                            services.add("TG")
                                            pings.add(tgPing)
                                        }

                                        val intlPing = intlDef.await()
                                        if (intlPing > 0) {
                                            isAlive = true
                                            services.add("INTL")
                                            pings.add(intlPing)
                                        }
                                    }

                                    if (pings.isNotEmpty()) {
                                        finalPing = pings.average().toLong()
                                    }
                                } else {
                                    // ── МИКС / ЧС РЕЖИМ: Проверка через YouTube + TG + GPT + GEM ──
                                    val ytPing = CoreNativeManager.measureOutboundDelay(
                                        configJson,
                                        "https://www.youtube.com/generate_204"
                                    )

                                    if (ytPing > 0) {
                                        isAlive = true
                                        services.add("YT")
                                        pings.add(ytPing)

                                        // Параллельный запуск оставшихся сервисов (TG, GPT, GEM)
                                        coroutineScope {
                                            val tgDef = async(Dispatchers.IO) {
                                                CoreNativeManager.measureOutboundDelay(configJson, "https://telegram.org/")
                                            }
                                            val gptDef = async(Dispatchers.IO) {
                                                CoreNativeManager.measureOutboundDelay(configJson, "https://chatgpt.com/robots.txt")
                                            }
                                            val gemDef = async(Dispatchers.IO) {
                                                CoreNativeManager.measureOutboundDelay(configJson, "https://gemini.google.com/robots.txt")
                                            }

                                            val tgPing = tgDef.await()
                                            if (tgPing > 0) {
                                                services.add("TG")
                                                pings.add(tgPing)
                                            }
                                            val gptPing = gptDef.await()
                                            if (gptPing > 0) {
                                                services.add("GPT")
                                                pings.add(gptPing)
                                            }
                                            val gemPing = gemDef.await()
                                            if (gemPing > 0) {
                                                services.add("GEM")
                                                pings.add(gemPing)
                                            }
                                        }
                                        finalPing = pings.average().toLong()
                                    } else {
                                        // YouTube не ответил. Проверяем Cloudflare 204 и Telegram параллельно!
                                        coroutineScope {
                                            val cfDef = async(Dispatchers.IO) {
                                                CoreNativeManager.measureOutboundDelay(configJson, "https://cp.cloudflare.com/generate_204")
                                            }
                                            val tgDef = async(Dispatchers.IO) {
                                                CoreNativeManager.measureOutboundDelay(configJson, "https://telegram.org/")
                                            }

                                            val cfPing = cfDef.await()
                                            val tgPing = tgDef.await()

                                            if (tgPing > 0) {
                                                isAlive = true
                                                services.add("TG")
                                                pings.add(tgPing)
                                            }

                                            if (cfPing > 0) {
                                                isAlive = true
                                                val gptDef = async(Dispatchers.IO) {
                                                    CoreNativeManager.measureOutboundDelay(configJson, "https://chatgpt.com/robots.txt")
                                                }
                                                val gemDef = async(Dispatchers.IO) {
                                                    CoreNativeManager.measureOutboundDelay(configJson, "https://gemini.google.com/robots.txt")
                                                }

                                                val gptPing = gptDef.await()
                                                if (gptPing > 0) {
                                                    services.add("GPT")
                                                    pings.add(gptPing)
                                                }
                                                val gemPing = gemDef.await()
                                                if (gemPing > 0) {
                                                    services.add("GEM")
                                                    pings.add(gemPing)
                                                }

                                                if (services.isEmpty()) {
                                                    services.add("TUNNEL")
                                                    pings.add(cfPing)
                                                }
                                            }

                                            if (pings.isNotEmpty()) {
                                                finalPing = pings.average().toLong()
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (item.scheme in listOf("hysteria2", "hy2")) {
                            val hyPing = probeTcpPing(item.host, item.port, 600)
                            if (hyPing > 0) {
                                isAlive = true
                                finalPing = hyPing
                                services.add("HY2")
                            }
                        }

                        // УСЛОВИЕ ПРОХОЖДЕНИЯ:
                        val passesFilter = isAlive && services.isNotEmpty() && (
                            services.size >= minServices || (services.contains("TUNNEL") && minServices <= 1)
                        )

                        val processed = processedCounter.incrementAndGet()
                        if (passesFilter) {
                            aliveCounter.incrementAndGet()
                            val (geoCode, countryName) = GeoIpManager.resolveCountry(item)
                            val flag = GeoIpManager.getFlagEmoji(geoCode)

                            val result = CheckResult(
                                item = item,
                                ok = true,
                                speedMs = finalPing,
                                tags = services,
                                countryCode = geoCode,
                                countryName = countryName,
                                flag = flag
                            )

                            synchronized(listLock) {
                                verifiedList.add(result)
                            }
                        } else {
                            deadCounter.incrementAndGet()
                        }

                        val now = System.currentTimeMillis()
                        val elapsedSec = max(0.1f, (now - startTime) / 1000f)
                        val rate = processed / elapsedSec

                        // Throttled UI status updates
                        _stats.value = CheckerStats(
                            total = total,
                            processed = processed,
                            alive = aliveCounter.get(),
                            dead = deadCounter.get(),
                            ratePerSec = rate,
                            isRunning = isRunningFlag.get(),
                            isPaused = isPausedFlag.get(),
                            currentBatch = "${item.scheme.uppercase()} ${item.host}:${item.port}"
                        )

                        // Throttled results list emission:
                        // Внимание: НЕ выполняем дорогостоящий UriParser.rename на всём списке в цикле!
                        // Просто передаём снимок списка, сохраняя 100% плавность и высокую скорость после 1000 нод!
                        if (now - lastUiUpdateMs > 1000) {
                            lastUiUpdateMs = now
                            val snapshot = synchronized(listLock) {
                                if (mode == ParseMode.BS) {
                                    verifiedList.sortedWith(
                                        compareByDescending<CheckResult> { it.countryCode in setOf("RU", "BY") }
                                            .thenByDescending { it.item.port == 53 || it.item.port == 443 }
                                            .thenByDescending { it.tags.size }
                                            .thenBy { if (it.speedMs > 0) it.speedMs else Long.MAX_VALUE }
                                    )
                                } else {
                                    verifiedList.sortedWith(
                                        compareByDescending<CheckResult> { it.tags.size }
                                            .thenBy { if (it.speedMs > 0) it.speedMs else Long.MAX_VALUE }
                                    )
                                }
                            }
                            _results.value = snapshot
                        }
                    }
                }
            }

            workerJobs.joinAll()

            // Final sort and format on completion
            publishFormattedResults(verifiedList, listLock, mode)

            isRunningFlag.set(false)
            isPausedFlag.set(false)
            _stats.value = _stats.value.copy(
                isRunning = false,
                isPaused = false,
                currentBatch = "Проверка завершена! Отобрано ${verifiedList.size} реальных рабочих узлов"
            )
        }
    }

    private fun publishFormattedResults(
        verifiedList: MutableList<CheckResult>,
        listLock: Any,
        mode: ParseMode
    ) {
        val snapshot = synchronized(listLock) {
            if (mode == ParseMode.BS) {
                verifiedList.sortedWith(
                    compareByDescending<CheckResult> { it.countryCode in setOf("RU", "BY") }
                        .thenByDescending { it.item.port == 53 || it.item.port == 443 }
                        .thenByDescending { it.tags.size }
                        .thenBy { if (it.speedMs > 0) it.speedMs else Long.MAX_VALUE }
                )
            } else {
                verifiedList.sortedWith(
                    compareByDescending<CheckResult> { it.tags.size }
                        .thenBy { if (it.speedMs > 0) it.speedMs else Long.MAX_VALUE }
                )
            }
        }

        val totalAlive = snapshot.size
        val formattedList = snapshot.mapIndexed { idx, cr ->
            val numStr = if (totalAlive < 100) "#%02d".format(idx + 1) else "#%03d".format(idx + 1)
            val place = if (cr.countryName.isNotEmpty() && cr.countryName != "Планета 🌐") {
                "${cr.countryName} ${cr.flag}".trim()
            } else if (cr.flag.isNotEmpty() && cr.flag != "🌐") {
                cr.flag
            } else {
                "Планета 🌐"
            }
            val svcStr = if (cr.tags.isNotEmpty()) cr.tags.joinToString("×") else "ONLINE"
            val pingStr = if (cr.speedMs > 0) "${cr.speedMs}ms" else "—ms"
            val portTag = when (cr.item.port) {
                53 -> "53/DNS"
                443 -> "443"
                else -> "${cr.item.port}"
            }
            val remark = if (mode == ParseMode.BS) {
                "BUNKER 🤍 | $place $numStr | $svcStr | $portTag | $pingStr"
            } else {
                val extraPort = if (cr.item.port == 53) " | 53/DNS" else ""
                "BUNKER ${mode.heart} | $place $numStr | $svcStr$extraPort | $pingStr"
            }
            val finalUri = UriParser.rename(cr.item, remark)
            cr.copy(finalRemark = remark, finalUri = finalUri)
        }

        _results.value = formattedList
    }

    fun exportSubscription(results: List<CheckResult>, mode: ParseMode): String {
        val sb = StringBuilder()
        mode.formatSubscriptionHeader().forEach { sb.append(it).append("\n") }
        val totalAlive = results.size
        results.forEachIndexed { idx, cr ->
            val uriToExport = if (cr.finalUri.isNotBlank()) {
                cr.finalUri
            } else {
                val numStr = if (totalAlive < 100) "#%02d".format(idx + 1) else "#%03d".format(idx + 1)
                val place = if (cr.countryName.isNotEmpty() && cr.countryName != "Планета 🌐") {
                    "${cr.countryName} ${cr.flag}".trim()
                } else if (cr.flag.isNotEmpty() && cr.flag != "🌐") {
                    cr.flag
                } else {
                    "Планета 🌐"
                }
                val svcStr = if (cr.tags.isNotEmpty()) cr.tags.joinToString("×") else "ONLINE"
                val pingStr = if (cr.speedMs > 0) "${cr.speedMs}ms" else "—ms"
                val portTag = when (cr.item.port) {
                    53 -> "53/DNS"
                    443 -> "443"
                    else -> "${cr.item.port}"
                }
                val remark = if (mode == ParseMode.BS) {
                    "BUNKER 🤍 | $place $numStr | $svcStr | $portTag | $pingStr"
                } else {
                    val extraPort = if (cr.item.port == 53) " | 53/DNS" else ""
                    "BUNKER ${mode.heart} | $place $numStr | $svcStr$extraPort | $pingStr"
                }
                UriParser.rename(cr.item, remark)
            }
            sb.append(uriToExport).append("\n")
        }
        return sb.toString()
    }
}
