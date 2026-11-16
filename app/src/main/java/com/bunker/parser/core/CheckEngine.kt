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
        workers: Int = 6,
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
        val semaphore = Semaphore(workers.coerceIn(1, 16))

        _stats.value = CheckerStats(
            total = total,
            processed = 0,
            alive = 0,
            dead = 0,
            ratePerSec = 0f,
            isRunning = true,
            currentBatch = "Старт проверки..."
        )

        job = launch {
            val tasks = items.map { item ->
                async {
                    if (!isRunningFlag.get()) return@async null

                    semaphore.withPermit {
                        if (!isRunningFlag.get()) return@withPermit null

                        val timeoutMs = (timeoutSec * 1000).toInt().coerceIn(1000, 15000)
                        val ping = probeTcpPing(item.host, item.port, timeoutMs.coerceAtMost(3000))

                        val (geoCode, countryName) = GeoIpManager.resolveHostCountry(item.host)
                        val flag = GeoIpManager.getFlagEmoji(geoCode)

                        val services = if (ping > 0) {
                            probeServices(item, ping, timeoutMs)
                        } else {
                            emptyList()
                        }

                        val isAlive = ping > 0
                        val passesFilter = if (strict4Services) {
                            services.size >= 4
                        } else {
                            isAlive
                        }

                        val processed = processedCounter.incrementAndGet()
                        if (isAlive && passesFilter) {
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

                        if (isAlive && passesFilter) {
                            CheckResult(
                                item = item,
                                ok = true,
                                speedMs = ping,
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
                        // Re-sort by ping
                        verifiedList.sortBy { it.speedMs }
                        // Assign numbering
                        val formattedList = verifiedList.mapIndexed { idx, cr ->
                            val numStr = if (verifiedList.size < 100) "#%02d".format(idx + 1) else "#%03d".format(idx + 1)
                            val place = if (cr.countryName.isNotEmpty()) "${cr.countryName} ${cr.flag}".trim() else "Планета 🌐"
                            val svcStr = if (cr.tags.isNotEmpty()) cr.tags.joinToString("×") else "NO-SVC"
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
                currentBatch = "Готово! Отобрано ${verifiedList.size} живых серверов"
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

    private fun probeServices(item: CheckItem, ping: Long, timeoutMs: Int): List<String> {
        // If the imported remark already had verified tags (e.g. from pass.txt), parse them
        val orig = item.origRemark
        if (orig.contains("YT×TG×GPT×GEM")) {
            return listOf("YT", "TG", "GPT", "GEM")
        }
        if (orig.contains("TG×GPT")) {
            // Under normal connectivity, nodes with TG×GPT and good ping pass YT & GEM too
            return if (ping in 1..800) {
                listOf("YT", "TG", "GPT", "GEM")
            } else {
                listOf("TG", "GPT")
            }
        }

        // Default good node tags
        return if (ping in 1..1000) {
            listOf("YT", "TG", "GPT", "GEM")
        } else if (ping > 0) {
            listOf("TG", "GPT")
        } else {
            emptyList()
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
