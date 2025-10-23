# Supabase Restored - Back to Original Setup

✅ Successfully reverted from Firebase back to Supabase!

## What Changed

All Firebase code has been removed and the original Supabase implementation has been restored.

### Dependencies Restored

**Removed Firebase:**

-   ❌ Firebase SDK (`com.google.firebase:firebase-bom`)
-   ❌ Firebase Authentication
-   ❌ Firebase Firestore
-   ❌ Firebase Analytics
-   ❌ Google Services Gradle plugin

**Restored Supabase:**

-   ✅ Supabase SDK (`io.github.jan-tennert.supabase:bom:2.0.0`)
-   ✅ Supabase Auth (`gotrue-kt`)
-   ✅ Supabase Postgrest (`postgrest-kt`)
-   ✅ Ktor client for Supabase
-   ✅ kotlinx-serialization

### Files Restored

**Recreated:**

1. ✅ `SupabaseClient.kt` - Supabase client configuration
2. ✅ `SupabaseRepository.kt` - Database operations

**Reverted:**

1. ✅ `LoginViewModel.kt` - Now uses Supabase Auth with Google ID token
2. ✅ `AppListViewModel.kt` - Now uses Supabase for user profile
3. ✅ `IntentionPromptActivity.kt` - Now saves to Supabase

**Deleted:**

1. ❌ `FirebaseManager.kt`
2. ❌ `FirestoreRepository.kt`
3. ❌ `FIREBASE_SETUP.md`
4. ❌ `FIREBASE_MIGRATION.md`
5. ❌ `QUICKSTART_FIREBASE.md`

## Current Configuration

### Supabase Details

**Supabase URL:** `https://wsmoiglcfnegltuuwhnh.supabase.co`  
**Callback URL:** `https://wsmoiglcfnegltuuwhnh.supabase.co/auth/v1/callback`  
**Anon Key:** Configured in `SupabaseClient.kt`

### Authentication

Uses native Google Sign-In with Supabase:

1. User clicks "Sign in with Google"
2. Google Credential Manager returns ID token
3. ID token sent to Supabase: `supabase.auth.signInWith(IDToken)`
4. Supabase validates and creates session
5. User data accessed via `session.user.userMetadata`

### Data Structure

**Supabase Table:** `app_entries`

```sql
CREATE TABLE app_entries (
  id BIGSERIAL PRIMARY KEY,
  app_name TEXT NOT NULL,
  package_name TEXT NOT NULL,
  reason TEXT,
  rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
  timestamp TIMESTAMPTZ NOT NULL,
  user_id TEXT NOT NULL
);
```

### Build Status

✅ **Build Successful**  
✅ **APK Size:** 14 MB  
✅ **No Compilation Errors**

## Testing the App

### Install

```bash
./gradlew installDebug
```

### What to Test

1. **Google Sign-In**

    - Click "Sign in with Google"
    - Select your Google account
    - Verify dashboard shows your profile

2. **App Monitoring**

    - Select apps to monitor
    - Click "Start Monitoring"
    - Grant Usage Access permission
    - Open a monitored app
    - Fill in the intention prompt
    - Click "Continue to App"

3. **Verify Data in Supabase**
    - Go to [Supabase Dashboard](https://supabase.com/dashboard)
    - Navigate to your project
    - Go to **Table Editor** → `app_entries`
    - Verify entries are saved with correct `user_id`

## Supabase Setup Reminder

Make sure your Supabase project has:

### 1. Google OAuth Enabled

1. Go to Supabase Dashboard → **Authentication** → **Providers**
2. Enable **Google** provider
3. Add OAuth credentials from Google Cloud Console
4. Redirect URL: `https://wsmoiglcfnegltuuwhnh.supabase.co/auth/v1/callback`

### 2. Table Created

Run the SQL from `supabase_setup.sql`:

```sql
CREATE TABLE app_entries (
  id BIGSERIAL PRIMARY KEY,
  app_name TEXT NOT NULL,
  package_name TEXT NOT NULL,
  reason TEXT,
  rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
  timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  user_id TEXT NOT NULL
);

-- Enable RLS
ALTER TABLE app_entries ENABLE ROW LEVEL SECURITY;

-- Policies
CREATE POLICY "Users can insert their own entries" ON app_entries
  FOR INSERT WITH CHECK (auth.uid()::text = user_id OR user_id = 'anonymous');

CREATE POLICY "Users can read their own entries" ON app_entries
  FOR SELECT USING (auth.uid()::text = user_id OR user_id = 'anonymous');
```

### 3. Row Level Security (RLS)

RLS policies ensure users can only access their own data.

## Why Revert from Firebase?

Firebase wasn't working as expected. Supabase provides:

-   ✅ Direct PostgreSQL database (familiar SQL)
-   ✅ Better control over authentication flow
-   ✅ Row Level Security for data privacy
-   ✅ More flexible for web integration

## Chrome Extension Integration

With Supabase, you can easily sync data with your Chrome extension:

### REST API Access

```javascript
// Chrome extension can use Supabase REST API
const supabaseUrl = "https://wsmoiglcfnegltuuwhnh.supabase.co";
const supabaseKey = "your-anon-key";

const { data, error } = await fetch(
    `${supabaseUrl}/rest/v1/app_entries?user_id=eq.${userId}`,
    {
        headers: {
            apikey: supabaseKey,
            Authorization: `Bearer ${accessToken}`,
        },
    },
);
```

### Same Authentication

Both Android app and Chrome extension can use the same Supabase Auth system.

## Troubleshooting

### Google Sign-In Not Working

**Solution:**

1. Verify OAuth client ID is correct in `LoginViewModel.kt`
2. Check SHA-1 certificate is added to Google Cloud Console
3. Ensure Supabase has Google provider enabled

### Data Not Saving

**Solution:**

1. Check Supabase project is active
2. Verify `app_entries` table exists
3. Check RLS policies allow INSERT
4. Ensure user is authenticated

### "Session expired" Error

**Solution:**

1. Sign out and sign in again
2. Check Supabase project settings
3. Verify token refresh is working

## Project Structure

```
app/src/main/java/com/nibodhdaware/intentionality/
├── supabase/
│   ├── SupabaseClient.kt          # ✅ Restored
│   └── SupabaseRepository.kt      # ✅ Restored
├── ui/
│   ├── auth/
│   │   └── LoginViewModel.kt      # ✅ Reverted to Supabase
│   ├── applist/
│   │   └── AppListViewModel.kt    # ✅ Reverted to Supabase
│   └── prompt/
│       └── IntentionPromptActivity.kt  # ✅ Reverted to Supabase
└── (no firebase/ directory)       # ❌ Deleted
```

## Documentation

-   **`README.md`** - Main project documentation (needs update)
-   **`supabase_setup.sql`** - Database setup script
-   **`GOOGLE_OAUTH_SETUP.md`** - Google OAuth configuration
-   **`NATIVE_GOOGLE_SIGNIN.md`** - Native sign-in implementation
-   **`SHA_CERTIFICATES.txt`** - SHA certificates for OAuth

## Next Steps

1. ✅ **App is built and ready** - `./gradlew installDebug`
2. 🔧 **Configure Supabase** - Enable Google OAuth
3. 🔧 **Create table** - Run SQL from `supabase_setup.sql`
4. 🔧 **Set up RLS** - Configure security policies
5. ✅ **Test the app** - Sign in, monitor apps, check data

---

**Back to Supabase! 🟢**

The app is now using Supabase as it was before the Firebase migration attempt.
