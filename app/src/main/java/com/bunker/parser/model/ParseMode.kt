package com.bunker.parser.model

enum class ParseMode(
    val title: String,
    val heart: String,
    val headerTitle: String,
    val fileName: String,
    val defaultStrict4: Boolean
) {
    MIX(
        title = "МИКС",
        heart = "⚡",
        headerTitle = "Бункер МИКС ⚡",
        fileName = "BLWL_MIX.txt",
        defaultStrict4 = true
    ),
    CS(
        title = "ЧС",
        heart = "🖤",
        headerTitle = "Бункер ЧС 🖤",
        fileName = "BLACK_LIST.txt",
        defaultStrict4 = false
    ),
    BS(
        title = "БС",
        heart = "🤍",
        headerTitle = "Бункер БС 🤍",
        fileName = "WHITE_LIST.txt",
        defaultStrict4 = false
    );

    fun formatSubscriptionHeader(): List<String> = listOf(
        "#profile-title: $headerTitle",
        "#profile-update-interval: 1",
        "#support-url: https://t.me/BUNKER_KOTA",
        "#profile-web-page-url: https://t.me/BUNKER_KOTA",
        "# 🐈‍⬛ Бункер | VPN • Proxy TG",
        "# 📢 Наш Telegram: https://t.me/BUNKER_KOTA",
        "# 💬 Чат: https://t.me/BUNKER_K0TA",
        ""
    )
}
