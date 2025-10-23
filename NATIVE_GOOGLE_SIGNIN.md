# Native In-App Google Sign-In Setup ✅

## What Changed

✅ **No more browser redirects!**  
✅ **Sign-In happens entirely within the app**  
✅ **Uses Google Credential Manager API**  
✅ **Modern Android approach (recommended by Google)**

## How It Works Now

1. User taps "Sign in with Google"
2. **Native Android dialog appears** (no browser!)
3. User selects Google account
4. App gets ID token directly
5. App signs into Supabase with the token
6. User is logged in!

## Setup Steps

### Step 1: Get Your Web Client ID

You need the **Web OAuth Client ID** from Google Cloud Console (not the Android one).

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to **APIs & Services** → **Credentials**
3. Find your **Web application** OAuth client
4. Copy the **Client ID** (looks like: `123456789-abc.apps.googleusercontent.com`)

### Step 2: Update LoginViewModel.kt

Open: `app/src/main/java/com/nibodhdaware/intentionality/ui/auth/LoginViewModel.kt`

Find line 62 and replace `YOUR_WEB_CLIENT_ID_HERE` with your actual Web Client ID:

```kotlin
// BEFORE:
.setServerClientId("YOUR_WEB_CLIENT_ID_HERE")

// AFTER (example):
.setServerClientId("123456789-abc.apps.googleusercontent.com")
```

**⚠️ Important:** Use the **Web** Client ID, not the Android Client ID!

### Step 3: Configure Supabase

In your Supabase Dashboard:

1. Go to **Authentication** → **Providers** → **Google**
2. Enable Google provider
3. Enter your **Web Client ID** and **Secret**
4. Save

### Step 4: Rebuild & Test

```bash
./gradlew clean assembleDebug
./gradlew installDebug
```

## Testing

1. Open the app
2. Tap "Sign in with Google"
3. **Native Android dialog appears** showing your Google accounts
4. Select your account
5. ✅ You're logged in!

## What You'll See

Instead of opening a browser, you'll see a native Android bottom sheet with:

-   Your Google accounts
-   Google logo and branding
-   "Continue as [Your Name]" button
-   Option to add another account

It looks like this:

```
┌─────────────────────────┐
│ Sign in with Google     │
│                         │
│ 🔵 yourname@gmail.com   │
│    Your Name            │
│                         │
│ + Use another account   │
│                         │
│ [Continue]              │
└─────────────────────────┘
```

## Dependencies Added

✅ `play-services-auth` - Google Play Services  
✅ `credentials` - Android Credential Manager  
✅ `credentials-play-services-auth` - Integration  
✅ `googleid` - Google ID library

## Advantages Over Browser OAuth

| Feature               | Native (New)      | Browser (Old) |
| --------------------- | ----------------- | ------------- |
| User Experience       | ✅ Smooth, in-app | ❌ Leaves app |
| Speed                 | ✅ Fast           | ❌ Slower     |
| Feels Like            | Native Android    | Web redirect  |
| User Trust            | ✅ Higher         | ❌ Lower      |
| Complexity            | ✅ Simple         | ❌ Deep links |
| Google Recommendation | ✅ Yes            | ❌ Legacy     |

## Security

-   ✅ Uses Google's official Credential Manager
-   ✅ ID token verified by Supabase
-   ✅ Secure token exchange
-   ✅ No manual token handling
-   ✅ Follows Android best practices

## Troubleshooting

### "No eligible accounts"

**Solution:**

-   Make sure you have a Google account added to your Android device
-   Go to Settings → Accounts → Add Google Account

### "Developer error" or "API not enabled"

**Solution:**

1. Check Web Client ID is correct in `LoginViewModel.kt`
2. Verify SHA-1 certificate is added to Android OAuth client
3. Enable Google+ API in Google Cloud Console

### "Sign-In failed"

**Check:**

-   Web Client ID is configured in Supabase
-   Google provider is enabled in Supabase
-   Your test account is added to OAuth consent screen test users

### Still opens browser?

**This means:**

-   Google Credential Manager isn't available on your device
-   Falls back to browser OAuth
-   Update Google Play Services on your device

## Code Changes Summary

### LoginViewModel.kt

-   Replaced `signInWith(Google)` with Credential Manager API
-   Added `GetGoogleIdOption` configuration
-   Signs in with ID token instead of OAuth redirect

### LoginScreen.kt

-   Added coroutine scope for async sign-in
-   Passes context to ViewModel

### MainActivity.kt

-   Removed deep link handling (no longer needed!)
-   Simplified code

### AndroidManifest.xml

-   Deep link intent filter still there but unused
-   Can be removed if you want (optional)

## Configuration Checklist

-   [ ] Get Web Client ID from Google Cloud Console
-   [ ] Replace `YOUR_WEB_CLIENT_ID_HERE` in `LoginViewModel.kt`
-   [ ] Configure Web Client ID in Supabase
-   [ ] Enable Google provider in Supabase
-   [ ] Add SHA-1 to Android OAuth client (for future features)
-   [ ] Rebuild the app
-   [ ] Test on device with Google account

## Your Configuration

**Package Name:** `com.nibodhdaware.intentionality`  
**SHA-1:** `D6:AE:E3:B9:3C:22:5E:D0:68:B8:6C:E6:53:F7:E6:30:80:4C:D4:08`  
**Supabase URL:** `https://wsmoiglcfnegltuuwhnh.supabase.co`

## Next Steps

1. **Get your Web Client ID** from Google Cloud Console
2. **Replace the placeholder** in `LoginViewModel.kt` line 62
3. **Rebuild:** `./gradlew assembleDebug`
4. **Test:** Tap "Sign in with Google" and enjoy in-app sign-in! 🎉

---

**No more browser redirects! Sign-In is now 100% native and in-app!** ✨
