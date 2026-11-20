package com.bunker.parser.model

enum class ParseMode(
    val title: String,
    val heart: String,
    val headerTitle: String,
    val fileName: String,
    val defaultMinServices: Int = 1
) {
    MIX(
        title = "МИКС",
        heart = "⚡",
        headerTitle = "Бункер МИКС ⚡",
        fileName = "BLWL_MIX.txt",
        defaultMinServices = 1
    ),
    CS(
        title = "ЧС",
        heart = "🖤",
        headerTitle = "Бункер ЧС 🖤",
        fileName = "BLACK_LIST.txt",
        defaultMinServices = 1
    ),
    BS(
        title = "БС",
        heart = "🤍",
        headerTitle = "Бункер БС 🤍",
        fileName = "WHITE_LIST.txt",
        defaultMinServices = 1
    );

    val defaultStrict4: Boolean
        get() = defaultMinServices >= 4

    fun formatSubscriptionHeader(): List<String> = listOf(
        "#profile-title: $headerTitle",
        "#profile-update-interval: 1",
        "# 🐈‍⬛ BUNKER PARSER",
        ""
    )
}
