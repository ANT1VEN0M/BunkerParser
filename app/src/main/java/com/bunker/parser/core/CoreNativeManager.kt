package com.bunker.parser.core

import android.content.Context
import go.Seq
import libv2ray.Libv2ray
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

object CoreNativeManager {
    private val initialized = AtomicBoolean(false)

    fun initCoreEnv(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            try {
                Seq.setContext(context.applicationContext)
                val assetDir = File(context.filesDir, "xray_assets")
                assetDir.mkdirs()
                copyAssetIfNeeded(context, "geoip.dat", File(assetDir, "geoip.dat"))
                copyAssetIfNeeded(context, "geosite.dat", File(assetDir, "geosite.dat"))
                Libv2ray.initCoreEnv(assetDir.absolutePath, "bunker-parser-client")
            } catch (e: Throwable) {
                initialized.set(false)
            }
        }
    }

    private fun copyAssetIfNeeded(context: Context, assetName: String, destFile: File) {
        if (!destFile.exists() || destFile.length() == 0L) {
            try {
                context.assets.open(assetName).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun measureOutboundDelay(config: String, testUrl: String): Long {
        return try {
            Libv2ray.measureOutboundDelay(config, testUrl)
        } catch (_: Throwable) {
            -1L
        }
    }
}
