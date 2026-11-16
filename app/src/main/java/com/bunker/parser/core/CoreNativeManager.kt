package com.bunker.parser.core

import android.content.Context
import android.provider.Settings
import android.util.Base64
import go.Seq
import libv2ray.Libv2ray
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

object CoreNativeManager {
    private val initialized = AtomicBoolean(false)
    private val initLock = Any()

    fun isReady(): Boolean = initialized.get()

    fun initCoreEnv(context: Context) {
        if (initialized.get()) return
        synchronized(initLock) {
            if (initialized.get()) return
            try {
                Seq.setContext(context.applicationContext)
                val assetDir = File(context.filesDir, "xray_assets")
                if (!assetDir.exists()) assetDir.mkdirs()

                copyAssetSafely(context, "geoip.dat", File(assetDir, "geoip.dat"))
                copyAssetSafely(context, "geosite.dat", File(assetDir, "geosite.dat"))
                copyAssetSafely(context, "geoip-only-cn-private.dat", File(assetDir, "geoip-only-cn-private.dat"))

                val deviceId = getDeviceIdForXUDP(context)
                Libv2ray.initCoreEnv(assetDir.absolutePath, deviceId)
                initialized.set(true)
            } catch (e: Throwable) {
                initialized.set(false)
            }
        }
    }

    private fun getDeviceIdForXUDP(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?.toByteArray(Charsets.UTF_8) ?: "bunker_parser_unique_seed".toByteArray()
            Base64.encodeToString(androidId.copyOf(32), Base64.NO_PADDING or Base64.URL_SAFE).trim()
        } catch (_: Exception) {
            "YnVua2VyX3BhcnNlcl94dWRwX2tleV9kZWZhdWx0XzMyeA"
        }
    }

    private fun copyAssetSafely(context: Context, assetName: String, destFile: File) {
        val tmpFile = File(destFile.parentFile, "${destFile.name}.tmp")
        try {
            val assetFd = context.assets.openFd(assetName)
            val expectedLen = assetFd.length
            assetFd.close()
            if (destFile.exists() && destFile.length() == expectedLen) {
                return
            }
        } catch (_: Exception) {
            if (destFile.exists() && destFile.length() > 50000L) {
                return
            }
        }

        try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(tmpFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tmpFile.exists() && tmpFile.length() > 0L) {
                if (destFile.exists()) destFile.delete()
                tmpFile.renameTo(destFile)
            }
        } catch (_: Exception) {
            tmpFile.delete()
        }
    }

    fun measureOutboundDelay(config: String, testUrl: String): Long {
        if (!initialized.get()) {
            return -1L
        }
        return try {
            Libv2ray.measureOutboundDelay(config, testUrl)
        } catch (_: Throwable) {
            -1L
        }
    }
}
