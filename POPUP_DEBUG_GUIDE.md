# Popup Debug Guide

## 🐛 Issue: Popup Not Appearing

The popup isn't showing when a monitored app is opened. I've added extensive logging to help debug this.

---

## 🔍 Step-by-Step Debugging

### Step 1: Install Updated App

```bash
./gradlew installDebug
```

### Step 2: Open Logcat in Android Studio

1. Click **Logcat** tab at the bottom
2. Filter by tag: `AppMonitorService`
3. Keep it open while testing

### Step 3: Test the Flow

1. **Launch the app**
2. **Sign in** (or skip for now)
3. **Select an app to monitor** (e.g., Chrome, Instagram)
4. **Click "Start Monitoring"**
5. **Grant "Usage Access" permission** when prompted
6. **Return to the app**
7. **Open the monitored app** (e.g., Chrome)

### Step 4: Check Logcat Output

You should see logs like this:

```
D/AppMonitorService: ===== Service onCreate =====
D/AppMonitorService: Service started in foreground
D/AppMonitorService: ===== Service onStartCommand =====
D/AppMonitorService: Monitoring loop started, checking every 2 seconds
```

Then every 2 seconds:

```
D/AppMonitorService: Checking foreground app...
D/AppMonitorService: Current foreground app: com.android.chrome
D/AppMonitorService: Monitored apps: com.android.chrome
D/AppMonitorService: Last detected app: null
D/AppMonitorService: Recently prompted apps:
D/AppMonitorService: ✅ ALL CONDITIONS MET! Showing prompt for: com.android.chrome
D/AppMonitorService: ===== showPrompt called for: com.android.chrome =====
D/AppMonitorService: App name: Chrome
D/AppMonitorService: Starting IntentionPromptActivity...
D/AppMonitorService: ✅ Activity started successfully!
D/IntentionPromptActivity: ===== IntentionPromptActivity onCreate =====
D/IntentionPromptActivity: Received app_name: Chrome
D/IntentionPromptActivity: Received package_name: com.android.chrome
D/IntentionPromptActivity: Setting content...
D/IntentionPromptActivity: Composing IntentionalityTheme...
D/IntentionPromptActivity: ✅ Content set successfully!
D/IntentionPromptActivity: onStart called
D/IntentionPromptActivity: onResume called
D/IntentionPromptActivity: 🎨 IntentionPromptScreen is being composed!
```

**🎉 IF YOU SEE ALL THE ABOVE LOGS, THE POPUP IS WORKING!**

---

## ❌ Common Issues & Solutions

### Issue 1: Service Not Starting

**Symptoms:**

```
(No logs at all, or no "Service onCreate" message)
```

**Solution:**

1. Make sure you clicked "Start Monitoring"
2. Check if the notification "Intentionality Active" is showing
3. If not showing, restart the app and try again

### Issue 2: No Foreground App Detected

**Symptoms:**

```
D/AppMonitorService: ❌ No foreground app detected
```

**Solution:**

1. **Grant Usage Access Permission**:
    - Settings → Apps → Special app access → Usage access
    - Find "Intentionality"
    - Enable it
2. Restart monitoring after granting permission

### Issue 3: App Not in Monitored List

**Symptoms:**

```
D/AppMonitorService: Current foreground app: com.example.app
D/AppMonitorService: Monitored apps: com.android.chrome
D/AppMonitorService: ❌ App not in monitored list: com.example.app
```

**Solution:**

1. The app you're testing is not selected for monitoring
2. Go back to the app list
3. Check the box for the app you want to test
4. Try opening it again

### Issue 4: App in Cooldown Period

**Symptoms:**

```
D/AppMonitorService: ❌ App in cooldown period
```

**Solution:**

-   The popup was already shown for this app in the last 30 seconds
-   Wait 30 seconds and try again
-   Or test with a different monitored app

### Issue 5: Same as Last Detected App

**Symptoms:**

```
D/AppMonitorService: ❌ Same as last detected app (no change)
```

**Solution:**

-   The app is already open (you switched away and back)
-   The prompt only shows when you **first open** the app
-   To test again:
    1. Go back to home screen
    2. Wait 2-3 seconds
    3. Open the monitored app again

### Issue 6: Activity Started But Popup Doesn't Appear

**Symptoms:**

```
D/AppMonitorService: ✅ Activity started successfully!
(But NO logs from IntentionPromptActivity)
```

**Solution:**

The activity is being started, but it's either:

1. **Crashing silently** - Look for error logs:

```bash
adb logcat | grep -E "(IntentionPromptActivity|AndroidRuntime)"
```

2. **Getting blocked by the target app** - Some apps (especially games) might prevent other activities from appearing on top. Try:

    - Testing with a different monitored app
    - Going to Settings → Apps → Intentionality → Permissions and ensure "Display over other apps" is enabled

3. **Activity configuration issue** - Check if there are any `ActivityManager` errors:

```bash
adb logcat | grep "ActivityManager"
```

---

## 📋 Checklist Before Testing

Before opening a monitored app, verify:

-   [ ] ✅ App is installed and launched
-   [ ] ✅ Signed in (or skipped for dev mode)
-   [ ] ✅ At least one app is checked/selected
-   [ ] ✅ Clicked "Start Monitoring" button
-   [ ] ✅ Notification "Intentionality Active" is visible
-   [ ] ✅ Usage Access permission is granted
-   [ ] ✅ Logcat is open with `AppMonitorService` filter
-   [ ] ✅ The app you're testing is actually in the monitored list

---

## 🔬 Advanced Debugging

### Check Service Status

Run in terminal:

```bash
adb shell dumpsys activity services | grep -A 10 AppMonitorService
```

Should show the service is running.

### Check Monitored Apps Database

The app saves monitored apps to Room database. Check if they're actually saved:

Look for logs when you check/uncheck apps in the list:

```bash
adb logcat | grep "MonitoredApp"
```

### Check Usage Stats Permission

```bash
adb shell dumpsys usagestats | grep -A 5 "com.nibodhdaware.intentionality"
```

Should show the app has permission.

### Force Kill and Restart

If the service seems stuck:

```bash
adb shell am force-stop com.nibodhdaware.intentionality
```

Then relaunch the app and start monitoring again.

---

## 📊 What Each Log Means

### Service Logs

| Log Message                     | Meaning                              |
| ------------------------------- | ------------------------------------ |
| `===== Service onCreate =====`  | Service is initializing              |
| `Service started in foreground` | Service is running with notification |
| `Monitoring loop started`       | Background checking has begun        |

### Detection Logs

| Log Message                  | Meaning                              |
| ---------------------------- | ------------------------------------ |
| `Checking foreground app...` | Service is polling (every 2 seconds) |
| `Current foreground app: X`  | Detected which app is in foreground  |
| `Monitored apps: X, Y, Z`    | List of apps being monitored         |
| `✅ ALL CONDITIONS MET!`     | **POPUP SHOULD APPEAR!**             |

### Error Logs

| Log Message                     | Reason                                         |
| ------------------------------- | ---------------------------------------------- |
| `❌ No foreground app detected` | Can't detect foreground app (permission issue) |
| `❌ App not in monitored list`  | You opened an unmonitored app                  |
| `❌ Same as last detected app`  | Already showing for this app session           |
| `❌ App in cooldown period`     | Shown within last 30 seconds                   |
| `❌ Ignoring our own app`       | You're using Intentionality itself             |

---

## 🧪 Test Scenarios

### Test 1: First Time Opening

1. Start monitoring
2. Go to home screen
3. **Wait 3 seconds**
4. Open monitored app
5. **Expected**: Popup appears immediately

### Test 2: Switching Apps

1. Open monitored app A → Popup appears
2. Fill in and click "Proceed"
3. Go back to home
4. Wait 30+ seconds
5. Open monitored app A again
6. **Expected**: Popup appears again

### Test 3: Multiple Monitored Apps

1. Monitor apps: Chrome, Instagram
2. Open Chrome → Popup for Chrome
3. Go to home
4. Open Instagram → Popup for Instagram
5. **Expected**: Each app triggers its own popup

---

## 📤 What to Share

If the popup still doesn't work, share these logs:

### 1. Service Status

```
D/AppMonitorService: ===== Service onCreate =====
D/AppMonitorService: Service started in foreground
D/AppMonitorService: Monitoring loop started, checking every 2 seconds
```

### 2. Detection Logs (when opening monitored app)

```
D/AppMonitorService: Checking foreground app...
D/AppMonitorService: Current foreground app: ???
D/AppMonitorService: Monitored apps: ???
D/AppMonitorService: [Some ❌ or ✅ message]
```

### 3. Any Error Messages

```
E/AppMonitorService: ❌ Error ...
```

---

## 🎯 Quick Troubleshooting Commands

```bash
# Install the app
./gradlew installDebug

# View all logs
adb logcat | grep AppMonitorService

# Check if service is running
adb shell dumpsys activity services | grep AppMonitorService

# Check app processes
adb shell ps | grep intentionality

# Grant usage access manually (if permission dialog doesn't appear)
adb shell appops set com.nibodhdaware.intentionality GET_USAGE_STATS allow

# Clear app data and start fresh
adb shell pm clear com.nibodhdaware.intentionality
```

---

## ✅ Success Indicators

You'll know it's working when you see:

1. **✅ Notification**: "Intentionality Active" persists
2. **✅ Logs**: "Checking foreground app..." every 2 seconds
3. **✅ Detection**: "Current foreground app: [your app]"
4. **✅ Trigger**: "✅ ALL CONDITIONS MET!"
5. **✅ Popup**: Full-screen prompt appears
6. **✅ Data**: After "Proceed", entry saved to Supabase

---

## 🚀 Next Steps

1. Install the updated app with logging
2. Open Logcat with `AppMonitorService` filter
3. Follow the test flow
4. Share the relevant log output

The logs will tell us exactly where the problem is!
