# Overlay Persistence and Monitoring Fixes

## Problems Fixed

### 1. **Overlay Appearing Multiple Times**

**Problem:** The overlay was showing up repeatedly, even after being dismissed.

**Root Cause:**

-   No session tracking - app would show overlay every time it detected the monitored app
-   Poor state management between `AppMonitorService` and `OverlayService`

**Solution:**

-   Added `appsShownThisSession` set to track which apps have already shown overlay
-   Overlay only shows **once per session** for each monitored app
-   Better state synchronization with `currentMonitoredApp` tracking

### 2. **Overlay Persisting After App Closed**

**Problem:** Overlay would stay visible even after closing the monitored app.

**Root Cause:**

-   No detection of when monitored app goes to background
-   Overlay only dismissed on user interaction

**Solution:**

-   Added monitoring to detect when the monitored app is no longer foreground
-   Automatically dismiss overlay when monitored app closes/switches away
-   Check: `if (foregroundApp != currentMonitoredApp && foregroundApp != packageName)`

### 3. **Constant App Monitoring (Battery Drain)**

**Problem:** Service was checking foreground app every 500ms continuously.

**Root Cause:**

-   Polling too frequently
-   Checking even when no overlay was visible

**Solution:**

-   Reduced polling interval from 500ms to 1000ms (1 second)
-   Early return when overlay is visible - no need to check for new apps
-   Only log when app actually changes (reduces log spam)

## Key Changes

### AppMonitorService.kt

**New Variables:**

```kotlin
private val appsShownThisSession = mutableSetOf<String>()
var currentMonitoredApp: String? = null // Track which app is being monitored
private const val CHECK_INTERVAL_MS = 1000L // Reduced from 500ms
```

**Removed:**

-   `overlayCurrentlyShowing` (replaced with better state management)
-   `lastPromptTime` map (replaced with session tracking)
-   `PROMPT_COOLDOWN_MS` constant (no longer needed)

**New Logic:**

```kotlin
// If overlay is visible and monitored app is no longer foreground, dismiss it
if (isOverlayVisible && currentMonitoredApp != null) {
    if (foregroundApp != currentMonitoredApp && foregroundApp != packageName) {
        dismissCurrentOverlay()
    }
    return // Don't show new overlay while one is visible
}

// Only show once per session
if (!appsShownThisSession.contains(foregroundApp)) {
    appsShownThisSession.add(foregroundApp)
    showPrompt(foregroundApp)
}
```

**New Method:**

```kotlin
fun resetSession() {
    appsShownThisSession.clear()
    Log.d(TAG, "Session reset - all apps can show overlays again")
}
```

### OverlayService.kt

**Updated Callbacks:**

```kotlin
onProceed = { reason, rating ->
    // Reset flags FIRST before any other action
    AppMonitorService.isOverlayVisible = false
    AppMonitorService.currentMonitoredApp = null
    launchApp(packageName)
    dismissOverlay()
    stopSelf()
}

onGoBack = {
    // Reset flags FIRST before any other action
    AppMonitorService.isOverlayVisible = false
    AppMonitorService.currentMonitoredApp = null
    goToHomeScreen()
    dismissOverlay()
    stopSelf()
}
```

**Updated dismissOverlay():**

```kotlin
private fun dismissOverlay() {
    // Reset ALL flags when dismissing
    AppMonitorService.isOverlayVisible = false
    AppMonitorService.currentMonitoredApp = null
    isOverlayShowing = false
    // ... rest of dismissal logic
}
```

## Behavior Now

### ✅ Correct Behavior:

1. **First Time Opening Monitored App:**

    - ✅ Overlay shows up
    - ✅ User fills out form or dismisses
    - ✅ App launches or goes to home

2. **Opening Same App Again:**

    - ✅ NO overlay - already shown in this session
    - ✅ App opens normally

3. **Closing Monitored App While Overlay is Visible:**

    - ✅ Overlay automatically dismisses
    - ✅ Returns to previous screen

4. **Switching Apps While Overlay is Visible:**

    - ✅ Overlay automatically dismisses
    - ✅ Can continue to other apps

5. **Battery Usage:**
    - ✅ Reduced polling from 500ms to 1000ms
    - ✅ No unnecessary checks when overlay is visible
    - ✅ Minimal logging (only on app changes)

## Testing Checklist

-   [ ] Open monitored app → Overlay appears
-   [ ] Fill form and click Continue → App launches, overlay gone
-   [ ] Close monitored app → Try to open again → NO overlay (session tracking)
-   [ ] Press back while overlay is showing → Overlay dismisses, goes to home
-   [ ] Press home while overlay is showing → Overlay auto-dismisses
-   [ ] Switch to another app while overlay is showing → Overlay auto-dismisses
-   [ ] No repeated overlay appearances
-   [ ] No overlay stuck on screen after app is closed

## Reset Session (Future Enhancement)

If you want to allow overlays to show again without restarting the app, you can call:

```kotlin
// In MainActivity or wherever appropriate
val intent = Intent(this, AppMonitorService::class.java)
// Could add an action to trigger resetSession()
```

Or expose a button in settings:

```kotlin
Button(onClick = {
    // Reset the session so overlays can show again
    AppMonitorService.resetSession()
}) {
    Text("Reset Overlay Session")
}
```

## Log Messages to Look For

**Good:**

```
✅ Showing prompt for: com.popularapp.sevenmins (FIRST TIME THIS SESSION)
📱 Monitored app (com.popularapp.sevenmins) is no longer foreground. Dismissing overlay.
Overlay dismissed at [timestamp]
```

**Should NOT See:**

```
❌ Showing prompt for: <same app> (multiple times in short period)
❌ Overlay glitching/appearing repeatedly
❌ Constant "App changed" logs when nothing is happening
```

## Files Modified

1. `app/src/main/java/com/nibodhdaware/intentionality/service/AppMonitorService.kt`
2. `app/src/main/java/com/nibodhdaware/intentionality/service/OverlayService.kt`

## Next Steps

1. Install the updated APK
2. Test all scenarios in the checklist
3. Monitor logcat for any issues
4. Verify battery usage is reasonable
5. Consider adding a "Reset Session" button in settings if needed
