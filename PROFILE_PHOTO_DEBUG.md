# Profile Photo Debug Guide

## ✅ UI Changes Made

### Search Bar Updated

-   ✅ Changed to **full-width** search bar (no profile photo in top bar)
-   ✅ Changed corner radius from `28.dp` (fully rounded) to `12.dp` (rounded rectangle)
-   ✅ Matches the reference design you provided

### What You'll See

```
┌────────────────────────────────┐
│  [🔍 Search apps...]          │
└────────────────────────────────┘
```

Instead of the previous:

```
┌───────────────────────────┬───┐
│  [🔍 Search apps...]      │ 👤│
└───────────────────────────┴───┘
```

## 🔍 Profile Photo Debugging

I've added extensive logging to help debug why the profile photo isn't displaying. Here's how to check:

### Step 1: Install the Updated App

```bash
./gradlew installDebug
```

### Step 2: Open Logcat

In Android Studio:

1. Click **Logcat** tab at the bottom
2. Filter by tag: `LoginViewModel` or `AppListViewModel`

### Step 3: Sign In and Check Logs

When you sign in with Google, you'll see these logs:

**From LoginViewModel (during sign-in):**

```
D/LoginViewModel: Google ID: [your-google-email]
D/LoginViewModel: Display Name: [Your Name]
D/LoginViewModel: Profile Picture URI: [photo-url]
D/LoginViewModel: Given Name: [First Name]
D/LoginViewModel: Family Name: [Last Name]
D/LoginViewModel: Supabase User ID: [uuid]
D/LoginViewModel: Supabase Email: [email]
D/LoginViewModel: Supabase Metadata: {metadata-json}
```

**From AppListViewModel (when loading dashboard):**

```
D/AppListViewModel: Loading user profile...
D/AppListViewModel: Session: [session-data]
D/AppListViewModel: User: [user-data]
D/AppListViewModel: User metadata: {metadata-json}
D/AppListViewModel: Extracted name: [name]
D/AppListViewModel: Extracted email: [email]
D/AppListViewModel: Extracted photoUrl: [photo-url or null]
D/AppListViewModel: User profile set: UserProfile(name=..., email=..., photoUrl=...)
```

### Step 4: What to Look For

**✅ Good Signs:**

-   `Profile Picture URI` shows a URL like `https://lh3.googleusercontent.com/...`
-   `Supabase Metadata` contains `picture` or `avatar_url` field
-   `Extracted photoUrl` is not null

**❌ Problem Signs:**

-   `Profile Picture URI: null` - Google didn't provide photo
-   `Supabase Metadata` doesn't contain photo URL
-   `Extracted photoUrl: null` - Photo URL not found in metadata

## 🔧 Possible Issues & Solutions

### Issue 1: Supabase Not Storing Google Profile Data

**Symptoms:**

-   `Profile Picture URI` in logs shows a URL
-   But `Supabase Metadata` doesn't contain it

**Solution:**
Check your Supabase project settings:

1. Go to [Supabase Dashboard](https://supabase.com/dashboard)
2. Navigate to **Authentication** → **Providers** → **Google**
3. Make sure **"Skip nonce check"** is **ENABLED**
4. Ensure the OAuth scopes include `profile` and `email`

### Issue 2: Metadata Field Names Mismatch

**Symptoms:**

-   `Supabase Metadata` has data but `Extracted photoUrl` is null

**Possible Solutions:**

The photo URL might be stored under a different key. Check the metadata and update `AppListViewModel.kt`:

```kotlin
// Try these variations:
val photoUrl = user?.userMetadata?.get("avatar_url") as? String
    ?: user?.userMetadata?.get("picture") as? String
    ?: user?.userMetadata?.get("photo_url") as? String
    ?: user?.userMetadata?.get("profile_picture") as? String
```

### Issue 3: Coil Image Loading Error

**Symptoms:**

-   Photo URL exists but image doesn't display

**Solution:**

Check if there are any Coil errors in logcat:

```
adb logcat | grep -i coil
```

Make sure you have internet permission (already added):

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Issue 4: Google OAuth Configuration

**Symptoms:**

-   Sign-in works but no profile data

**Solution:**

1. **Check OAuth Client Scopes:**

    - Go to [Google Cloud Console](https://console.cloud.google.com)
    - Navigate to **APIs & Services** → **Credentials**
    - Edit your OAuth 2.0 Client ID
    - Ensure scopes include:
        - `https://www.googleapis.com/auth/userinfo.profile`
        - `https://www.googleapis.com/auth/userinfo.email`

2. **Update GetGoogleIdOption:**

In `LoginViewModel.kt`, add scopes explicitly:

```kotlin
val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false)
    .setServerClientId("889785018407-fc3bmn63rntqrnnk25t3778nc670jm3r.apps.googleusercontent.com")
    .setRequestVerifiedPhoneNumber(false)
    .build()
```

## 📋 Manual Testing Checklist

After installing the updated app:

1. ☐ Open the app
2. ☐ Click "Sign in with Google"
3. ☐ Check logcat for `LoginViewModel` logs
4. ☐ Verify profile data is logged
5. ☐ Navigate to dashboard
6. ☐ Check logcat for `AppListViewModel` logs
7. ☐ Note what `Extracted photoUrl` shows
8. ☐ Take a screenshot of the logs
9. ☐ Share the relevant log lines if photo still doesn't show

## 🔄 Alternative: Test with Dev Mode

If Google sign-in is being problematic, you can test the UI by:

1. Click **"Skip for now (Dev Mode)"** on login screen
2. This creates a mock user but won't have a real photo URL
3. You'll see the fallback person icon instead

## 📱 Expected Behavior

**With Photo:**

-   Circular profile image loads from Google
-   Should show your Google account photo
-   Has a 2dp primary-colored border

**Without Photo (Fallback):**

-   Person icon (👤) displays instead
-   Icon is 28dp in size
-   Primary color tint
-   Still has circular background with border

## 📤 Sharing Debug Info

If the profile photo still doesn't work after these steps, share:

1. **Relevant log lines** from `LoginViewModel` showing:

    - Profile Picture URI
    - Supabase Metadata

2. **Relevant log lines** from `AppListViewModel` showing:

    - Extracted photoUrl value

3. **Screenshot** of the dashboard showing the current state

---

**Current Build:**

-   ✅ Built: October 23, 2024, 14:12
-   ✅ Size: 14 MB
-   ✅ Status: Ready to install and test
