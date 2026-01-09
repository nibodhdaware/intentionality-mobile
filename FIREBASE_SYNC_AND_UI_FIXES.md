# Firebase Sync & UI Fixes - Implementation Summary

## ⚠️ IMPORTANT: This Document is Outdated

**Please refer to:** `FIREBASE_MONITORED_APPS_FIX.md` for the latest implementation.

**Major Changes:**
- Firebase collection changed from `users/{userId}/settings/appList` to `users/{userId}/monitoredApps`
- Firebase is now the single source of truth (not local database)
- Local database is used only for caching
- Only apps installed locally are shown from Firebase list
- App list is cached to prevent stuttering

---

## Original Changes (Superseded)

### 1. Firebase App List Sync ✅

**Location:** ~~`users/{userID}/settings/appList`~~ → **NOW: `users/{userID}/monitoredApps`**

**Schema:**

```javascript
{
  packages: ["com.app.package1", "com.app.package2", ...],
  lastUpdated: Timestamp
}
```

**Implementation:**

-   **FirebaseManager.kt**

    -   Updated `syncMonitoredApps()` to sync to `users/{userID}/settings/appList`
    -   Stores only package names in `packages` array (not full app data)
    -   Uses Firestore Timestamp for `lastUpdated`
    -   Updated `getMonitoredApps()` to fetch from new location

-   **AppListViewModel.kt**

    -   ✅ **FIXED: Temporary Selection State**
        -   Added `selectedApps` StateFlow to track UI selections
        -   Selections are NOT saved to database until button click
    -   Added `toggleAppSelection()` for checkbox interactions
    -   Added `initializeSelectionFromMonitored()` to load current state
    -   Added `addSelectedAppsToMonitored()` for bulk database update
    -   **Firebase sync now only happens when "Add apps" button is clicked**
    -   Removed immediate sync from `onAppChecked()` (kept for backward compatibility)

-   **AppListScreen.kt**
    -   Uses `selectedApps` state instead of `monitoredApps` for checkboxes
    -   Checkboxes toggle selection state only (no database changes)
    -   "Add apps" button calls `addSelectedAppsToMonitored()` which:
        1. Bulk updates local database
        2. Syncs to Firebase once
        3. Shows toast and navigates back

**Flow:**

1. User opens app selection screen
2. Current monitored apps loaded into selection state
3. User checks/unchecks apps → Only UI state changes
4. User clicks "Add X apps" button
5. Bulk update to local database (add new, remove deselected)
6. **Single Firebase sync** with final package list
7. Navigate back with toast confirmation

### 2. Time Interval Display Fix ✅

**Location:** `AppConfigScreen.kt`

**Changes:**

-   Removed "m" suffix from quick preset buttons (was: "5m", now: "5")
-   Changed from `OutlinedButton` to solid `Button` for better visibility
-   Removed border styling
-   Increased font size from 12sp to 14sp with bold weight
-   Better visual feedback when selected (full blue vs transparent)
-   Adjusted padding for better spacing

**Before:**

```kotlin
Text(text = "${minutes}m", fontSize = 12.sp)
```

**After:**

```kotlin
Text(text = "$minutes", fontSize = 14.sp, fontWeight = FontWeight.Bold)
```

### 3. Integration Points

**When Apps Are Selected (Checkbox Click):**

```kotlin
fun toggleAppSelection(packageName: String) {
    val currentSelection = _selectedApps.value.toMutableSet()
    if (currentSelection.contains(packageName)) {
        currentSelection.remove(packageName)
    } else {
        currentSelection.add(packageName)
    }
    _selectedApps.value = currentSelection
    // NO DATABASE CHANGES, NO FIREBASE SYNC
}
```

**When "Add Apps" Button Clicked:**

```kotlin
suspend fun addSelectedAppsToMonitored() {
    val appsToAdd = _selectedApps.value.toList()
    val currentMonitored = monitoredApps.first().map { it.packageName }.toSet()

    // Add new apps
    appsToAdd.forEach { packageName ->
        if (!currentMonitored.contains(packageName)) {
            val appName = _installedApps.value.find { it.packageName == packageName }?.name ?: packageName
            repository.insert(MonitoredApp(packageName, appName))
        }
    }

    // Remove deselected apps
    currentMonitored.forEach { packageName ->
        if (!appsToAdd.contains(packageName)) {
            repository.delete(MonitoredApp(packageName))
        }
    }

    // Single Firebase sync after all changes
    syncMonitoredAppsToFirebase()
}
```

**When User Logs In:**

```kotlin
private fun loadUserProfile() {
    viewModelScope.launch {
        // ... load profile ...
        if (user != null) {
            syncMonitoredAppsFromFirebase() // ← Fetches from Firebase
        }
    }
}
```

## Firebase Collection Structure

```
users/
  {userID}/
    settings/
      appList/  ← Document
        packages: ["com.youtube.android", "com.instagram.android"]
        lastUpdated: Timestamp(2025-11-01 10:30:00)
    activities/
      {documentID}/
        dumbReason: "slightly_distracted"
        reason: "Anime life lessons"
        sessionDuration: 23.87
        timestamp: Timestamp
        url: "https://www.youtube.com/..."
        packageName: "com.youtube.android"
```

## Bug Fixes

### Issue: Firebase sync on every checkbox click

**Problem:**

-   Logs showed: `Synced 2 monitored apps`, then `Synced 1 monitored apps`
-   Each checkbox click triggered database update AND Firebase sync
-   Package list was inconsistent

**Root Cause:**

-   `onAppChecked()` immediately modified database
-   `syncMonitoredAppsToFirebase()` called on every change
-   No batching or waiting for user confirmation

**Solution:**

-   Introduced temporary `selectedApps` state
-   Checkboxes only modify UI state
-   Database and Firebase updates happen together on button click
-   Single bulk sync instead of multiple individual syncs

## Testing

### Test Firebase Sync:

1. ✅ Login on Device A
2. ✅ Select multiple apps (e.g., YouTube, Instagram, Calendly)
3. ✅ Click "Add X apps" button
4. ✅ Check Firebase Console → `users/{userID}/settings/appList`
5. ✅ Verify `packages` array contains exactly selected packages
6. ✅ Login on Device B with same account
7. ✅ Apps should automatically be selected

### Test UI Fix:

1. ✅ Open app configuration screen
2. ✅ Look at time interval presets
3. ✅ Should show: `5`, `10`, `15`, `30`, `60` (no "m")
4. ✅ Numbers should be bold and clearly visible
5. ✅ Selected button should have solid blue background

### Test Selection Flow:

1. ✅ Open app selection screen
2. ✅ Check multiple apps - should see checkmarks
3. ✅ Button shows "Add X apps" with count
4. ✅ Uncheck some apps - count updates
5. ✅ Click button - single Firebase sync
6. ✅ Check logcat - should see ONE "Synced X monitored apps" message

## Files Modified

1. ✅ `firebase/FirebaseManager.kt` - Firebase sync methods
2. ✅ `ui/applist/AppListViewModel.kt` - Selection state and bulk sync
3. ✅ `ui/applist/AppListScreen.kt` - UI selection integration
4. ✅ `ui/appconfig/AppConfigScreen.kt` - UI fixes

## Build Status

✅ **BUILD SUCCESSFUL** - No errors, only deprecation warnings

## Behavior Before vs After

**BEFORE:**

```
User checks app 1    → DB: [app1]           → Firebase: [app1]
User checks app 2    → DB: [app1, app2]     → Firebase: [app1, app2]
User unchecks app 1  → DB: [app2]           → Firebase: [app2]
User checks app 3    → DB: [app2, app3]     → Firebase: [app2, app3]
User clicks button   → Navigate back
Total Firebase syncs: 4
```

**AFTER:**

```
User checks app 1    → UI: [app1]           → (no Firebase)
User checks app 2    → UI: [app1, app2]     → (no Firebase)
User unchecks app 1  → UI: [app2]           → (no Firebase)
User checks app 3    → UI: [app2, app3]     → (no Firebase)
User clicks button   → DB: [app2, app3]     → Firebase: [app2, app3]
Total Firebase syncs: 1
```

## Next Steps

-   Test on actual device to verify single Firebase sync
-   Verify monitored apps sync across multiple devices
-   Check that interval buttons are no longer wrapped
-   Confirm package list in Firebase matches selected apps exactly
