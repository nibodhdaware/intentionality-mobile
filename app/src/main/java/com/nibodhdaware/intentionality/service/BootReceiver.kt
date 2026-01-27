package com.nibodhdaware.intentionality.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED) {
            
            Log.d(TAG, "Boot/restart detected, checking if monitoring should start automatically")
            
            // Check if monitoring was previously active
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val wasMonitoring = sharedPrefs.getBoolean("is_monitoring", false)
            
            if (wasMonitoring) {
                Log.d(TAG, "Previous monitoring state was active, starting service automatically")
                
                try {
                    // Start the monitoring service
                    val serviceIntent = Intent(context, AppMonitorService::class.java)
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    
                    Log.d(TAG, "AppMonitorService started automatically")
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting service from boot receiver", e)
                }
            } else {
                Log.d(TAG, "Previous monitoring state was inactive, not starting service")
            }
        }
    }
}