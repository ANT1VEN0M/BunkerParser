package com.bunker.parser.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bunker.parser.core.CheckEngine
import com.bunker.parser.core.CoreNativeManager
import com.bunker.parser.core.UriParser
import com.bunker.parser.model.BunkerCloudFeed
import com.bunker.parser.model.CheckItem
import com.bunker.parser.model.CheckResult
import com.bunker.parser.model.CheckerStats
import com.bunker.parser.model.ParseMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val checkEngine = CheckEngine()

    private val _selectedMode = MutableStateFlow(ParseMode.MIX)
    val selectedMode: StateFlow<ParseMode> = _selectedMode.asStateFlow()

    private val _minServices = MutableStateFlow(1)
    val minServices: StateFlow<Int> = _minServices.asStateFlow()

    private val _strictAll4 = MutableStateFlow(false)
    val strictAll4: StateFlow<Boolean> = _strictAll4.asStateFlow()

    private val _onlyFactoryPorts = MutableStateFlow(false)
    val onlyFactoryPorts: StateFlow<Boolean> = _onlyFactoryPorts.asStateFlow()

    private val _workers = MutableStateFlow(32)
    val workers: StateFlow<Int> = _workers.asStateFlow()

    private val _timeoutSeconds = MutableStateFlow(2.5f)
    val timeoutSeconds: StateFlow<Float> = _timeoutSeconds.asStateFlow()

    private val _parsedItems = MutableStateFlow<List<CheckItem>>(emptyList())
    val parsedItems: StateFlow<List<CheckItem>> = _parsedItems.asStateFlow()

    val stats: StateFlow<CheckerStats> = checkEngine.stats
    val results: StateFlow<List<CheckResult>> = checkEngine.results

    private val _customBgPath = MutableStateFlow<String?>(null)
    val customBgPath: StateFlow<String?> = _customBgPath.asStateFlow()

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    init {
        loadSavedBackground()
    }

    private fun loadSavedBackground() {
        try {
            val context = getApplication<Application>()
            val bgFile = File(context.filesDir, "custom_background.jpg")
            if (bgFile.exists() && bgFile.length() > 0) {
                _customBgPath.value = bgFile.absolutePath
            }
        } catch (_: Exception) {}
    }

    fun setCustomBackground(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val bgFile = File(context.filesDir, "custom_background.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(bgFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (bgFile.exists() && bgFile.length() > 0) {
                    _customBgPath.value = bgFile.absolutePath
                    _uiEvents.emit("Фон приложения обновлен 🖼️")
                }
            } catch (e: Exception) {
                _uiEvents.emit("Ошибка установки фона: ${e.message}")
            }
        }
    }

    fun resetCustomBackground() {
        try {
            val context = getApplication<Application>()
            val bgFile = File(context.filesDir, "custom_background.jpg")
            if (bgFile.exists()) {
                bgFile.delete()
            }
            _customBgPath.value = null
            viewModelScope.launch {
                _uiEvents.emit("Фон сброшен к стандартному 🖤")
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _uiEvents.emit("Ошибка сброса фона: ${e.message}")
            }
        }
    }

    fun setMode(mode: ParseMode) {
        _selectedMode.value = mode
        _minServices.value = mode.defaultMinServices
        _strictAll4.value = mode.defaultStrict4
    }

    fun setMinServices(count: Int) {
        val coerced = count.coerceIn(1, 4)
        _minServices.value = coerced
        _strictAll4.value = (coerced >= 4)
    }

    fun setStrictAll4(value: Boolean) {
        _strictAll4.value = value
        _minServices.value = if (value) 4 else 1
    }

    fun setOnlyFactoryPorts(enabled: Boolean) {
        _onlyFactoryPorts.value = enabled
    }

    fun setWorkers(count: Int) {
        _workers.value = count.coerceIn(4, 64)
    }

    fun setTimeout(seconds: Float) {
        _timeoutSeconds.value = seconds.coerceIn(1.0f, 6.0f)
    }

    fun loadFromText(rawText: String) {
        val statsResult = UriParser.extractWithStats(rawText, deduplicate = true)
        _parsedItems.value = statsResult.items
        viewModelScope.launch {
            if (statsResult.duplicatesRemoved > 0) {
                _uiEvents.emit("Загружено: ${statsResult.items.size} (отсеяно ${statsResult.duplicatesRemoved} дублей)")
            } else {
                _uiEvents.emit("Загружено конфигов: ${statsResult.items.size}")
            }
        }
    }

    fun loadFromFileUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: ""
                val statsResult = UriParser.extractWithStats(content, deduplicate = true)
                _parsedItems.value = statsResult.items
                if (statsResult.duplicatesRemoved > 0) {
                    _uiEvents.emit("Загружено из файла: ${statsResult.items.size} (отсеяно ${statsResult.duplicatesRemoved} дублей)")
                } else {
                    _uiEvents.emit("Загружено из файла: ${statsResult.items.size} конфигов")
                }
            } catch (e: Exception) {
                _uiEvents.emit("Ошибка чтения файла: ${e.message}")
            }
        }
    }

    fun loadFromUrl(url: String) {
        val trimmedUrl = url.trim()
        if (!trimmedUrl.startsWith("http://", ignoreCase = true) && !trimmedUrl.startsWith("https://", ignoreCase = true)) {
            viewModelScope.launch {
                _uiEvents.emit("Ошибка: ссылка должна начинаться с http:// или https://")
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiEvents.emit("Загрузка подписки по ссылке 🌐...")
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()

                val request = Request.Builder()
                    .url(trimmedUrl)
                    .header("User-Agent", "BunkerParser/1.2.8 (Android; v2rayN)")
                    .header("Accept", "*/*")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _uiEvents.emit("Ошибка сети: код ${response.code}")
                        return@use
                    }
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        _uiEvents.emit("Ошибка: пустой ответ от сервера")
                        return@use
                    }
                    val statsResult = UriParser.extractWithStats(body, deduplicate = true)
                    _parsedItems.value = statsResult.items
                    if (statsResult.duplicatesRemoved > 0) {
                        _uiEvents.emit("Загружено по ссылке: ${statsResult.items.size} (отсеяно ${statsResult.duplicatesRemoved} дублей)")
                    } else {
                        _uiEvents.emit("Загружено по ссылке: ${statsResult.items.size} конфигов")
                    }
                }
            } catch (e: Exception) {
                _uiEvents.emit("Ошибка загрузки: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun loadFromCloud(feed: BunkerCloudFeed) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiEvents.emit("☁️ Загрузка из Облака Бункера: ${feed.title}...")
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()

                var body = ""
                // Try primary jsDelivr CDN, then GitHub Pages mirror
                for (targetUrl in listOf(feed.url, feed.mirrorUrl)) {
                    try {
                        val request = okhttp3.Request.Builder()
                            .url(targetUrl)
                            .header("User-Agent", "BunkerParser/1.3.0 (Android; CloudEngine)")
                            .header("Accept", "*/*")
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val text = response.body?.string().orEmpty()
                                if (text.isNotBlank()) {
                                    body = text
                                }
                            }
                        }
                        if (body.isNotBlank()) break
                    } catch (_: Exception) {
                        // continue to mirror
                    }
                }

                if (body.isBlank()) {
                    _uiEvents.emit("❌ Не удалось загрузить [${feed.title}] из облака")
                    return@launch
                }

                val statsResult = UriParser.extractWithStats(body, deduplicate = true)
                _parsedItems.value = statsResult.items
                if (statsResult.duplicatesRemoved > 0) {
                    _uiEvents.emit("☁️ Облако [${feed.title}]: ${statsResult.items.size} нод (удалено ${statsResult.duplicatesRemoved} дублей)")
                } else {
                    _uiEvents.emit("☁️ Облако [${feed.title}]: ${statsResult.items.size} живых нод")
                }
            } catch (e: Exception) {
                _uiEvents.emit("Ошибка облака: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun loadFromClipboard() {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val item = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        if (item.isNotBlank()) {
            loadFromText(item)
        } else {
            viewModelScope.launch {
                _uiEvents.emit("Буфер обмена пуст")
            }
        }
    }

    fun startChecking() {
        val items = _parsedItems.value
        if (items.isEmpty()) {
            viewModelScope.launch {
                _uiEvents.emit("Сначала загрузите или вставьте конфиги!")
            }
            return
        }

        viewModelScope.launch {
            val context = getApplication<Application>()
            if (!CoreNativeManager.isReady()) {
                CoreNativeManager.initCoreEnv(context)
            }
            checkEngine.start(
                items = items,
                mode = _selectedMode.value,
                minServices = _minServices.value,
                workers = _workers.value,
                timeoutSec = _timeoutSeconds.value,
                onlyFactoryPorts = _onlyFactoryPorts.value
            )
        }
    }

    fun stopChecking() {
        checkEngine.stop()
    }

    fun pauseChecking() {
        checkEngine.pause()
    }

    fun resumeChecking() {
        checkEngine.resume()
    }

    fun togglePauseResume() {
        if (checkEngine.isPaused()) {
            checkEngine.resume()
        } else if (checkEngine.isRunning()) {
            checkEngine.pause()
        }
    }

    fun copyResultsToClipboard() {
        val alive = results.value
        if (alive.isEmpty()) {
            viewModelScope.launch { _uiEvents.emit("Нет рабочих конфигов для копирования") }
            return
        }
        val exportText = checkEngine.exportSubscription(alive, _selectedMode.value)
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("BunkerConfigs", exportText)
        clipboard?.setPrimaryClip(clip)
        viewModelScope.launch {
            _uiEvents.emit("Скопировано ${alive.size} конфигов в буфер!")
        }
    }

    fun copySingleNode(rawUri: String) {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("BunkerNode", rawUri)
        clipboard?.setPrimaryClip(clip)
        viewModelScope.launch {
            _uiEvents.emit("Конфиг скопирован!")
        }
    }

    fun saveResultsToDownloads() {
        val alive = results.value
        if (alive.isEmpty()) {
            viewModelScope.launch { _uiEvents.emit("Нет рабочих конфигов для сохранения") }
            return
        }

        val exportText = checkEngine.exportSubscription(alive, _selectedMode.value)
        val fileName = _selectedMode.value.fileName

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { stream ->
                            stream.write(exportText.toByteArray(Charsets.UTF_8))
                        }
                        true
                    } ?: false
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val targetFile = File(downloadsDir, fileName)
                    FileOutputStream(targetFile).use { stream ->
                        stream.write(exportText.toByteArray(Charsets.UTF_8))
                    }
                    true
                }

                if (saved) {
                    _uiEvents.emit("Успешно сохранено: Downloads/$fileName")
                } else {
                    _uiEvents.emit("Не удалось записать файл")
                }
            } catch (e: Exception) {
                _uiEvents.emit("Ошибка сохранения: ${e.message}")
            }
        }
    }

    fun getExportText(): String {
        return checkEngine.exportSubscription(results.value, _selectedMode.value)
    }

    fun clear() {
        stopChecking()
        _parsedItems.value = emptyList()
    }
}
