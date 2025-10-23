# Google OAuth Setup Guide

## Your SHA Certificates

**SHA-1 (Debug):**

```
D6:AE:E3:B9:3C:22:5E:D0:68:B8:6C:E6:53:F7:E6:30:80:4C:D4:08
```

**SHA-256 (Debug):**

```
63:B8:AF:AE:52:02:01:3C:02:C3:07:22:51:3C:F3:49:12:BE:F1:CA:F0:EC:0E:DA:DA:B4:94:F3:C7:08:C2:07
```

## Step 1: Google Cloud Console Setup

### 1.1 Create OAuth Client ID

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable **Google+ API** (Required for sign-in):

    - Go to **APIs & Services** → **Library**
    - Search for "Google+ API"
    - Click **Enable**

4. Create OAuth credentials:

    - Go to **APIs & Services** → **Credentials**
    - Click **Create Credentials** → **OAuth client ID**
    - If prompted, configure the OAuth consent screen first:
        - User Type: **External**
        - App name: **Intentionality**
        - User support email: Your email
        - Developer contact: Your email
        - Scopes: Just email and profile (default)
        - Test users: Add your Gmail account

5. Now create the OAuth client:

    - Click **Create Credentials** → **OAuth client ID**
    - Application type: **Android**
    - Name: **Intentionality Android**
    - Package name: `com.nibodhdaware.intentionality`
    - SHA-1 certificate fingerprint: `D6:AE:E3:B9:3C:22:5E:D0:68:B8:6C:E6:53:F7:E6:30:80:4C:D4:08`
    - Click **Create**

6. **Copy the Client ID** (you'll need this for Supabase)

### 1.2 Create Web Client ID (Important!)

Supabase also needs a **Web application** OAuth client:

1. Click **Create Credentials** → **OAuth client ID**
2. Application type: **Web application**
3. Name: **Intentionality Web**
4. Authorized redirect URIs:
    - Add: `https://wsmoiglcfnegltuuwhnh.supabase.co/auth/v1/callback`
5. Click **Create**
6. **Copy both Client ID and Client Secret**

## Step 2: Supabase Configuration

1. Go to [Supabase Dashboard](https://supabase.com/dashboard)
2. Select your project
3. Go to **Authentication** → **Providers**
4. Find **Google** and click to expand
5. Enable the toggle
6. Enter:
    - **Client ID**: (Web Client ID from step 1.2)
    - **Client Secret**: (Web Client Secret from step 1.2)
    - **Authorized Client IDs**: Add your Android Client ID from step 1.1
7. Click **Save**

## Step 3: App Configuration (Already Done!)

The app is already configured with:

-   ✅ Supabase URL: `https://wsmoiglcfnegltuuwhnh.supabase.co`
-   ✅ Supabase Key: `sb_publishable_ernPkyC6EdAA0d31G0X2sw_TZumZ_1l`
-   ✅ Package name: `com.nibodhdaware.intentionality`

## Step 4: Update LoginViewModel (Code Update Needed)

You'll need to implement the actual Google Sign-In. Here's the updated code:

### Update LoginViewModel.kt:

```kotlin
fun signInWithGoogle(context: Context) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            supabase.auth.signInWith(Google) {
                // This will open Google Sign-In in a browser
            }

            // After successful sign-in, the app will receive a callback
            // Check for session
            delay(1000) // Give time for callback
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    userId = session.user?.id
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Sign-in was cancelled or failed"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Login failed"
            )
        }
    }
}
```

### Update LoginScreen.kt:

```kotlin
Button(
    onClick = {
        viewModel.signInWithGoogle(context)  // Pass context
    },
    // ... rest of button code
)
```

## Step 5: Add Deep Link Handler (Optional but Recommended)

Add this to your `MainActivity.kt` to handle OAuth callbacks:

```kotlin
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.data?.let { uri ->
        if (uri.scheme == "com.nibodhdaware.intentionality" && uri.host == "login") {
            // Handle OAuth callback
            lifecycleScope.launch {
                try {
                    // The callback is handled automatically by Supabase
                    Log.d("MainActivity", "OAuth callback received")
                } catch (e: Exception) {
                    Log.e("MainActivity", "OAuth callback error", e)
                }
            }
        }
    }
}
```

## Testing the Setup

1. **Rebuild the app**:

    ```bash
    ./gradlew clean assembleDebug
    ./gradlew installDebug
    ```

2. **Launch the app**
3. **Tap "Sign in with Google"**
4. **Browser/WebView opens** with Google Sign-In
5. **Select your Google account**
6. **Grant permissions**
7. **App returns** and you're logged in!

## Troubleshooting

### "Sign-in failed" or nothing happens

**Check:**

1. ✅ Web Client ID/Secret added to Supabase (not Android Client ID!)
2. ✅ Android Client ID added to "Authorized Client IDs" in Supabase
3. ✅ Callback URL correct: `https://wsmoiglcfnegltuuwhnh.supabase.co/auth/v1/callback`
4. ✅ Google+ API enabled in Google Cloud Console
5. ✅ SHA-1 certificate matches your debug keystore

### "Redirect URI mismatch"

-   Make sure the Web OAuth client has the exact callback URL
-   No trailing slashes, exact match required

### "Invalid client"

-   Check that you're using the **Web** Client ID in Supabase, not Android
-   Verify Client Secret is correct

### Testing with your account

-   Add your Gmail to "Test users" in OAuth consent screen
-   App must be in "Testing" mode for external users

## Production Setup

When ready for production:

1. Generate release keystore:

    ```bash
    keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
    ```

2. Get release SHA-1:

    ```bash
    keytool -list -v -keystore release.keystore
    ```

3. Add release SHA-1 to Google Cloud Console OAuth client
4. Move OAuth consent screen from "Testing" to "Published"

## Summary

**What you need:**

-   ✅ SHA-1: `D6:AE:E3:B9:3C:22:5E:D0:68:B8:6C:E6:53:F7:E6:30:80:4C:D4:08`
-   ✅ Package: `com.nibodhdaware.intentionality`
-   ✅ Callback: `https://wsmoiglcfnegltuuwhnh.supabase.co/auth/v1/callback`

**Create in Google Cloud:**

1. Android OAuth client (with SHA-1)
2. Web OAuth client (with callback URL)

**Add to Supabase:**

-   Web Client ID + Secret
-   Android Client ID in "Authorized Client IDs"

---

**Once configured, you can remove the "Skip for now" button and users will sign in with Google!** 🎉
