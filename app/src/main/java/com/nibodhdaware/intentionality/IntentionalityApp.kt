package com.nibodhdaware.intentionality

import android.app.Application
import com.nibodhdaware.intentionality.billing.BillingManager

class IntentionalityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize RevenueCat billing
        BillingManager.initialize(this)
    }

    companion object {
        lateinit var instance: IntentionalityApp
            private set
    }
}

