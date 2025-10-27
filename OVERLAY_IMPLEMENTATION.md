# System Overlay Implementation Guide

## Overview

Successfully implemented a **system overlay** using `SYSTEM_ALERT_WINDOW` permission that displays a full-screen popup when monitored apps are opened.

## What's New

### 1. System Overlay Window

-   Replaced the Activity-based approach with a true system overlay
-   The overlay appears on top of all apps, not as a separate activity
-   More reliable detection and display when opening monitored apps

### 2. Updated UI

-   New intentionality rating system (1-5):
    -   **1 - Very intentional**
    -   **2 - Somewhat intentional**
    -   **3 - Not intentional**
    -   **4 - Mindless**
    -   **5 - Regretful**
-   Modern card-based overlay design with semi-transparent background
-   Submit button (instead of "Proceed")
-   Go Back button returns to home screen

### 3. New Components

#### OverlayService

-   **Location**: `app/src/main/java/com/nibodhdaware/intentionality/service/OverlayService.kt`
-   Manages the system overlay window
-   Uses `WindowManager` to display the overlay
-   Handles launching the monitored app after submission

#### IntentionOverlayView

-   **Location**: `app/src/main/java/com/nibodhdaware/intentionality/ui/prompt/IntentionOverlayView.kt`
-   Jetpack Compose UI for the overlay
-   Displays the prompt, text input, dropdown, and buttons
-   Saves data to Supabase before launching the app

### 4. Updated Components

#### AppMonitorService

-   Now starts `OverlayService` instead of `IntentionPromptActivity`
-   Checks for `SYSTEM_ALERT_WINDOW` permission before showing overlay
-   Logs permission status for debugging

#### AppListScreen

-   Updated permission checking to include overlay permission
-   New permission dialog explaining both required permissions
-   Automatically guides users to the correct settings screen

### 5. Permissions

Added `SYSTEM_ALERT_WINDOW` permission in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

## How to Test

### Step 1: Grant Permissions

1. Open the app
2. Navigate to the App Selection screen
3. Select one or more apps to monitor
4. Click "Start Monitoring"
5. If prompted, grant the following permissions:
    - **Usage Access**: Settings → Apps → Special app access → Usage access
    - **Display over other apps**: Settings → Apps → Special app access → Display over other apps

### Step 2: Test the Overlay

1. After granting permissions, start monitoring
2. Press the home button or switch to another screen
3. Open one of the monitored apps
4. The overlay should appear immediately with:
    - "Why are you opening [App Name]?"
    - Text input for your reason
    - Dropdown with intentionality ratings (1-5)
    - Submit button
    - Go Back button

### Step 3: Verify Data Storage

1. Fill in the reason and select a rating
2. Click "Submit"
3. The monitored app should launch
4. Data should be saved to Supabase with:
    - `app_name`
    - `package_name`
    - `reason` (text)
    - `rating` (1-5)
    - `timestamp`
    - `user_id`

## Troubleshooting

### Overlay Doesn't Appear

1. Check if both permissions are granted:
    - Settings → Apps → Intentionality → Permissions
    - Verify "Display over other apps" is enabled
2. Check logcat for errors:
    ```bash
    adb logcat | grep -E "AppMonitorService|OverlayService"
    ```
3. Look for these log messages:
    - ✅ "OverlayService started successfully!"
    - ✅ "Overlay shown successfully!"
    - ❌ "SYSTEM_ALERT_WINDOW permission not granted!"

### Overlay Appears But Doesn't Accept Input

-   This is a known Android limitation on some devices
-   Try adjusting the `WindowManager.LayoutParams` flags in `OverlayService.kt`
-   Current flags include `FLAG_NOT_FOCUSABLE` removed to allow input

### App Doesn't Launch After Submit

1. Check if the app has a launch intent
2. Verify the package name is correct
3. Check logcat for launch errors

## Technical Details

### Window Type

-   Android 8.0+: `TYPE_APPLICATION_OVERLAY`
-   Android 7.1 and below: `TYPE_PHONE` (deprecated)

### Window Flags

-   Focusable: Yes (allows input)
-   Not touch modal: Yes (touches outside the overlay are allowed)
-   Layout in screen: Yes (full screen)
-   Layout no limits: Yes (can extend beyond screen bounds if needed)

### Data Flow

1. `AppMonitorService` detects monitored app launch
2. Starts `OverlayService` with app name and package
3. `OverlayService` creates and displays `IntentionOverlayView`
4. User fills form and clicks Submit
5. `IntentionOverlayView` saves data to Supabase
6. `OverlayService` launches the monitored app
7. Overlay is removed and service stops

## Files Modified/Created

### New Files

-   `app/src/main/java/com/nibodhdaware/intentionality/service/OverlayService.kt`
-   `app/src/main/java/com/nibodhdaware/intentionality/ui/prompt/IntentionOverlayView.kt`

### Modified Files

-   `app/src/main/AndroidManifest.xml` (added permission and service)
-   `app/src/main/java/com/nibodhdaware/intentionality/service/AppMonitorService.kt` (updated to use overlay)
-   `app/src/main/java/com/nibodhdaware/intentionality/ui/applist/AppListScreen.kt` (added overlay permission check)

## Build Status

✅ Project builds successfully with no errors

## Next Steps

1. Install the app on a test device
2. Grant both permissions
3. Test with various apps
4. Verify data is being saved to Supabase
5. Monitor logs for any issues

