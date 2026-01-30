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
import com.nibodhdaware.intentionality.api.ApiManager // Added import
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var monitoredPackageName: String? = null
    
    // Debouncing and state management
    private var isOverlayShowing = false

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
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Log.d(TAG, "OverlayService created")
    }

    private fun requestFocusAndPauseAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { }
                .build()
            
            audioFocusRequest?.let {
                audioManager?.requestAudioFocus(it)
                Log.d(TAG, "Audio focus requested - background audio should pause")
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
        Log.d(TAG, "Audio focus abandoned")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "===== OverlayService onStartCommand =====")

        val appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: "Unknown App"
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val customIntention = intent?.getStringExtra(EXTRA_CUSTOM_INTENTION) ?: ""

        // Prevent showing multiple overlays if already showing
        if (isOverlayShowing) return START_NOT_STICKY

        monitoredPackageName = packageName
        
        requestFocusAndPauseAudio()
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
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.OPAQUE
            )

            params.gravity = Gravity.CENTER
            // Allow the window to receive input focus so the keyboard works
            params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

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
                            // Mark as proceeded - this starts the interval timer
                            AppMonitorService.recordProceed(packageName)
                            launchApp(packageName)
                            dismissOverlay()
                            stopSelf()
                        },
                        onGoBack = {
                            Log.d(TAG, "Go back pressed")
                            // Reset flags via centralized method
                            AppMonitorService.recordDismissal(packageName)
                            forceKillAndGoHome()
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
                try {
                    activityManager.killBackgroundProcesses(packageName)
                    Log.d(TAG, "Killed background processes for: $packageName")
                } catch (se: SecurityException) {
                    Log.w(TAG, "SecurityException: Cannot kill background processes for $packageName. This is common on newer Android versions.")
                }
                
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
            } catch (se: SecurityException) {
                Log.w(TAG, "SecurityException: Cannot kill app $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to kill app: $packageName", e)
            }
        }
    }
    
    private fun dismissOverlay() {
        try {
            abandonAudioFocus()
            // Reset ALL flags when dismissing
            AppMonitorService.recordDismissal(monitoredPackageName)
            isOverlayShowing = false
            
            // Only change lifecycle state if it's valid
            if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            }
            
            if (overlayView != null) {
                try {
                    if (overlayView?.isAttachedToWindow == true) {
                        windowManager?.removeView(overlayView)
                        Log.d(TAG, "Overlay dismissed")
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
                if (ApiManager.isUserSignedIn() && BillingManager.hasProEntitlement()) {
                    val saveResult = ApiManager.saveAppEntry(
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
        abandonAudioFocus()
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

