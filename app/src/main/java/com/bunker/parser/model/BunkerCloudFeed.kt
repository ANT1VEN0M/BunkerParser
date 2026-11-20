package com.bunker.parser.model

enum class BunkerCloudFeed(
    val title: String,
    val iconEmoji: String,
    val subtitle: String,
    val url: String,
    val mirrorUrl: String
) {
    MIX(
        title = "Микс",
        iconEmoji = "🌍",
        subtitle = "YouTube 4K",
        url = "https://cdn.jsdelivr.net/gh/ANT1VEN0M/Bunker-Auto-Engine@main/sub/mix_b64.txt",
        mirrorUrl = "https://ant1ven0m.github.io/Bunker-Auto-Engine/sub/mix_b64.txt"
    ),
    BS(
        title = "БС",
        iconEmoji = "🤍",
        subtitle = "РФ / РБ",
        url = "https://cdn.jsdelivr.net/gh/ANT1VEN0M/Bunker-Auto-Engine@main/sub/bs_b64.txt",
        mirrorUrl = "https://ant1ven0m.github.io/Bunker-Auto-Engine/sub/bs_b64.txt"
    ),
    FACTORY(
        title = "Завод",
        iconEmoji = "🏭",
        subtitle = "443 & 53 DNS",
        url = "https://cdn.jsdelivr.net/gh/ANT1VEN0M/Bunker-Auto-Engine@main/sub/factory_b64.txt",
        mirrorUrl = "https://ant1ven0m.github.io/Bunker-Auto-Engine/sub/factory_b64.txt"
    ),
    TURBO(
        title = "Турбо",
        iconEmoji = "⚡",
        subtitle = "Мин. пинг",
        url = "https://cdn.jsdelivr.net/gh/ANT1VEN0M/Bunker-Auto-Engine@main/sub/fast_b64.txt",
        mirrorUrl = "https://ant1ven0m.github.io/Bunker-Auto-Engine/sub/fast_b64.txt"
    ),
    TG_SOS(
        title = "TG SOS",
        iconEmoji = "💬",
        subtitle = "Telegram",
        url = "https://cdn.jsdelivr.net/gh/ANT1VEN0M/Bunker-Auto-Engine@main/sub/tg_b64.txt",
        mirrorUrl = "https://ant1ven0m.github.io/Bunker-Auto-Engine/sub/tg_b64.txt"
    )
}
