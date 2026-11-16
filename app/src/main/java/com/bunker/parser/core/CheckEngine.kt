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
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class CheckEngine {
    private val _stats = MutableStateFlow(CheckerStats())
    val stats: StateFlow<CheckerStats> = _stats.asStateFlow()

    private val _results = MutableStateFlow<List<CheckResult>>(emptyList())
    val results: StateFlow<List<CheckResult>> = _results.asStateFlow()

    private val isRunningFlag = AtomicBoolean(false)
    private var engineJob: Job? = null

    fun isRunning(): Boolean = isRunningFlag.get()

    fun stop() {
        isRunningFlag.set(false)
        engineJob?.cancel()
        _stats.value = _stats.value.copy(isRunning = false)
    }

    suspend fun start(
        items: List<CheckItem>,
        mode: ParseMode,
        minServices: Int = 1,
        workers: Int = 12,
        timeoutSec: Float = 4.0f
    ) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        stop()

        isRunningFlag.set(true)
        val verifiedList = mutableListOf<CheckResult>()
        _results.value = emptyList()

        val total = items.size
        val processedCounter = AtomicInteger(0)
        val aliveCounter = AtomicInteger(0)
        val deadCounter = AtomicInteger(0)

        val startTime = System.currentTimeMillis()
        val workerPoolSize = workers.coerceIn(1, 24)

        _stats.value = CheckerStats(
            total = total,
            processed = 0,
            alive = 0,
            dead = 0,
            ratePerSec = 0f,
            isRunning = true,
            currentBatch = "Старт мульти-сервисной проверки Xray Core..."
        )

        engineJob = launch {
            val channel = Channel<CheckItem>(capacity = Channel.BUFFERED)
            val listLock = Any()
            var lastUiUpdateMs = System.currentTimeMillis()

            // Producer: sends items into channel sequentially (zero memory explosion)
            launch(Dispatchers.IO) {
                for (item in items) {
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

                        val configJson = XrayConfigBuilder.buildConfig(item.rawUri)
                        var isAlive = false
                        var finalPing = 0L
                        val services = mutableListOf<String>()

                        if (configJson != null) {
                            // Quick TCP pre-check (1000ms timeout) to drop completely dead IP/ports immediately
                            val preTcp = probeTcpPing(item.host, item.port, 1000)
                            if (preTcp > 0) {
                                val pings = mutableListOf<Long>()

                                // ── 1. Проверка YouTube ──
                                val ytPing = CoreNativeManager.measureOutboundDelay(
                                    configJson,
                                    "https://www.youtube.com/generate_204"
                                )
                                if (ytPing > 0) {
                                    services.add("YT")
                                    pings.add(ytPing)
                                }

                                // ── 2. Проверка Telegram ──
                                val tgPing = CoreNativeManager.measureOutboundDelay(
                                    configJson,
                                    "https://telegram.org/"
                                )
                                if (tgPing > 0) {
                                    services.add("TG")
                                    pings.add(tgPing)
                                }

                                // ── 3. Проверка ChatGPT ──
                                val gptPing = CoreNativeManager.measureOutboundDelay(
                                    configJson,
                                    "https://chatgpt.com/robots.txt"
                                )
                                if (gptPing > 0) {
                                    services.add("GPT")
                                    pings.add(gptPing)
                                }

                                // ── 4. Проверка Gemini ──
                                val gemPing = CoreNativeManager.measureOutboundDelay(
                                    configJson,
                                    "https://gemini.google.com/robots.txt"
                                )
                                if (gemPing > 0) {
                                    services.add("GEM")
                                    pings.add(gemPing)
                                }

                                if (services.isNotEmpty()) {
                                    isAlive = true
                                    finalPing = pings.average().toLong()
                                } else {
                                    // Если ни один из 4 специфичных сервисов не открылся, проверяем общий выход в интернет
                                    var genPing = CoreNativeManager.measureOutboundDelay(
                                        configJson,
                                        "https://www.google.com/generate_204"
                                    )
                                    if (genPing <= 0) {
                                        genPing = CoreNativeManager.measureOutboundDelay(
                                            configJson,
                                            "https://cp.cloudflare.com/generate_204"
                                        )
                                    }
                                    if (genPing > 0) {
                                        isAlive = true
                                        services.add("TUNNEL")
                                        finalPing = genPing
                                    }
                                }
                            }
                        } else if (item.scheme in listOf("hysteria2", "hy2")) {
                            // Протокол Hysteria2
                            val hyPing = probeTcpPing(item.host, item.port, (timeoutSec * 1000).toInt())
                            if (hyPing > 0) {
                                isAlive = true
                                finalPing = hyPing
                                services.add("HY2")
                            }
                        }

                        // УСЛОВИЕ ПРОХОЖДЕНИЯ:
                        // 1. Узел должен быть реально живым (isAlive)
                        // 2. Должен иметь хотя бы 1 работающий сервис (services.isNotEmpty) — узлы которые "никуда не заходят" отсекаются!
                        // 3. Количество работающих сервисов должно удовлетворять minServices (1, 2, 3 или 4)
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
                            currentBatch = "${item.scheme.uppercase()} ${item.host}:${item.port}"
                        )

                        // Throttled results list emission (every 500ms)
                        if (now - lastUiUpdateMs > 500) {
                            lastUiUpdateMs = now
                            publishFormattedResults(verifiedList, listLock, mode)
                        }
                    }
                }
            }

            workerJobs.joinAll()

            // Final sort and format on completion
            publishFormattedResults(verifiedList, listLock, mode)

            isRunningFlag.set(false)
            _stats.value = _stats.value.copy(
                isRunning = false,
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
            // Элитная сортировка:
            // Сначала идут ноды с наибольшим количеством сервисов (4/4 -> 3 -> 2 -> 1),
            // а внутри одной категории — самые быстрые по пингу!
            verifiedList.sortedWith(
                compareByDescending<CheckResult> { it.tags.size }
                    .thenBy { if (it.speedMs > 0) it.speedMs else Long.MAX_VALUE }
            )
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
            val remark = "BUNKER ${mode.heart} | $place $numStr | $svcStr | $pingStr"
            val finalUri = UriParser.rename(cr.item, remark)
            cr.copy(finalRemark = remark, finalUri = finalUri)
        }

        _results.value = formattedList
    }

    private fun probeTcpPing(host: String, port: Int, timeoutMs: Int): Long {
        val started = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.soTimeout = timeoutMs
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                System.currentTimeMillis() - started
            }
        } catch (_: Exception) {
            -1L
        }
    }

    fun exportSubscription(results: List<CheckResult>, mode: ParseMode): String {
        val sb = StringBuilder()
        mode.formatSubscriptionHeader().forEach { sb.append(it).append("\n") }
        results.forEach { cr ->
            sb.append(cr.finalUri).append("\n")
        }
        return sb.toString()
    }
}
