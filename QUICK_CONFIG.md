# Quick Configuration - Native Google Sign-In

## ⚡ 3 Steps to Enable Native Google Sign-In

### Step 1: Get Web Client ID (1 minute)

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click **APIs & Services** → **Credentials**
3. Find **Web application** OAuth 2.0 Client
4. Copy the **Client ID** (format: `123456-abc.apps.googleusercontent.com`)

### Step 2: Update Code (30 seconds)

Open this file:

```
app/src/main/java/com/nibodhdaware/intentionality/ui/auth/LoginViewModel.kt
```

**Line 62** - Replace this:

```kotlin
.setServerClientId("YOUR_WEB_CLIENT_ID_HERE")
```

With your Web Client ID:

```kotlin
.setServerClientId("YOUR_ACTUAL_CLIENT_ID")
```

### Step 3: Rebuild (1 minute)

```bash
./gradlew clean assembleDebug
./gradlew installDebug
```

## ✅ Done!

Now tap "Sign in with Google" and you'll see a **native Android dialog** instead of browser redirect!

---

## What You Need

| Item              | Where to Get It                                                                |
| ----------------- | ------------------------------------------------------------------------------ |
| Web Client ID     | Google Cloud Console → Credentials → Web application                           |
| Web Client Secret | Same place (for Supabase config)                                               |
| SHA-1 Certificate | Already have it: `D6:AE:E3:B9:3C:22:5E:D0:68:B8:6C:E6:53:F7:E6:30:80:4C:D4:08` |

## Supabase Configuration

In Supabase Dashboard → Authentication → Providers → Google:

-   **Enable:** ✅ Turn on
-   **Client ID:** Your Web Client ID
-   **Client Secret:** Your Web Client Secret

## Testing

1. Open app
2. Tap "Sign in with Google"
3. **Native dialog appears** (no browser!)
4. Select Google account
5. ✅ Logged in!

## Troubleshooting

**No accounts shown?**

-   Add a Google account to your Android device

**"Developer error"?**

-   Check Web Client ID is correct (line 62)
-   Verify Supabase configuration

**Still opens browser?**

-   Update Google Play Services on device
-   The new Credential Manager might not be available

---

**See `NATIVE_GOOGLE_SIGNIN.md` for complete documentation.**
