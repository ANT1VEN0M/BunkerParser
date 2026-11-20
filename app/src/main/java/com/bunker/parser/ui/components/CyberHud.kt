package com.bunker.parser.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunker.parser.model.CheckerStats
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

@Composable
fun CyberHud(
    stats: CheckerStats,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (statusText, statusColor) = when {
        stats.isPaused -> "ПАУЗА" to Color(0xFFF59E0B)
        stats.isRunning -> "СКАНИРОВАНИЕ" to Color(0xFFEF4444)
        stats.total > 0 && stats.processed >= stats.total -> "ЗАВЕРШЕНО" to Color(0xFF22C55E)
        stats.total > 0 && !stats.isRunning -> "ОСТАНОВЛЕНО" to Color(0xFFF59E0B)
        else -> "ГОТОВ К РАБОТЕ" to Color(0xFFEF4444)
    }

    val progressPercent = if (stats.total > 0) (stats.processed.toFloat() / stats.total.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141414))
            .border(
                1.dp,
                Color(0xFF282828),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            // Status bar header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (stats.isRunning && stats.ratePerSec > 0) {
                        Text(
                            text = String.format("%.1f nod/s", stats.ratePerSec),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                            .then(if (stats.isRunning) Modifier.alpha(pulseAlpha) else Modifier)
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFF282828),
                modifier = Modifier.padding(vertical = 10.dp)
            )

            // 4 Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HudMetricWithIcon(
                    label = "ВСЕГО",
                    value = if (stats.total > 0) "${stats.processed}" else "0",
                    icon = Icons.Default.Storage,
                    iconColor = Color.White,
                    valueColor = TextPrimary
                )
                HudMetricWithIcon(
                    label = "ЖИВЫЕ",
                    value = stats.alive.toString(),
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF22C55E),
                    valueColor = TextPrimary
                )
                HudMetricWithIcon(
                    label = "МЁРТВЫЕ",
                    value = stats.dead.toString(),
                    icon = Icons.Default.Cancel,
                    iconColor = Color(0xFFEF4444),
                    valueColor = TextPrimary
                )
                HudMetricWithIcon(
                    label = "ПРОГРЕСС",
                    value = "${(progressPercent * 100).toInt()}%",
                    icon = Icons.Default.DataUsage,
                    iconColor = Color(0xFFDC2626),
                    valueColor = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sleek glowing progress slider with dot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF222222))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressPercent)
                        .height(6.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFDC2626),
                                    Color(0xFFEF4444),
                                    Color.White
                                )
                            )
                        )
                )
            }

            if (stats.currentBatch.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stats.currentBatch,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HudMetricWithIcon(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = valueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
