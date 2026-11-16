package com.bunker.parser

import android.app.Application
import com.bunker.parser.core.CoreNativeManager

class BunkerParserApp : Application() {
    companion object {
        lateinit var instance: BunkerParserApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Thread {
            try {
                CoreNativeManager.initCoreEnv(this)
            } catch (_: Throwable) {
            }
        }.start()
    }
}
