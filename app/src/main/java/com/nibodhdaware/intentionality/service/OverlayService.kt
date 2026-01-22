package com.nibodhdaware.intentionality.service

import android.app.ActivityManager
import android.app.Service
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
import com.nibodhdaware.intentionality.database.AppDatabase
import com.nibodhdaware.intentionality.database.IntentionLog
import com.nibodhdaware.intentionality.ui.prompt.IntentionOverlayView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.nibodhdaware.intentionality.billing.BillingManager // Added import
import com.nibodhdaware.intentionality.firebase.FirebaseManager // Added import
import android.content.pm.PackageManager

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var monitoredPackageName: String? = null
    
    // Debouncing and state management
    private var isOverlayShowing = false
    private var lastDismissTime = 0L
    private val DISMISS_DEBOUNCE_MS = 2000L  // Increased from 1000L to prevent glitching

    companion object {
        private const val TAG = "OverlayService"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_CUSTOM_INTENTION = "custom_intention"
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
        val customIntention = intent?.getStringExtra(EXTRA_CUSTOM_INTENTION) ?: ""

        Log.d(TAG, "Showing overlay for app: $appName ($packageName), customIntention: ${if (customIntention.isNotBlank()) "'$customIntention'" else "(default)"}")

        // Debounce check - prevent rapid recreation
        val currentTime = System.currentTimeMillis()
        if (isOverlayShowing && currentTime - lastDismissTime < DISMISS_DEBOUNCE_MS) {
            Log.d(TAG, "⚠️ Ignoring overlay request - too soon after dismissal (${currentTime - lastDismissTime}ms)")
            return START_NOT_STICKY
        }

        // Dismiss any existing overlay first
        dismissOverlay()

        // Store the monitored package name
        monitoredPackageName = packageName

        // Show new overlay
        showOverlay(appName, packageName, customIntention)

        return START_NOT_STICKY
    }

    private fun showOverlay(appName: String, packageName: String, customIntention: String) {
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
            // Prevent keyboard from showing and interfering with overlay
            params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING

            // Create ComposeView for the overlay
            overlayView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)

                setContent {
                    IntentionOverlayView(
                        appName = appName,
                        packageName = packageName,
                        customIntention = customIntention,
                        onProceed = { reason, rating ->
                            Log.d(TAG, "Overlay submitted: reason='$reason', rating=$rating")
                            // Save to database
                            saveIntentionLog(packageName, appName, reason, rating)
                            // Reset flags FIRST before any other action
                            AppMonitorService.isOverlayVisible = false
                            AppMonitorService.currentMonitoredApp = null
                            launchApp(packageName)
                            dismissOverlay()
                            stopSelf()
                        },
                        onGoBack = {
                            Log.d(TAG, "Go back pressed")
                            // Mark this app to reset timer to now (interval restarts)
                            monitoredPackageName?.let { pkg ->
                                AppMonitorService.appsToResetTimer.add(pkg)
                                Log.d(TAG, "Marked $pkg for timer reset")
                            }
                            // Reset flags FIRST before any other action
                            AppMonitorService.isOverlayVisible = false
                            AppMonitorService.currentMonitoredApp = null
                            goToHomeScreen()
                            dismissOverlay()
                            stopSelf()
                        }
                    )
                }
            }

            windowManager?.addView(overlayView, params)
            isOverlayShowing = true
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            Log.d(TAG, "✅ Overlay displayed successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing overlay", e)
            e.printStackTrace()
            isOverlayShowing = false
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

    private fun forceKillAndGoHome() {
        monitoredPackageName?.let { packageName ->
            try {
                // First, force stop the app using ActivityManager
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                
                // Remove from recent tasks (this also kills the app)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val appTasks = activityManager.appTasks
                    for (task in appTasks) {
                        try {
                            val taskInfo = task.taskInfo
                            if (taskInfo.baseActivity?.packageName == packageName ||
                                taskInfo.origActivity?.packageName == packageName) {
                                task.finishAndRemoveTask()
                                Log.d(TAG, "Removed $packageName from recents via appTasks")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error checking task", e)
                        }
                    }
                }
                
                // Also kill background processes as backup
                activityManager.killBackgroundProcesses(packageName)
                Log.d(TAG, "Killed background processes for: $packageName")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to force kill app: $packageName", e)
            }
        }
        
        // Go to home screen
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
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
    
    private fun dismissOverlay() {
        try {
            // Reset ALL flags when dismissing
            AppMonitorService.isOverlayVisible = false
            AppMonitorService.currentMonitoredApp = null
            isOverlayShowing = false
            lastDismissTime = System.currentTimeMillis()
            
            // Only change lifecycle state if it's valid
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            }
            
            if (overlayView != null) {
                try {
                    if (overlayView?.isAttachedToWindow == true) {
                        windowManager?.removeView(overlayView)
                        Log.d(TAG, "Overlay dismissed at ${lastDismissTime}")
                    }
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "View was not attached to window manager")
                }
            }
            overlayView = null
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing overlay", e)
        }
    }
    
    private fun saveIntentionLog(packageName: String, appName: String, reason: String, rating: Int) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@OverlayService)
                val log = IntentionLog(
                    packageName = packageName,
                    appName = appName,
                    reason = reason,
                    dumbnessRating = rating
                )
                db.intentionLogDao().insert(log)
                Log.d(TAG, "Saved intention log locally: $appName, rating=$rating")

                // Sync to Firebase if user is signed in and has Pro entitlement
                if (FirebaseManager.isUserSignedIn() && BillingManager.hasProEntitlement()) {
                    val saveResult = FirebaseManager.saveAppEntry(
                        appName = appName,
                        packageName = packageName,
                        reason = reason,
                        dumbReason = when (rating) {
                            1 -> "extremely_distracted"
                            2 -> "very_distracted"
                            3 -> "pretty_distracted"
                            4 -> "slightly_distracted"
                            5 -> "productive"
                            else -> "unknown"
                        },
                        sessionDuration = 0.0, // Session duration is not captured here directly
                        url = "app://$packageName"
                    )
                    saveResult.onSuccess {
                        Log.d(TAG, "Synced intention log to Firebase: $appName, rating=$rating")
                    }.onFailure { e ->
                        Log.e(TAG, "Failed to sync intention log to Firebase: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving intention log", e)
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "OverlayService destroyed")
        // Reset ALL flags
        AppMonitorService.isOverlayVisible = false
        AppMonitorService.currentMonitoredApp = null
        isOverlayShowing = false
        dismissOverlay()
        serviceScope.cancel()
        store.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

