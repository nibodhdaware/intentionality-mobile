package com.nibodhdaware.intentionality

import android.app.Application

class IntentionalityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: IntentionalityApp
            private set
    }
}

