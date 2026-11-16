package com.bunker.parser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunker.parser.core.GeoIpManager
import com.bunker.parser.model.CheckResult
import com.bunker.parser.ui.theme.BunkerCardBorder
import com.bunker.parser.ui.theme.BunkerSurface
import com.bunker.parser.ui.theme.BunkerSurfaceVariant
import com.bunker.parser.ui.theme.NeonAmber
import com.bunker.parser.ui.theme.NeonCyan
import com.bunker.parser.ui.theme.NeonGreen
import com.bunker.parser.ui.theme.NeonPurple
import com.bunker.parser.ui.theme.NeonRed
import com.bunker.parser.ui.theme.TextMuted
import com.bunker.parser.ui.theme.TextPrimary
import com.bunker.parser.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsBlock(
    results: List<CheckResult>,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) return

    // Grouping by Country
    val countryStats = remember(results) {
        val counts = HashMap<String, Pair<String, Int>>() // code -> Pair(nameWithFlag, count)
        results.forEach { cr ->
            val code = cr.countryCode.ifEmpty { "OTHER" }
            val flag = if (cr.flag.isNotBlank() && cr.flag != "🌐") cr.flag else GeoIpManager.getFlagEmoji(code)
            val name = if (cr.countryName.isNotBlank() && cr.countryName != "Планета 🌐") cr.countryName else GeoIpManager.getCountryName(code)
            val label = "$flag $name".trim()
            val current = counts[code]
            if (current == null) {
                counts[code] = label to 1
            } else {
                counts[code] = current.first to (current.second + 1)
            }
        }
        counts.values.sortedByDescending { it.second }
    }

    // Grouping by Protocol
    val protocolStats = remember(results) {
        val counts = HashMap<String, Int>()
        results.forEach { cr ->
            val proto = cr.item.scheme.uppercase()
            counts[proto] = (counts[proto] ?: 0) + 1
        }
        counts.toList().sortedByDescending { it.second }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BunkerSurface)
            .border(1.dp, BunkerCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "АНАЛИТИКА СЕТИ",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "Рабочих: ${results.size}",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Protocols Section
            Text(
                text = "ПРОТОКОЛЫ ТУННЕЛЕЙ",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                protocolStats.forEach { (proto, count) ->
                    val color = when (proto) {
                        "VLESS" -> NeonPurple
                        "VMESS" -> NeonCyan
                        "TROJAN" -> NeonAmber
                        "SS", "SHADOWSOCKS" -> NeonGreen
                        "HYSTERIA2", "HY2" -> NeonRed
                        else -> TextPrimary
                    }
                    ProtocolChip(proto = proto, count = count, color = color)
                }
            }

            HorizontalDivider(
                color = BunkerCardBorder,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            // Countries Section
            Text(
                text = "НАЙДЕННЫЕ СТРАНЫ (${countryStats.size})",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                countryStats.forEach { (label, count) ->
                    CountryChip(label = label, count = count)
                }
            }
        }
    }
}

@Composable
private fun ProtocolChip(
    proto: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BunkerSurfaceVariant)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⚡ $proto",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = count.toString(),
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun CountryChip(
    label: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BunkerSurfaceVariant)
            .border(1.dp, BunkerCardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(NeonCyan.copy(alpha = 0.2f))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = count.toString(),
                color = NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
