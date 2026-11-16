package com.bunker.parser

import android.app.Application
import com.bunker.parser.core.CoreNativeManager

class BunkerParserApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            CoreNativeManager.initCoreEnv(this)
        } catch (_: Throwable) {
        }
    }
}
