# Scheduling Feature Implementation - Complete ✅

## Overview

Successfully implemented time-based recurring overlay feature with per-app scheduling capabilities.

## What Was Implemented

### 1. Database Schema Update (v2 → v3)

**File:** `app/src/main/java/com/nibodhdaware/intentionality/database/MonitoredApp.kt`

-   Added 6 new columns to `MonitoredApp` entity:
    -   `startHour: Int` - Start time hour (0-23)
    -   `startMinute: Int` - Start time minute (0-59)
    -   `endHour: Int` - End time hour (0-23)
    -   `endMinute: Int` - End time minute (0-59)
    -   `allDay: Boolean` - Whether to work all day (ignores time window if true)
    -   `intervalMinutes: Int` - How often to show overlay (1-60 minutes)

**File:** `app/src/main/java/com/nibodhdaware/intentionality/database/AppDatabase.kt`

-   Created MIGRATION_2_3 to safely upgrade existing databases
-   All existing data preserved with sensible defaults:
    -   `allDay = true` (works 24/7)
    -   `startHour = 0, startMinute = 0`
    -   `endHour = 23, endMinute = 59`
    -   `intervalMinutes = 5` (default 5 minutes)

### 2. Service Layer - Interval-Based Overlay Display

**File:** `app/src/main/java/com/nibodhdaware/intentionality/service/AppMonitorService.kt`

**Major Changes:**

-   **Removed:** Session-based tracking (`appsShownThisSession` set)
-   **Added:** Interval tracking (`lastOverlayTime` map to track last overlay per app)
-   **Changed:** `cachedMonitoredApps` from `List<String>` to `Map<String, MonitoredApp>` to access full app data

**New Logic:**

```kotlin
// Check if within time window
if (isWithinActiveTime(monitoredApp)) {
    // Check if interval has elapsed
    val intervalMs = monitoredApp.intervalMinutes * 60 * 1000L
    if (timeSinceLastOverlay >= intervalMs) {
        showPrompt(foregroundApp)
        lastOverlayTime[foregroundApp] = currentTime
    }
}
```

**New Method:** `isWithinActiveTime()`

-   Validates current time against app's configured window
-   Handles same-day windows (9 AM - 5 PM)
-   Handles overnight windows (11 PM - 2 AM)
-   Respects `allDay` setting

### 3. Configuration UI - AppConfigScreen

**File:** `app/src/main/java/com/nibodhdaware/intentionality/ui/appconfig/AppConfigScreen.kt` (NEW - 378 lines)

**Features:**

-   **Time Window Configuration:**
    -   "All Day" toggle switch
    -   Start time picker (hour:minute)
    -   End time picker (hour:minute)
-   **Interval Configuration:**
    -   Slider: 1-60 minutes
    -   Quick presets: 5, 10, 15, 30, 60 minutes
-   **Summary Card:**
    -   Shows current configuration before saving
    -   Example: "Works: 09:00 - 10:00" or "Works: All day"
    -   Example: "Shows overlay every 10 minutes"
-   **Save Functionality:**
    -   Updates database via ViewModel
    -   Navigates back automatically
    -   Changes take effect immediately

### 4. UI Updates - HomeScreen

**File:** `app/src/main/java/com/nibodhdaware/intentionality/ui/home/HomeScreen.kt`

**MonitoredAppItem Component:**

-   Added `onConfigure` callback parameter
-   Displays schedule information:
    ```
    App Name
    Works: 09:00 - 10:00  (or "Works: All day")
    Every 10 min
    ```
-   Added Settings icon button to configure individual apps
-   Changed layout from Row to Column to accommodate schedule info

### 5. Navigation Integration

**File:** `app/src/main/java/com/nibodhdaware/intentionality/ui/main/MainScreen.kt`

**Added:**

-   AppConfigScreen route: `app_config/{packageName}`
-   Navigation parameter: `onNavigateToAppConfig`
-   Proper back navigation

**Usage Flow:**

1. User sees monitored app with schedule info
2. Clicks Settings icon
3. Opens AppConfigScreen for that app
4. Configures time window and interval
5. Saves → returns to HomeScreen
6. Changes take effect immediately

### 6. Data Access Layer

**Files:**

-   `app/src/main/java/com/nibodhdaware/intentionality/database/MonitoredAppDao.kt`
-   `app/src/main/java/com/nibodhdaware/intentionality/database/MonitoredAppRepository.kt`
-   `app/src/main/java/com/nibodhdaware/intentionality/ui/applist/AppListViewModel.kt`

**Added Methods:**

```kotlin
// DAO
@Update
suspend fun update(monitoredApp: MonitoredApp)

@Query("SELECT * FROM monitored_apps WHERE packageName = :packageName LIMIT 1")
suspend fun getByPackageName(packageName: String): MonitoredApp?

// Repository
suspend fun update(monitoredApp: MonitoredApp)
suspend fun getByPackageName(packageName: String): MonitoredApp?

// ViewModel
suspend fun getMonitoredApp(packageName: String): MonitoredApp?
suspend fun updateMonitoredApp(app: MonitoredApp)
```

## How It Works

### User Flow

1. **Select Apps to Monitor** (existing)

    - User selects apps from AppListScreen
    - Apps added with default settings (all day, every 5 minutes)

2. **Configure Schedule** (NEW)

    - From HomeScreen, click Settings icon on any monitored app
    - Opens AppConfigScreen
    - Set time window (or use All Day)
    - Set interval (1-60 minutes, or use quick presets)
    - Save configuration

3. **Overlay Display** (NEW BEHAVIOR)
    - Service monitors foreground app every 1 second
    - When monitored app is in foreground:
        - Check if current time is within configured window
        - Check if enough time has passed since last overlay
        - If both conditions met, show overlay
        - Track time of display for next interval

### Examples

**Example 1: Workout App (Morning Only)**

-   App: 7 Minute Workout
-   Time Window: 09:00 - 10:00
-   Interval: Every 10 minutes
-   Result: Overlay appears at 9:00, 9:10, 9:20, 9:30, 9:40, 9:50 when app is open

**Example 2: Social Media (All Day)**

-   App: Instagram
-   Time Window: All day
-   Interval: Every 5 minutes
-   Result: Overlay appears every 5 minutes whenever Instagram is open, 24/7

**Example 3: Work App (Business Hours)**

-   App: Slack
-   Time Window: 09:00 - 17:00
-   Interval: Every 15 minutes
-   Result: Overlay appears every 15 minutes during work hours only

## Technical Details

### Database Migration

-   Automatic migration on app upgrade
-   Existing monitored apps get default values
-   No data loss
-   Handles schema version 2 → 3

### Time Window Validation

```kotlin
private fun isWithinActiveTime(app: MonitoredApp): Boolean {
    if (app.allDay) return true

    val currentTimeInMinutes = currentHour * 60 + currentMinute
    val startTimeInMinutes = app.startHour * 60 + app.startMinute
    val endTimeInMinutes = app.endHour * 60 + app.endMinute

    // Handle same-day: 9:00 - 17:00
    // Handle overnight: 23:00 - 02:00
    return if (startTimeInMinutes <= endTimeInMinutes) {
        currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
    } else {
        currentTimeInMinutes >= startTimeInMinutes ||
        currentTimeInMinutes <= endTimeInMinutes
    }
}
```

### Interval Tracking

```kotlin
// Track last overlay time per app
private val lastOverlayTime = mutableMapOf<String, Long>()

// Check interval
val lastTime = lastOverlayTime[appPackage] ?: 0L
val intervalMs = app.intervalMinutes * 60 * 1000L
val timeSinceLastOverlay = System.currentTimeMillis() - lastTime

if (timeSinceLastOverlay >= intervalMs) {
    showPrompt(appPackage)
    lastOverlayTime[appPackage] = System.currentTimeMillis()
}
```

## Build Status

✅ **Build Successful**

```
BUILD SUCCESSFUL in 57s
39 actionable tasks: 8 executed, 31 up-to-date
```

Only deprecation warnings (not errors):

-   Icons.Filled.ArrowBack → use AutoMirrored version (cosmetic)
-   Divider → renamed to HorizontalDivider (cosmetic)

## What Users Will See

### Monitored Apps List

Before:

```
Instagram ✓
```

After:

```
Instagram                    ⚙️  ✓
Works: All day
Every 5 min
```

### Configuration Screen

-   App name and package
-   "All Day" toggle
-   Time pickers (if not all day)
-   Interval slider with value display
-   Quick preset buttons (5, 10, 15, 30, 60 min)
-   Summary card showing configuration
-   Save button

### Overlay Behavior

Before: Showed once per app session
After: Shows every X minutes during configured time window

## Files Modified

### Created

1. `app/src/main/java/com/nibodhdaware/intentionality/ui/appconfig/AppConfigScreen.kt` (NEW)

### Modified

1. `app/src/main/java/com/nibodhdaware/intentionality/database/MonitoredApp.kt`
2. `app/src/main/java/com/nibodhdaware/intentionality/database/AppDatabase.kt`
3. `app/src/main/java/com/nibodhdaware/intentionality/database/MonitoredAppDao.kt`
4. `app/src/main/java/com/nibodhdaware/intentionality/database/MonitoredAppRepository.kt`
5. `app/src/main/java/com/nibodhdaware/intentionality/service/AppMonitorService.kt`
6. `app/src/main/java/com/nibodhdaware/intentionality/ui/home/HomeScreen.kt`
7. `app/src/main/java/com/nibodhdaware/intentionality/ui/main/MainScreen.kt`
8. `app/src/main/java/com/nibodhdaware/intentionality/ui/applist/AppListViewModel.kt`

## Testing Recommendations

### Manual Testing

1. **Database Migration:**

    - Install current version (v2)
    - Add some monitored apps
    - Install new version (v3)
    - Verify apps still listed with default schedule

2. **Time Window Testing:**

    - Configure app with specific time window (e.g., 14:00 - 15:00)
    - Open app before window → no overlay
    - Open app during window → overlay appears
    - Open app after window → no overlay

3. **Interval Testing:**

    - Set 5-minute interval
    - Open monitored app
    - First overlay appears immediately
    - Wait 5 minutes → overlay appears again
    - Verify timing accuracy

4. **All Day Toggle:**

    - Enable "All Day"
    - Time pickers should be disabled
    - Overlay should work 24/7

5. **Overnight Window:**

    - Set window: 23:00 - 02:00
    - Test at 22:30 → no overlay
    - Test at 23:30 → overlay works
    - Test at 01:00 → overlay works
    - Test at 02:30 → no overlay

6. **Multiple Apps:**
    - Configure different schedules for different apps
    - Verify each app respects its own schedule
    - Switch between apps rapidly

### Edge Cases to Test

-   [ ] System time changes
-   [ ] Timezone changes
-   [ ] App uninstalled while monitoring
-   [ ] Service restart (preserves interval tracking)
-   [ ] Invalid time ranges
-   [ ] Same start and end time

## Future Enhancements (Optional)

1. **Days of Week:**

    - Add weekday selection (M, T, W, Th, F, Sa, Su)
    - Different schedules for weekdays vs weekends

2. **Multiple Time Windows:**

    - Morning: 9-10 AM
    - Evening: 6-7 PM

3. **Smart Scheduling:**

    - Based on usage patterns
    - ML-driven suggestions

4. **Pause/Resume:**

    - Temporary pause for specific apps
    - One-time skip button in overlay

5. **Statistics:**
    - How many overlays shown today
    - Adherence to time windows
    - Productivity score adjustments

## Notes

-   All data layer changes complete ✅
-   All UI components created ✅
-   Navigation integrated ✅
-   Build successful ✅
-   Ready for testing ✅

The feature is **fully implemented and ready to use**. Users can now configure individual apps with custom time windows and intervals, and the overlay will respect these settings.
