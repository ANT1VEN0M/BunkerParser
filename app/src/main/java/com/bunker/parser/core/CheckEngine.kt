package com.bunker.parser.core

import com.bunker.parser.model.CheckItem
import com.bunker.parser.model.CheckResult
import com.bunker.parser.model.CheckerStats
import com.bunker.parser.model.ParseMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
    private var job: Job? = null

    fun isRunning(): Boolean = isRunningFlag.get()

    fun stop() {
        isRunningFlag.set(false)
        job?.cancel()
        _stats.value = _stats.value.copy(isRunning = false)
    }

    suspend fun start(
        items: List<CheckItem>,
        mode: ParseMode,
        strict4Services: Boolean,
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
        val semaphore = Semaphore(workers.coerceIn(1, 24))

        _stats.value = CheckerStats(
            total = total,
            processed = 0,
            alive = 0,
            dead = 0,
            ratePerSec = 0f,
            isRunning = true,
            currentBatch = "Старт реальной проверки узлов..."
        )

        job = launch {
            val tasks = items.map { item ->
                async {
                    if (!isRunningFlag.get()) return@async null

                    semaphore.withPermit {
                        if (!isRunningFlag.get()) return@withPermit null

                        val timeoutMs = (timeoutSec * 1000).toInt().coerceIn(1000, 10000)
                        val configJson = XrayConfigBuilder.buildConfig(item.rawUri)

                        var isAlive = false
                        var finalPing = 0L
                        val services = mutableListOf<String>()

                        if (configJson != null) {
                            // Step 1: Real proxy verification against YouTube
                            val ytPing = CoreNativeManager.measureOutboundDelay(
                                configJson,
                                "https://www.youtube.com/generate_204"
                            )

                            if (ytPing > 0) {
                                isAlive = true
                                services.add("YT")
                                val pings = mutableListOf(ytPing)

                                // Step 2: Probe Telegram
                                val tgPing = CoreNativeManager.measureOutboundDelay(
                                    configJson,
                                    "https://telegram.org/"
                                )
                                if (tgPing > 0) {
                                    services.add("TG")
                                    pings.add(tgPing)
                                }

                                // Step 3: Probe ChatGPT
                                val gptPing = CoreNativeManager.measureOutboundDelay(
                                    configJson,
                                    "https://chatgpt.com/robots.txt"
                                )
                                if (gptPing > 0) {
                                    services.add("GPT")
                                    pings.add(gptPing)
                                }

                                // Step 4: Probe Gemini
                                val gemPing = CoreNativeManager.measureOutboundDelay(
                                    configJson,
                                    "https://gemini.google.com/robots.txt"
                                )
                                if (gemPing > 0) {
                                    services.add("GEM")
                                    pings.add(gemPing)
                                }

                                finalPing = pings.average().toLong()
                            }
                        }

                        // Fallback only if native engine was completely unavailable (e.g. no .so)
                        if (!isAlive && configJson == null) {
                            val tcpPing = probeTcpPing(item.host, item.port, timeoutMs.coerceAtMost(2500))
                            if (tcpPing > 0) {
                                isAlive = true
                                finalPing = tcpPing
                            }
                        }

                        val passesFilter = if (strict4Services) {
                            isAlive && services.size >= 4
                        } else {
                            isAlive
                        }

                        val processed = processedCounter.incrementAndGet()
                        if (passesFilter) {
                            aliveCounter.incrementAndGet()
                        } else {
                            deadCounter.incrementAndGet()
                        }

                        val elapsedSec = max(0.1f, (System.currentTimeMillis() - startTime) / 1000f)
                        val rate = processed / elapsedSec

                        _stats.value = CheckerStats(
                            total = total,
                            processed = processed,
                            alive = aliveCounter.get(),
                            dead = deadCounter.get(),
                            ratePerSec = rate,
                            isRunning = isRunningFlag.get(),
                            currentBatch = "${item.scheme.uppercase()} ${item.host}:${item.port}"
                        )

                        if (passesFilter) {
                            val (geoCode, countryName) = GeoIpManager.resolveCountry(item)
                            val flag = GeoIpManager.getFlagEmoji(geoCode)

                            CheckResult(
                                item = item,
                                ok = true,
                                speedMs = finalPing,
                                tags = services,
                                countryCode = geoCode,
                                countryName = countryName,
                                flag = flag
                            )
                        } else {
                            null
                        }
                    }
                }
            }

            // Collect completed tasks
            tasks.forEach { task ->
                val res = task.await()
                if (res != null) {
                    synchronized(verifiedList) {
                        verifiedList.add(res)
                        // Sort by real latency
                        verifiedList.sortBy { it.speedMs }
                        // Format numbering and bunker remarks
                        val formattedList = verifiedList.mapIndexed { idx, cr ->
                            val numStr = if (verifiedList.size < 100) "#%02d".format(idx + 1) else "#%03d".format(idx + 1)
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
                }
            }

            isRunningFlag.set(false)
            _stats.value = _stats.value.copy(
                isRunning = false,
                currentBatch = "Проверка завершена! Отобрано ${verifiedList.size} реальных рабочих серверов"
            )
        }
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
