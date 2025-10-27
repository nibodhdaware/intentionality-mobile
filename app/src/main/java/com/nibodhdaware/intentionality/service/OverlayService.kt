package com.nibodhdaware.intentionality.service

import android.app.ActivityManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nibodhdaware.intentionality.ui.prompt.IntentionOverlayView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var monitorJob: Job? = null
    private var monitoredPackageName: String? = null

    companion object {
        private const val TAG = "OverlayService"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PACKAGE_NAME = "package_name"
        private const val CHECK_INTERVAL_MS = 500L // Check every 500ms for faster dismissal
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry 
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        Log.d(TAG, "OverlayService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "===== OverlayService onStartCommand =====")

        val appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: "Unknown App"
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""

        Log.d(TAG, "Showing overlay for app: $appName ($packageName)")

        // Dismiss any existing overlay first
        dismissOverlay()

        // Store the monitored package name
        monitoredPackageName = packageName

        // Show new overlay
        showOverlay(appName, packageName)
        
        // Start monitoring if monitored app goes away
        startMonitoringForegroundApp()

        return START_NOT_STICKY
    }

    private fun showOverlay(appName: String, packageName: String) {
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.CENTER

            // Create ComposeView for the overlay
            overlayView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)

                setContent {
                    IntentionOverlayView(
                        appName = appName,
                        packageName = packageName,
                        onProceed = { reason, rating ->
                            Log.d(TAG, "Overlay submitted: reason='$reason', rating=$rating")
                            launchApp(packageName)
                            dismissOverlay()
                            stopSelf()
                        },
                        onGoBack = {
                            Log.d(TAG, "Go back pressed")
                            goToHomeScreen()
                            dismissOverlay()
                            stopSelf()
                        }
                    )
                }
            }

            windowManager?.addView(overlayView, params)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            Log.d(TAG, "✅ Overlay displayed successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing overlay", e)
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun launchApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launchIntent != null) {
                Log.d(TAG, "Launching app: $packageName")
                startActivity(launchIntent)
            } else {
                Log.e(TAG, "No launch intent found for: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app", e)
        }
    }

    private fun goToHomeScreen() {
        // Go to home screen
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        
        // Kill the monitored app if package name is available
        monitoredPackageName?.let { packageName ->
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.killBackgroundProcesses(packageName)
                Log.d(TAG, "Killed app: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to kill app: $packageName", e)
            }
        }
    }
    
    private fun startMonitoringForegroundApp() {
        // Cancel any existing monitoring job
        monitorJob?.cancel()
        
        monitorJob = serviceScope.launch(Dispatchers.IO) {
            while (true) {
                delay(CHECK_INTERVAL_MS)
                
                try {
                    val foregroundApp = getForegroundApp()
                    
                    // If the overlay is showing but the monitored app is no longer in foreground
                    // AND it's not our own app (which would be the overlay)
                    if (foregroundApp != null && 
                        foregroundApp != monitoredPackageName &&
                        foregroundApp != packageName) {
                        Log.d(TAG, "Monitored app ($monitoredPackageName) is no longer in foreground. Current: $foregroundApp. Dismissing overlay.")
                        launch(Dispatchers.Main) {
                            dismissOverlay()
                            stopSelf()
                        }
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking foreground app", e)
                }
            }
        }
    }
    
    private fun getForegroundApp(): String? {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 2000,
                time
            )
            usageStats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground app", e)
            null
        }
    }

    private fun dismissOverlay() {
        try {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            if (overlayView != null && overlayView?.windowToken != null) {
                windowManager?.removeView(overlayView)
                Log.d(TAG, "Overlay dismissed")
            }
            overlayView = null
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing overlay", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "OverlayService destroyed")
        monitorJob?.cancel()
        dismissOverlay()
        serviceScope.cancel()
        store.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

