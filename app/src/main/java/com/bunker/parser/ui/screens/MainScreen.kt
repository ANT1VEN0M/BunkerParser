package com.bunker.parser.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.bunker.parser.R
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunker.parser.model.ParseMode
import com.bunker.parser.ui.components.AnalyticsBlock
import com.bunker.parser.ui.components.ConfigCard
import com.bunker.parser.ui.components.CyberHud
import com.bunker.parser.ui.theme.BunkerBg
import com.bunker.parser.ui.theme.BunkerCardBorder
import com.bunker.parser.ui.theme.BunkerSurface
import com.bunker.parser.ui.theme.BunkerSurfaceVariant
import com.bunker.parser.ui.theme.NeonAmber
import com.bunker.parser.ui.theme.NeonCyan
import com.bunker.parser.ui.theme.NeonGreen
import com.bunker.parser.ui.theme.NeonPurple
import com.bunker.parser.ui.theme.NeonPurpleDark
import com.bunker.parser.ui.theme.NeonRed
import com.bunker.parser.ui.theme.TextMuted
import com.bunker.parser.ui.theme.TextPrimary
import com.bunker.parser.ui.theme.TextSecondary
import com.bunker.parser.ui.util.ImageUtils
import com.bunker.parser.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val selectedMode by viewModel.selectedMode.collectAsState()
    val minServices by viewModel.minServices.collectAsState()
    val strictAll4 by viewModel.strictAll4.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val timeoutSec by viewModel.timeoutSeconds.collectAsState()
    val onlyFactoryPorts by viewModel.onlyFactoryPorts.collectAsState()
    val parsedItems by viewModel.parsedItems.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val results by viewModel.results.collectAsState()
    val customBgPath by viewModel.customBgPath.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var inputUrlText by remember { mutableStateOf("") }

    val customBgBitmap = remember(customBgPath) {
        ImageUtils.loadScaledBitmap(customBgPath)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.loadFromFileUri(it) }
    }

    val bgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setCustomBackground(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background layer: custom image or default cyberpunk dark
        if (customBgBitmap != null) {
            Image(
                bitmap = customBgBitmap,
                contentDescription = "Фон приложения",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Cyber dimming overlay for perfect contrast & readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xD90B0914),
                                Color(0xE60B0914),
                                Color(0xF20B0914)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF090514),
                                Color(0xFF0F0921),
                                Color(0xFF080412)
                            )
                        )
                    )
            )
        }

        // Master scrollable list covering entire screen with bottom dock inset
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Item 1: Top App Bar / Header (Matching User Mockup)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.bunker_logo),
                            contentDescription = "Bunker Crown Logo",
                            modifier = Modifier
                                .size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BUNKER PARSER",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.1.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF8B2FC9))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "v1.2.8",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = "Быстрый парсинг · Надёжный результат",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Header Right Icon Actions (Telegram, Channel, Settings, Theme)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Telegram Bot / Channel Action
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/BUNKER_KOTA"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1533))
                                .border(1.dp, Color(0xFF3C2766), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Telegram",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Community Chat Action
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/BUNKER_K0TA"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1533))
                                .border(1.dp, Color(0xFF3C2766), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Community",
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Settings button
                        IconButton(
                            onClick = { showSettings = !showSettings },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (showSettings) Color(0xFF3B1D66) else Color(0xFF1E1533))
                                .border(1.dp, Color(0xFF3C2766), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Настройки",
                                tint = if (showSettings) NeonCyan else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Wallpaper picker button
                        IconButton(
                            onClick = { bgPickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (customBgPath != null) Color(0xFF3B1D66) else Color(0xFF1E1533))
                                .border(1.dp, if (customBgPath != null) NeonCyan else Color(0xFF3C2766), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = "Фон",
                                tint = if (customBgPath != null) NeonCyan else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Item 2: Mode Selector Tabs (Matching Mockup with rich gradients)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF140D26))
                        .border(1.2.dp, Color(0xFF341E5C), RoundedCornerShape(14.dp))
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ParseMode.entries.forEach { mode ->
                        val isSelected = mode == selectedMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF8B2FC9),
                                                Color(0xFF6B18B5)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF1A1230), Color(0xFF1A1230))
                                        )
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFA855F7) else Color(0xFF281C44),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setMode(mode) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mode.title} ${mode.heart}",
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Item 2.1: Factory / Anti-Jammer Quick Banner (When BS is selected)
            if (selectedMode == ParseMode.BS) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF160E2A))
                            .border(1.dp, if (onlyFactoryPorts) NeonCyan else Color(0xFF381F66), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🏭", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Анти-глушилка (Режим Завод / Проходная)",
                                    color = if (onlyFactoryPorts) NeonCyan else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Оставлять только порты 443 и 53 (DNS)",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = onlyFactoryPorts,
                            onCheckedChange = { viewModel.setOnlyFactoryPorts(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BunkerBg,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = BunkerSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Item 2.5: Filter Services Card (Matching Mockup)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF140D26))
                        .border(1.2.dp, Color(0xFF341E5C), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ФИЛЬТР СЕРВИСОВ",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = when (minServices) {
                                1 -> "1+ (Все живые связки) ⌄"
                                2 -> "2+ (Дуэты и выше) ⌄"
                                3 -> "3+ (Трио и 4/4) ⌄"
                                else -> if (selectedMode == ParseMode.BS) "4/4 (Строгий YA×VK×TG×INTL) ⌄" else "4/4 (Строгий YT×TG×GPT×GEM) ⌄"
                            },
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filterTiers = listOf(
                            1 to "1+ Все",
                            2 to "2+ Связки",
                            3 to "3+ Трио",
                            4 to "4/4 Строго"
                        )
                        filterTiers.forEach { (count, label) ->
                            val isSelected = minServices == count
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFF0F2C3D) else Color(0xFF1B1430)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan else Color(0xFF2C2048),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setMinServices(count) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) NeonCyan else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Item 3: Settings collapsible panel
            item {
                AnimatedVisibility(visible = showSettings) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF140D26))
                            .border(1.dp, BunkerCardBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        // Custom Background Settings Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Фоновое изображение",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (customBgPath != null) "Установлено фото из галереи" else "Стандартный тёмный киберпанк",
                                    color = if (customBgPath != null) NeonCyan else TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (customBgPath != null) {
                                    OutlinedButton(
                                        onClick = { viewModel.resetCustomBackground() },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed)
                                    ) {
                                        Text("Сброс", fontSize = 11.sp)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { bgPickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                                ) {
                                    Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (customBgPath != null) "Сменить" else "Галерея", fontSize = 11.sp)
                                }
                            }
                        }

                        HorizontalDivider(
                            color = BunkerCardBorder,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        // Strict 4/4 Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Строгий отбор 4/4 сервисов",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Только ноды прошедшие YT + TG + ChatGPT + Gemini",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = strictAll4,
                                onCheckedChange = { viewModel.setStrictAll4(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BunkerBg,
                                    checkedTrackColor = NeonPurple,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = BunkerSurfaceVariant
                                )
                            )
                        }

                        HorizontalDivider(
                            color = BunkerCardBorder,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        // Factory Ports (443 and 53) Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Фильтр портов 443 и 53 (Завод / Анти-глушилка)",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Отсекать порты 9000+, оставляя только 443 (HTTPS) и 53 (DNS) для пробива вышек у заводов",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = onlyFactoryPorts,
                                onCheckedChange = { viewModel.setOnlyFactoryPorts(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BunkerBg,
                                    checkedTrackColor = NeonCyan,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = BunkerSurfaceVariant
                                )
                            )
                        }

                        HorizontalDivider(
                            color = BunkerCardBorder,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        // Workers Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Параллельные потоки (Turbo)", color = TextSecondary, fontSize = 12.sp)
                            Text("$workers потоков", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = workers.toFloat(),
                            onValueChange = { viewModel.setWorkers(it.toInt()) },
                            valueRange = 8f..64f,
                            steps = 13,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = BunkerSurfaceVariant
                            )
                        )

                        // Timeout Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Таймаут проверки", color = TextSecondary, fontSize = 12.sp)
                            Text(String.format("%.1f сек", timeoutSec), color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = timeoutSec,
                            onValueChange = { viewModel.setTimeout(it) },
                            valueRange = 1.0f..5.0f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonPurple,
                                activeTrackColor = NeonPurple,
                                inactiveTrackColor = BunkerSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Item 4: Input buttons row (TXT, Clipboard, URL)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("text/plain") },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF140D26),
                            contentColor = TextPrimary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF8B2FC9), Color(0xFF6B18B5))
                            )
                        )
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFA855F7))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TXT", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.loadFromClipboard() },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF140D26),
                            contentColor = TextPrimary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF06B6D4))
                            )
                        )
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Буфер", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { showUrlDialog = true },
                        modifier = Modifier
                            .weight(1.15f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF140D26),
                            contentColor = TextPrimary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF059669), Color(0xFF10B981))
                            )
                        )
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("По URL 🌐", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Item 5: Node count info (if loaded)
            if (parsedItems.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Загружено нод: ${parsedItems.size}",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(
                            onClick = { viewModel.clear() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Очистить",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Item 6: Cyber Master Action Button (Start / Pause / Resume / Stop)
            item {
                if (stats.isRunning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pause / Resume Main Button
                        Button(
                            onClick = { viewModel.togglePauseResume() },
                            modifier = Modifier
                                .weight(2.5f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (stats.isPaused) Color(0xFF0D9488) else Color(0xFFD97706)
                            )
                        ) {
                            Icon(
                                imageVector = if (stats.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (stats.isPaused) "ПРОДОЛЖИТЬ ▶" else "ПАУЗА ⏸",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                        }

                        // Stop / Reset Button
                        OutlinedButton(
                            onClick = { viewModel.stopChecking() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(
                                    listOf(NeonRed, Color(0xFFB91C1C))
                                )
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("СТОП", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.startChecking() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7E22CE)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "НАЧАТЬ ПАРСИНГ И ТЕСТ ⚡",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Item 7: Cyber HUD (Matching Mockup with 4 metrics & neon progress bar)
            item {
                CyberHud(stats = stats)
            }

            // Item 8: Inline Export Action Buttons
            if (results.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.copyResultsToClipboard() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonGreen, NeonCyan)))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Копия TXT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.saveResultsToDownloads() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("В Загрузки", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val text = viewModel.getExportText()
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, selectedMode.fileName)
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Поделиться подпиской"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonPurple, NeonPurpleDark)))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Поделиться", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Item 9: Live Analytics Block (Countries & Protocols)
                item {
                    AnalyticsBlock(results = results)
                }
            }

            // Item 10: Results List Cards
            itemsIndexed(results) { index, item ->
                ConfigCard(
                    result = item,
                    index = index + 1,
                    onCopy = { viewModel.copySingleNode(it) }
                )
            }
        }

        // Sticky Bottom Cyber Dock for instant export without having to scroll
        AnimatedVisibility(
            visible = results.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF0120B24))
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(Color(0xFF8B2FC9), Color(0xFF06B6D4), Color(0xFF10B981))),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.copyResultsToClipboard() },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = NeonGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "КОПИЯ TXT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = { viewModel.saveResultsToDownloads() },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1533))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = NeonCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "СКАЧАТЬ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            val text = viewModel.getExportText()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, selectedMode.fileName)
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Поделиться подпиской"))
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1533))
                            .border(1.dp, Color(0xFF3C2766), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поделиться",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Dialog for loading subscription by URL
        if (showUrlDialog) {
            AlertDialog(
                onDismissRequest = { showUrlDialog = false },
                containerColor = Color(0xFF150E28),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Загрузить по ссылке",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Вставьте прямую ссылку на подписку (HTTP/HTTPS). Автоматически поддерживается Base64 и дедупликация нод.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = inputUrlText,
                            onValueChange = { inputUrlText = it },
                            placeholder = { Text("https://example.com/sub/...", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0xFF3C2766),
                                cursorColor = NeonCyan
                            )
                        )
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                if (clip.isNotBlank()) {
                                    inputUrlText = clip
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(NeonCyan, Color(0xFF0284C7)))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Вставить из буфера 📋", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputUrlText.isNotBlank()) {
                                viewModel.loadFromUrl(inputUrlText)
                                showUrlDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Загрузить ⚡", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showUrlDialog = false },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Отмена", fontSize = 12.sp)
                    }
                }
            )
        }
    }
}
