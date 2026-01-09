# Overlay Glitch Fix - Complete

## Problem Summary

The intention prompt overlay was opening and closing repeatedly, causing a constant glitching effect. The logs showed this pattern:

```
11:19:51 - Overlay shown for com.popularapp.sevenmins
11:19:51 - Overlay dismissed at 1761889791895
11:19:51 - App changed: com.popularapp.sevenmins -> null
11:19:51 - App changed: null -> com.popularapp.sevenmins
11:19:51 - Showing overlay again
[Loop continues]
```

## Root Cause

The `OverlayService` had a background monitoring function (`startMonitoringForegroundApp()`) that continuously checked if the monitored app was still in the foreground. When the overlay appeared, it would detect that:

1. The overlay itself became the "foreground" (not the monitored app)
2. The service interpreted this as the user navigating away
3. It auto-dismissed the overlay
4. `AppMonitorService` detected the monitored app again
5. It showed the overlay again
6. **Loop repeated indefinitely**

This created a race condition where the overlay was constantly being shown and dismissed.

## Solution Implemented

**Removed all automatic dismissal logic from `OverlayService`:**

### Changes Made:

1. **Removed the monitoring function** (`startMonitoringForegroundApp()`)

    - This function was checking every 500ms if the monitored app was still foreground
    - Caused the glitching by detecting the overlay as a foreground change

2. **Removed getForegroundApp()** helper function

    - No longer needed without monitoring

3. **Removed unused imports and variables:**

    - `UsageStatsManager` import
    - `delay`, `Job`, `launch` coroutine imports
    - `monitorJob` variable
    - `CHECK_INTERVAL_MS` constant

4. **Updated onStartCommand:**
    - Removed call to `startMonitoringForegroundApp()`
    - Added comment explaining why we don't monitor

### New Behavior:

**The overlay now only dismisses when:**

-   ✅ User clicks "Continue" button (fills form and proceeds)
-   ✅ User clicks "X" close button (returns to home)

**The overlay will NOT auto-dismiss when:**

-   ❌ User switches to another app temporarily
-   ❌ Keyboard appears
-   ❌ Notifications appear
-   ❌ Any other foreground changes

## Why This Fix Works

1. **Overlay is now stable** - No background monitoring means no automatic dismissal
2. **User has full control** - Only user actions dismiss the overlay
3. **Debouncing still active** - The 2-second debounce prevents rapid recreation if user navigates quickly
4. **isOverlayVisible flag** - Prevents AppMonitorService from showing duplicate overlays

## Files Modified

-   `app/src/main/java/com/nibodhdaware/intentionality/service/OverlayService.kt`
    -   Removed monitoring functionality (51 lines removed)
    -   Simplified service to only handle display and user interaction

## Testing Recommendations

1. ✅ Open a monitored app → Overlay should appear
2. ✅ Fill out form and click Continue → App should launch
3. ✅ Click X button → Should return to home screen
4. ✅ Try switching apps rapidly → No glitching
5. ✅ Try opening keyboard while overlay is visible → Overlay stays
6. ✅ Check that overlay doesn't appear multiple times for same app opening

## Build Status

✅ **BUILD SUCCESSFUL in 35s**

-   39 actionable tasks: 11 executed, 28 up-to-date
-   No compilation errors
-   Only standard deprecation warnings (unrelated to changes)

## Summary

The glitching was caused by over-engineering. The overlay tried to be "smart" by auto-dismissing when the monitored app went away, but this created a feedback loop. The fix simplifies the logic: **show the overlay and let the user decide when to dismiss it**. This is more stable and gives users better control.
