package com.bunker.parser.core

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
        return if (flag.isNotEmpty()) "$name $flag" else name
    }

    suspend fun resolveHostCountry(host: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val trimmed = host.trim()
        if (trimmed.isEmpty()) return@withContext "" to "Планета 🌐"
        
        cache[trimmed]?.let { return@withContext it }

        // Try IP resolution if domain
        val ip = try {
            InetAddress.getByName(trimmed).hostAddress ?: trimmed
        } catch (_: Exception) {
            trimmed
        }

        cache[ip]?.let {
            cache[trimmed] = it
            return@withContext it
        }

        // Query ip-api
        try {
            val req = Request.Builder()
                .url("http://ip-api.com/line/$ip?fields=countryCode")
                .header("User-Agent", "Mozilla/5.0 BunkerParser/1.0")
                .build()

            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val code = response.body?.string()?.trim()?.uppercase().orEmpty()
                    if (code.length == 2 && code.all { it in 'A'..'Z' }) {
                        val name = getCountryName(code)
                        val res = code to name
                        cache[ip] = res
                        cache[trimmed] = res
                        return@withContext res
                    }
                }
            }
        } catch (_: Exception) {
        }

        val fallback = "" to "Планета 🌐"
        cache[trimmed] = fallback
        return@withContext fallback
    }
}
