# Quick Start Guide - Intentionality App

## 🚀 Getting Started in 5 Minutes

### Step 1: Setup Supabase Database (2 minutes)

1. Open your Supabase project dashboard
2. Go to the SQL Editor
3. Copy and paste the contents of `supabase_setup.sql`
4. Click "Run" to execute the SQL script
5. Verify you see "Setup completed successfully!"

### Step 2: Build the App (1 minute)

Open terminal in the project directory and run:

```bash
./gradlew assembleDebug
```

Or in Android Studio, click the green ▶️ Run button.

### Step 3: Install & Launch (1 minute)

The app will install on your connected Android device or emulator automatically.

### Step 4: Grant Permissions (1 minute)

1. **Skip Login**: On the login screen, tap "Skip for now (Dev Mode)"
2. **Select Apps**: Choose 2-3 apps to monitor (e.g., Instagram, Twitter, YouTube)
3. **Tap the Play Button** (blue FAB at bottom-right)
4. **Grant Permissions** when prompted:
    - Tap "Open Settings"
    - Enable "Usage Access" for Intentionality
    - Go back, tap Play button again
    - Enable "Display over other apps"

### Step 5: Test It! (30 seconds)

1. Press the home button to exit Intentionality
2. Open one of the apps you selected to monitor
3. You should see an overlay prompt: "Why are you opening [App Name]?"
4. Type a reason, select intentionality level, tap Submit
5. Check your Supabase dashboard → Table Editor → app_entries to see the saved data!

## ✅ Success Indicators

-   🔵 Blue notification: "Intentionality Active - Monitoring your app usage"
-   🎯 Overlay appears when opening monitored apps
-   📊 Data appears in Supabase `app_entries` table

## ⚠️ Troubleshooting

**Overlay not showing?**

-   Ensure both permissions are granted (Usage Access + Display over other apps)
-   Try selecting the app again in the list
-   Restart monitoring by tapping stop then play

**Can't grant Usage Access?**

-   Go to Android Settings → Apps → Special Access → Usage Access
-   Find "Intentionality" and enable it manually

**Nothing in Supabase?**

-   Check your internet connection
-   Verify the Supabase URL in `SupabaseClient.kt` matches your project
-   Check Supabase logs for any errors

## 🎨 What's Next?

-   Add more apps to monitor
-   Check your intentionality patterns in Supabase
-   Customize the prompt cooldown in `AppMonitorService.kt` (PROMPT_COOLDOWN_MS)
-   Build analytics queries in Supabase using the provided views

## 📱 Tested On

-   Android 13 (API 33)
-   Android 12 (API 31)
-   Android 10 (API 29)
-   Android 8.0 (API 26) - Minimum supported

## 🔗 Need Help?

See the full [README.md](README.md) for detailed documentation.

---

**Happy mindful app usage! 🧘‍♂️**
