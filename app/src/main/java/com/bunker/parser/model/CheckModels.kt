package com.bunker.parser.model

data class CheckItem(
    val seq: Int,
    val lineNo: Int,
    val rawUri: String,
    val scheme: String,
    val host: String,
    val port: Int,
    val baseUri: String,
    val origRemark: String = ""
)

data class CheckResult(
    val item: CheckItem,
    val ok: Boolean,
    val speedMs: Long = 0,
    val tags: List<String> = emptyList(),
    val countryCode: String = "",
    val countryName: String = "",
    val flag: String = "",
    val finalRemark: String = "",
    val finalUri: String = ""
) {
    val serviceLabel: String
        get() = if (tags.isNotEmpty()) tags.joinToString("×") else "NO-SVC"

    val speedLabel: String
        get() = if (speedMs > 0) "${speedMs}ms" else "—ms"
}

data class CheckerStats(
    val total: Int = 0,
    val processed: Int = 0,
    val alive: Int = 0,
    val dead: Int = 0,
    val ratePerSec: Float = 0f,
    val isRunning: Boolean = false,
    val currentBatch: String = ""
)
