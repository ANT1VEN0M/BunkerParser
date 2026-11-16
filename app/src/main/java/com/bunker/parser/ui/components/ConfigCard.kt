package com.bunker.parser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunker.parser.model.CheckResult
import com.bunker.parser.ui.theme.BunkerCardBorder
import com.bunker.parser.ui.theme.BunkerSurface
import com.bunker.parser.ui.theme.BunkerSurfaceVariant
import com.bunker.parser.ui.theme.NeonAmber
import com.bunker.parser.ui.theme.NeonCyan
import com.bunker.parser.ui.theme.NeonGreen
import com.bunker.parser.ui.theme.NeonRed
import com.bunker.parser.ui.theme.TextMuted
import com.bunker.parser.ui.theme.TextPrimary
import com.bunker.parser.ui.theme.TextSecondary

@Composable
fun ConfigCard(
    result: CheckResult,
    index: Int,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pingColor = when {
        result.speedMs < 250 -> NeonGreen
        result.speedMs < 500 -> NeonAmber
        else -> NeonRed
    }

    val displayCountry = if (result.countryName.isNotBlank()) "${result.flag} ${result.countryName}".trim() else "Планета 🌐"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BunkerSurface)
            .border(1.dp, BunkerCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            // Header: Flag + Name + Protocol + Ping
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = displayCountry,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ProtocolBadge(protocol = result.item.scheme.uppercase())
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = result.speedLabel,
                        color = pingColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onCopy(result.finalUri.ifEmpty { result.item.rawUri }) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle: Host:Port and remarks
            Text(
                text = "${result.item.host}:${result.item.port}  •  ${result.item.origRemark.ifEmpty { "Node #$index" }}",
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Service chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ServiceChip(name = "YT", passed = result.tags.contains("YT"))
                ServiceChip(name = "TG", passed = result.tags.contains("TG"))
                ServiceChip(name = "GPT", passed = result.tags.contains("GPT"))
                ServiceChip(name = "GEM", passed = result.tags.contains("GEM"))
            }
        }
    }
}

@Composable
private fun ProtocolBadge(protocol: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(BunkerSurfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = protocol,
            color = NeonCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ServiceChip(name: String, passed: Boolean) {
    val bg = if (passed) NeonGreen.copy(alpha = 0.15f) else BunkerSurfaceVariant
    val border = if (passed) NeonGreen.copy(alpha = 0.5f) else BunkerCardBorder
    val textColor = if (passed) NeonGreen else TextMuted

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (passed) "$name ✓" else "$name ✗",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
