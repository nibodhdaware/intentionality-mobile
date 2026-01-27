package com.nibodhdaware.intentionality

import android.app.Application
import com.nibodhdaware.intentionality.billing.BillingManager
import com.nibodhdaware.intentionality.api.ApiManager

class IntentionalityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize API Manager
        ApiManager.initialize(this)
        
        // Initialize RevenueCat billing
        BillingManager.initialize(this)
    }

    companion object {
        lateinit var instance: IntentionalityApp
            private set
    }
}

