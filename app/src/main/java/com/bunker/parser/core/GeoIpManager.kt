package com.bunker.parser.core

import com.bunker.parser.model.CheckItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object GeoIpManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, Pair<String, String>>()

    val CC_RU: Map<String, String> = mapOf(
        "AD" to "Андорра",
        "AE" to "ОАЭ",
        "AF" to "Афганистан",
        "AG" to "Антигуа и Барбуда",
        "AI" to "Ангилья",
        "AL" to "Албания",
        "AM" to "Армения",
        "AO" to "Ангола",
        "AQ" to "Антарктида",
        "AR" to "Аргентина",
        "AS" to "Американское Самоа",
        "AT" to "Австрия",
        "AU" to "Австралия",
        "AW" to "Аруба",
        "AX" to "Аландские острова",
        "AZ" to "Азербайджан",
        "BA" to "Босния и Герцеговина",
        "BB" to "Барбадос",
        "BD" to "Бангладеш",
        "BE" to "Бельгия",
        "BF" to "Буркина-Фасо",
        "BG" to "Болгария",
        "BH" to "Бахрейн",
        "BI" to "Бурунди",
        "BJ" to "Бенин",
        "BL" to "Сен-Бартелеми",
        "BM" to "Бермуды",
        "BN" to "Бруней",
        "BO" to "Боливия",
        "BR" to "Бразилия",
        "BS" to "Багамы",
        "BT" to "Бутан",
        "BV" to "Остров Буве",
        "BW" to "Ботсвана",
        "BY" to "Беларусь",
        "BZ" to "Белиз",
        "CA" to "Канада",
        "CC" to "Кокосовые острова",
        "CD" to "Конго (ДРК)",
        "CF" to "ЦАР",
        "CG" to "Конго",
        "CH" to "Швейцария",
        "CI" to "Кот-д'Ивуар",
        "CK" to "Острова Кука",
        "CL" to "Чили",
        "CM" to "Камерун",
        "CN" to "Китай",
        "CO" to "Колумбия",
        "CR" to "Коста-Рика",
        "CU" to "Куба",
        "CV" to "Кабо-Верде",
        "CW" to "Кюрасао",
        "CX" to "Остров Рождества",
        "CY" to "Кипр",
        "CZ" to "Чехия",
        "DE" to "Германия",
        "DJ" to "Джибути",
        "DK" to "Дания",
        "DM" to "Доминика",
        "DO" to "Доминиканская Республика",
        "DZ" to "Алжир",
        "EC" to "Эквадор",
        "EE" to "Эстония",
        "EG" to "Египет",
        "EH" to "Западная Сахара",
        "ER" to "Эритрея",
        "ES" to "Испания",
        "ET" to "Эфиопия",
        "FI" to "Финляндия",
        "FJ" to "Фиджи",
        "FK" to "Фолклендские острова",
        "FM" to "Микронезия",
        "FO" to "Фарерские острова",
        "FR" to "Франция",
        "GA" to "Габон",
        "GB" to "Великобритания",
        "GD" to "Гренада",
        "GE" to "Грузия",
        "GF" to "Французская Гвиана",
        "GG" to "Гернси",
        "GH" to "Гана",
        "GI" to "Гибралтар",
        "GL" to "Гренландия",
        "GM" to "Гамбия",
        "GN" to "Гвинея",
        "GP" to "Гваделупа",
        "GQ" to "Экваториальная Гвинея",
        "GR" to "Греция",
        "GS" to "Южная Георгия",
        "GT" to "Гватемала",
        "GU" to "Гуам",
        "GW" to "Гвинея-Бисау",
        "GY" to "Гайана",
        "HK" to "Гонконг",
        "HM" to "Остров Херд",
        "HN" to "Гондурас",
        "HR" to "Хорватия",
        "HT" to "Гаити",
        "HU" to "Венгрия",
        "ID" to "Индонезия",
        "IE" to "Ирландия",
        "IL" to "Израиль",
        "IM" to "Остров Мэн",
        "IN" to "Индия",
        "IO" to "Британская территория в Индийском океане",
        "IQ" to "Ирак",
        "IR" to "Иран",
        "IS" to "Исландия",
        "IT" to "Италия",
        "JE" to "Джерси",
        "JM" to "Ямайка",
        "JO" to "Иордания",
        "JP" to "Япония",
        "KE" to "Кения",
        "KG" to "Кыргызстан",
        "KH" to "Камбоджа",
        "KI" to "Кирибати",
        "KM" to "Коморы",
        "KN" to "Сент-Китс и Невис",
        "KP" to "Северная Корея",
        "KR" to "Южная Корея",
        "KW" to "Кувейт",
        "KY" to "Каймановы острова",
        "KZ" to "Казахстан",
        "LA" to "Лаос",
        "LB" to "Ливан",
        "LC" to "Сент-Люсия",
        "LI" to "Лихтенштейн",
        "LK" to "Шри-Ланка",
        "LR" to "Либерия",
        "LS" to "Лесото",
        "LT" to "Литва",
        "LU" to "Люксембург",
        "LV" to "Латвия",
        "LY" to "Ливия",
        "MA" to "Марокко",
        "MC" to "Монако",
        "MD" to "Молдова",
        "ME" to "Черногория",
        "MF" to "Сен-Мартен",
        "MG" to "Мадагаскар",
        "MH" to "Маршалловы острова",
        "MK" to "Северная Македония",
        "ML" to "Мали",
        "MM" to "Мьянма",
        "MN" to "Монголия",
        "MO" to "Макао",
        "MP" to "Северные Марианские острова",
        "MQ" to "Мартиника",
        "MR" to "Мавритания",
        "MS" to "Монтсеррат",
        "MT" to "Мальта",
        "MU" to "Маврикий",
        "MV" to "Мальдивы",
        "MW" to "Малави",
        "MX" to "Мексика",
        "MY" to "Малайзия",
        "MZ" to "Мозамбик",
        "NA" to "Намибия",
        "NC" to "Новая Каледония",
        "NE" to "Нигер",
        "NF" to "Остров Норфолк",
        "NG" to "Нигерия",
        "NI" to "Никарагуа",
        "NL" to "Нидерланды",
        "NO" to "Норвегия",
        "NP" to "Непал",
        "NR" to "Науру",
        "NU" to "Ниуэ",
        "NZ" to "Новая Зеландия",
        "OM" to "Оман",
        "PA" to "Панама",
        "PE" to "Перу",
        "PF" to "Французская Полинезия",
        "PG" to "Папуа — Новая Гвинея",
        "PH" to "Филиппины",
        "PK" to "Пакистан",
        "PL" to "Польша",
        "PM" to "Сен-Пьер и Микелон",
        "PN" to "Питкэрн",
        "PR" to "Пуэрто-Рико",
        "PS" to "Палестина",
        "PT" to "Португалия",
        "PW" to "Палау",
        "PY" to "Парагвай",
        "QA" to "Катар",
        "RE" to "Реюньон",
        "RO" to "Румыния",
        "RS" to "Сербия",
        "RU" to "Россия",
        "RW" to "Руанда",
        "SA" to "Саудовская Аравия",
        "SB" to "Соломоновы Острова",
        "SC" to "Сейшелы",
        "SD" to "Судан",
        "SE" to "Швеция",
        "SG" to "Сингапур",
        "SH" to "Остров Святой Елены",
        "SI" to "Словения",
        "SJ" to "Шпицберген",
        "SK" to "Словакия",
        "SL" to "Сьерра-Леоне",
        "SM" to "Сан-Марино",
        "SN" to "Сенегал",
        "SO" to "Сомали",
        "SR" to "Суринам",
        "SS" to "Южный Судан",
        "ST" to "Сан-Томе и Принсипи",
        "SV" to "Сальвадор",
        "SX" to "Синт-Мартен",
        "SY" to "Сирия",
        "SZ" to "Эсватини",
        "TC" to "Тёркс и Кайкос",
        "TD" to "Чад",
        "TF" to "Французские Южные территории",
        "TG" to "Того",
        "TH" to "Таиланд",
        "TJ" to "Таджикистан",
        "TK" to "Токелау",
        "TL" to "Восточный Тимор",
        "TM" to "Туркменистан",
        "TN" to "Тунис",
        "TO" to "Тонга",
        "TR" to "Турция",
        "TT" to "Тринидад и Тобаго",
        "TV" to "Тувалу",
        "TW" to "Тайвань",
        "TZ" to "Танзания",
        "UA" to "Украина",
        "UG" to "Уганда",
        "UM" to "Внешние малые острова (США)",
        "US" to "США",
        "UY" to "Уругвай",
        "UZ" to "Узбекистан",
        "VA" to "Ватикан",
        "VC" to "Сент-Винсент и Гренадины",
        "VE" to "Венесуэла",
        "VG" to "Британские Виргинские острова",
        "VI" to "Виргинские острова (США)",
        "VN" to "Вьетнам",
        "VU" to "Вануату",
        "WF" to "Уоллис и Футуна",
        "WS" to "Самоа",
        "YE" to "Йемен",
        "YT" to "Майотта",
        "ZA" to "ЮАР",
        "ZM" to "Замбия",
        "ZW" to "Зимбабве",
    )

    private val NAME_TO_CC: Map<String, String> by lazy {
        val map = HashMap<String, String>()
        CC_RU.forEach { (code, name) ->
            map[name.lowercase()] = code
        }
        // Aliases and english names commonly present in remarks
        map["germany"] = "DE"; map["германия"] = "DE"; map["deutschland"] = "DE"
        map["netherlands"] = "NL"; map["нидерланды"] = "NL"; map["голландия"] = "NL"; map["holland"] = "NL"
        map["russia"] = "RU"; map["россия"] = "RU"; map["рф"] = "RU"
        map["usa"] = "US"; map["сша"] = "US"; map["united states"] = "US"; map["america"] = "US"
        map["sweden"] = "SE"; map["швеция"] = "SE"
        map["finland"] = "FI"; map["финляндия"] = "FI"
        map["estonia"] = "EE"; map["эстония"] = "EE"
        map["poland"] = "PL"; map["польша"] = "PL"
        map["turkey"] = "TR"; map["турция"] = "TR"; map["türkiye"] = "TR"
        map["france"] = "FR"; map["франция"] = "FR"
        map["united kingdom"] = "GB"; map["великобритания"] = "GB"; map["англия"] = "GB"; map["uk"] = "GB"
        map["austria"] = "AT"; map["австрия"] = "AT"
        map["switzerland"] = "CH"; map["швейцария"] = "CH"
        map["japan"] = "JP"; map["япония"] = "JP"
        map["singapore"] = "SG"; map["сингапур"] = "SG"
        map["canada"] = "CA"; map["канада"] = "CA"
        map["kazakhstan"] = "KZ"; map["казахстан"] = "KZ"
        map["ukraine"] = "UA"; map["украина"] = "UA"
        map["czech"] = "CZ"; map["чехия"] = "CZ"
        map["latvia"] = "LV"; map["латвия"] = "LV"
        map["lithuania"] = "LT"; map["литва"] = "LT"
        map["spain"] = "ES"; map["испания"] = "ES"
        map["italy"] = "IT"; map["италия"] = "IT"
        map["norway"] = "NO"; map["норвегия"] = "NO"
        map["hong kong"] = "HK"; map["гонконг"] = "HK"
        map["bulgaria"] = "BG"; map["болгария"] = "BG"
        map["romania"] = "RO"; map["румыния"] = "RO"
        map["moldova"] = "MD"; map["молдова"] = "MD"
        map["georgia"] = "GE"; map["грузия"] = "GE"
        map["armenia"] = "AM"; map["армения"] = "AM"
        map["serbia"] = "RS"; map["сербия"] = "RS"
        map["belarus"] = "BY"; map["беларусь"] = "BY"; map["белоруссия"] = "BY"
        map["south korea"] = "KR"; map["корея"] = "KR"
        map["israel"] = "IL"; map["израиль"] = "IL"
        map["uae"] = "AE"; map["оаэ"] = "AE"; map["дубай"] = "AE"; map["dubai"] = "AE"
        map
    }

    fun getCountryName(cc: String?): String {
        if (cc.isNullOrBlank()) return "Планета 🌐"
        val upper = cc.trim().uppercase()
        return CC_RU[upper] ?: upper
    }

    fun getFlagEmoji(cc: String?): String {
        if (cc.isNullOrBlank()) return "🌐"
        val upper = cc.trim().uppercase()
        if (upper.length == 2 && upper.all { it in 'A'..'Z' }) {
            val first = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
            val second = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(first)) + String(Character.toChars(second))
        }
        return "🌐"
    }

    fun getCountryWithFlag(cc: String?): String {
        val code = cc?.trim()?.uppercase().orEmpty()
        val name = getCountryName(code)
        val flag = getFlagEmoji(code)
        return if (flag.isNotEmpty() && flag != "🌐") "$name $flag" else name
    }

    /**
     * Extracts a 2-letter ISO country code from Unicode regional indicator flags (e.g. 🇩🇪 -> DE).
     */
    fun extractCountryFromFlagEmoji(text: String): String? {
        var i = 0
        while (i < text.length - 1) {
            val cp1 = text.codePointAt(i)
            if (cp1 in 0x1F1E6..0x1F1FF) {
                val step = Character.charCount(cp1)
                val nextIdx = i + step
                if (nextIdx < text.length) {
                    val cp2 = text.codePointAt(nextIdx)
                    if (cp2 in 0x1F1E6..0x1F1FF) {
                        val c1 = ('A'.code + (cp1 - 0x1F1E6)).toChar()
                        val c2 = ('A'.code + (cp2 - 0x1F1E6)).toChar()
                        val code = "$c1$c2"
                        if (CC_RU.containsKey(code)) {
                            return code
                        }
                    }
                }
            }
            i += Character.charCount(cp1)
        }
        return null
    }

    /**
     * Extracts country code from remark text using flags, names, or isolated codes.
     */
    fun extractCountryFromRemark(remark: String): String? {
        if (remark.isBlank()) return null

        // 1. Check flag emoji
        extractCountryFromFlagEmoji(remark)?.let { return it }

        // 2. Check full names & aliases
        val lower = remark.lowercase()
        for ((name, code) in NAME_TO_CC) {
            if (lower.contains(name)) return code
        }

        // 3. Check bracketed or separated 2-letter codes: e.g. [DE], (NL), | RU |, -US-
        val regex = Regex("""(?:\b|[\(\[\{_\|\-])([A-Z]{2})(?:\b|[\)\]\}_\|\-])""")
        val match = regex.find(remark.uppercase())
        if (match != null) {
            val candidate = match.groupValues[1]
            if (CC_RU.containsKey(candidate) && candidate != "IP" && candidate != "TG" && candidate != "MS" && candidate != "OK" && candidate != "NO") {
                return candidate
            }
        }
        return null
    }

    /**
     * Extracts country code from domain or host name prefixes/TLDs.
     */
    fun extractCountryFromDomain(host: String): String? {
        val h = host.lowercase().trim()
        val tld = h.substringAfterLast(".", "")
        if (tld.length == 2 && CC_RU.containsKey(tld.uppercase())) {
            return tld.uppercase()
        }
        val firstPart = h.substringBefore(".")
        val prefixMatch = Regex("""^(?:free-)?([a-z]{2})(?:[0-9]|-|$)""").find(firstPart)
        if (prefixMatch != null) {
            val code = prefixMatch.groupValues[1].uppercase()
            if (CC_RU.containsKey(code) && code != "WW" && code != "AP" && code != "EU") {
                return code
            }
        }
        return null
    }

    /**
     * Fast online GeoIP query with multiple HTTPS endpoints.
     */
    private fun queryOnlineGeoIp(ip: String): String? {
        // 1. Try https://api.country.is/$ip (returns {"ip":"...","country":"DE"})
        try {
            val req = Request.Builder()
                .url("https://api.country.is/$ip")
                .header("User-Agent", "Mozilla/5.0 BunkerParser/1.1")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val match = Regex(""""country"\s*:\s*"([A-Za-z]{2})"""").find(body)
                    val code = match?.groupValues?.get(1)?.uppercase()
                    if (!code.isNullOrEmpty() && CC_RU.containsKey(code)) {
                        return code
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Try https://ipwho.is/$ip
        try {
            val req = Request.Builder()
                .url("https://ipwho.is/$ip")
                .header("User-Agent", "Mozilla/5.0 BunkerParser/1.1")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val match = Regex(""""country_code"\s*:\s*"([A-Za-z]{2})"""").find(body)
                    val code = match?.groupValues?.get(1)?.uppercase()
                    if (!code.isNullOrEmpty() && CC_RU.containsKey(code)) {
                        return code
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Try http://ip-api.com/line/$ip?fields=countryCode
        try {
            val req = Request.Builder()
                .url("http://ip-api.com/line/$ip?fields=countryCode")
                .header("User-Agent", "Mozilla/5.0 BunkerParser/1.1")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val code = resp.body?.string()?.trim()?.uppercase().orEmpty()
                    if (code.length == 2 && CC_RU.containsKey(code)) {
                        return code
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Resolves the country for a CheckItem using Remark -> Domain -> Host DNS -> Online GeoIP.
     */
    suspend fun resolveCountry(item: CheckItem): Pair<String, String> = withContext(Dispatchers.IO) {
        // 1. Check remark
        val fromRemark = extractCountryFromRemark(item.origRemark)
        if (fromRemark != null) {
            return@withContext fromRemark to getCountryName(fromRemark)
        }

        val host = item.host.trim()
        if (host.isEmpty()) return@withContext "" to "Планета 🌐"

        // 2. Check host cache
        cache[host]?.let { return@withContext it }

        // 3. Check domain/host name
        val fromDomain = extractCountryFromDomain(host)
        if (fromDomain != null) {
            val res = fromDomain to getCountryName(fromDomain)
            cache[host] = res
            return@withContext res
        }

        // 4. Resolve IP
        val ip = try {
            InetAddress.getByName(host).hostAddress ?: host
        } catch (_: Exception) {
            host
        }

        cache[ip]?.let {
            cache[host] = it
            return@withContext it
        }

        // 5. Query online GeoIP
        val onlineCode = queryOnlineGeoIp(ip)
        if (onlineCode != null) {
            val res = onlineCode to getCountryName(onlineCode)
            cache[ip] = res
            cache[host] = res
            return@withContext res
        }

        val fallback = "" to "Планета 🌐"
        cache[host] = fallback
        return@withContext fallback
    }

    suspend fun resolveHostCountry(host: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val trimmed = host.trim()
        if (trimmed.isEmpty()) return@withContext "" to "Планета 🌐"

        cache[trimmed]?.let { return@withContext it }

        val fromDomain = extractCountryFromDomain(trimmed)
        if (fromDomain != null) {
            val res = fromDomain to getCountryName(fromDomain)
            cache[trimmed] = res
            return@withContext res
        }

        val ip = try {
            InetAddress.getByName(trimmed).hostAddress ?: trimmed
        } catch (_: Exception) {
            trimmed
        }

        cache[ip]?.let {
            cache[trimmed] = it
            return@withContext it
        }

        val onlineCode = queryOnlineGeoIp(ip)
        if (onlineCode != null) {
            val res = onlineCode to getCountryName(onlineCode)
            cache[ip] = res
            cache[trimmed] = res
            return@withContext res
        }

        val fallback = "" to "Планета 🌐"
        cache[trimmed] = fallback
        return@withContext fallback
    }
}
