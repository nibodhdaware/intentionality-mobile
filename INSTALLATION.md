# Installation Guide - Intentionality App

## ✅ Build Status: SUCCESS!

Your APK has been successfully compiled and is ready to install!

**APK Location**: `app/build/outputs/apk/debug/app-debug.apk`
**APK Size**: 13 MB

## 📱 Installation Methods

### Method 1: Install via ADB (Recommended)

If you have a device connected or an emulator running:

```bash
cd /Users/nibodhdaware/AndroidStudioProjects/intentionality
./gradlew installDebug
```

This will automatically install the app on your connected device.

### Method 2: Manual Installation

1. Copy the APK to your Android device:

    ```bash
    adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/
    ```

2. On your Android device:
    - Open Files app or any file manager
    - Navigate to Internal Storage
    - Tap on `app-debug.apk`
    - Tap "Install" (you may need to enable "Install unknown apps" first)

### Method 3: Android Studio

1. Open the project in Android Studio
2. Connect your device or start an emulator
3. Click the green ▶️ Run button
4. Select your device
5. The app will be installed automatically

## 🔧 First-Time Setup

### Step 1: Launch the App

Tap the "Intentionality" icon on your device

### Step 2: Skip Login (Development Mode)

-   On the login screen, tap **"Skip for now (Dev Mode)"**
-   This allows testing without Google OAuth setup

### Step 3: Select Apps to Monitor

-   Browse through the list of installed apps
-   **Tap the checkbox** next to apps you want to monitor
-   Recommended apps for testing:
    -   Instagram
    -   Twitter/X
    -   YouTube
    -   Reddit
    -   TikTok

### Step 4: Grant Required Permissions

#### A. Start Monitoring

-   Tap the **blue Play button** (FAB) at the bottom-right

#### B. Grant Usage Access Permission

1. A dialog will appear: "Permissions Required"
2. Tap **"Open Settings"**
3. Find "Intentionality" in the list
4. Toggle the switch to **ON**
5. Press back to return to the app

#### C. Grant Display Over Other Apps

1. The settings screen will automatically open again
2. Find "Intentionality" in the list
3. Toggle "Allow display over other apps" to **ON**
4. Press back to return to the app

#### D. Start Monitoring Again

-   Tap the **blue Play button** again
-   The button should turn **red** and monitoring will begin
-   You'll see a notification: "Intentionality Active"

### Step 5: Test the App!

1. Press the **Home button** to exit Intentionality
2. Open one of the apps you selected (e.g., Instagram)
3. An overlay should appear: **"Why are you opening Instagram?"**
4. Type a reason (e.g., "Checking notifications")
5. Select intentionality level (e.g., "3 - Not intentional")
6. Tap **"Submit"**
7. The overlay closes and data is saved to Supabase!

## 🗄️ Verify Data in Supabase

1. Log into your Supabase dashboard
2. Go to **Table Editor**
3. Select the **`app_entries`** table
4. You should see your entry with:
    - App name
    - Your reason
    - Rating (1-5)
    - Timestamp
    - User ID

## 🎯 Using the App Daily

### Starting Monitoring

-   Open Intentionality app
-   Tap the **Play button** (blue FAB)
-   Keep the app running in background

### Stopping Monitoring

-   Open Intentionality app
-   Tap the **Stop button** (red X FAB)
-   Notification disappears

### Viewing Monitored Apps

-   Open Intentionality app
-   See checkboxes next to selected apps
-   Tap checkboxes to add/remove apps from monitoring

### When You Open a Monitored App

1. Overlay appears automatically
2. Fill in your reason
3. Select intentionality level (1-5)
4. Submit or Cancel
5. Continue using the app normally

## ⚠️ Troubleshooting

### "Overlay not showing"

**Problem**: Opening monitored apps doesn't show the prompt

**Solutions**:

1. Check "Display over other apps" permission is granted
2. Verify monitoring is active (red stop button, notification visible)
3. Try re-selecting the app in the app list
4. Restart monitoring (stop, then start again)
5. Restart your device

### "No apps in the list"

**Problem**: App list is empty or loading

**Solutions**:

1. Wait a few seconds for apps to load
2. Check "Query all packages" permission in manifest
3. Restart the app

### "Can't grant Usage Access"

**Problem**: Permission dialog doesn't work

**Solutions**:

1. Go to Settings manually:
    - Settings → Apps → Special Access → Usage Access
    - Find "Intentionality" and enable it
2. Restart your device
3. Try on a different Android version (API 26+)

### "Data not in Supabase"

**Problem**: Submissions not appearing in database

**Solutions**:

1. Check internet connection
2. Verify Supabase setup script was run
3. Check Supabase logs for errors
4. Verify RLS policies allow inserts
5. Check the user_id (should be "dev-user-\*" in dev mode)

### "App crashes on launch"

**Problem**: App force closes immediately

**Solutions**:

1. Check Android version (must be 8.0+, API 26+)
2. Clear app data: Settings → Apps → Intentionality → Clear Data
3. Uninstall and reinstall
4. Check logcat for error messages:
    ```bash
    adb logcat | grep Intentionality
    ```

### "Service stops after a while"

**Problem**: Monitoring stops automatically

**Solutions**:

1. Disable battery optimization for Intentionality:
    - Settings → Apps → Intentionality → Battery → Unrestricted
2. Lock the app in recent apps (prevents Android from killing it)
3. Grant "Autostart" permission if available (some manufacturers)

## 🔍 Debugging

### View Logs

```bash
adb logcat | grep -E "Intentionality|AppMonitorService|OverlayWindow"
```

### Check Service Status

```bash
adb shell dumpsys activity services | grep Intentionality
```

### Check Permissions

```bash
adb shell dumpsys package com.nibodhdaware.intentionality | grep permission
```

### Force Stop

```bash
adb shell am force-stop com.nibodhdaware.intentionality
```

### Uninstall

```bash
adb uninstall com.nibodhdaware.intentionality
```

### Reinstall

```bash
./gradlew uninstallAll installDebug
```

## 📊 Next Steps

### 1. Set Up Google OAuth (Optional)

-   Configure in Supabase Dashboard → Authentication → Providers
-   Update deep linking in AndroidManifest.xml
-   Remove "Skip for now" button

### 2. Analyze Your Data

Use these SQL queries in Supabase:

**Daily Summary**:

```sql
SELECT * FROM get_daily_summary('your-user-id', CURRENT_DATE);
```

**Most Mindless Apps**:

```sql
SELECT app_name, AVG(rating) as avg_rating, COUNT(*) as times_opened
FROM app_entries
WHERE user_id = 'your-user-id'
GROUP BY app_name
ORDER BY avg_rating DESC;
```

**Usage Over Time**:

```sql
SELECT DATE(timestamp) as date, COUNT(*) as entries
FROM app_entries
WHERE user_id = 'your-user-id'
GROUP BY DATE(timestamp)
ORDER BY date DESC;
```

### 3. Customize the App

-   Adjust cooldown period in `AppMonitorService.kt` (PROMPT_COOLDOWN_MS)
-   Modify theme colors in `Color.kt`
-   Change overlay design in `overlay_prompt.xml`

### 4. Build for Production

```bash
# Create signed APK for Play Store
./gradlew assembleRelease

# Or build AAB (recommended)
./gradlew bundleRelease
```

## 📱 Tested Devices

The app has been designed and tested for:

-   ✅ Android 13 (API 33)
-   ✅ Android 12 (API 31)
-   ✅ Android 10 (API 29)
-   ✅ Android 8.0 (API 26) - Minimum

## 🎉 Enjoy Your Mindful Journey!

You're all set! The app is now monitoring your selected apps and helping you be more intentional with your phone usage.

---

**Need more help?** See the full documentation in `README.md`
