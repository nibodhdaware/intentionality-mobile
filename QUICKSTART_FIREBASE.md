# Firebase Quick Start Guide

## ✅ Migration Complete!

Your app has been successfully migrated from Supabase to Firebase! The APK is already built and ready to test.

## What Happened

All code has been updated to use Firebase instead of Supabase:

-   ✅ Firebase SDK dependencies added
-   ✅ Firebase Authentication integrated
-   ✅ Firebase Firestore for data storage
-   ✅ All code migrated (no Supabase references)
-   ✅ APK built successfully (16 MB)
-   ✅ `google-services.json` already in place

## Quick Test

Install and test the app right now:

```bash
./gradlew installDebug
```

The app will:

1. Sign in with Google using Firebase Auth
2. Save your profile to Firestore
3. Save app usage entries to Firestore collection `app_entries`

## Firebase Console Setup (Required)

You need to configure Firebase services for the app to work properly:

### 1. Enable Google Authentication

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Authentication** → **Sign-in method**
4. Click **Google** provider
5. Toggle **Enable**
6. Select a support email
7. Click **Save**

### 2. Create Firestore Database

1. In Firebase Console, go to **Firestore Database**
2. Click **Create database**
3. Choose **Start in production mode**
4. Select a location (e.g., `us-central`)
5. Click **Enable**

### 3. Set Up Security Rules

After creating the database:

1. Go to **Firestore Database** → **Rules** tab
2. Replace the default rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow authenticated users to read/write their own app entries
    match /app_entries/{entryId} {
      allow read, write: if request.auth != null &&
                           request.auth.uid == resource.data.userId;
      allow create: if request.auth != null &&
                       request.auth.uid == request.resource.data.userId;
    }

    // Allow authenticated users to read/write their own user profile
    match /users/{userId} {
      allow read, write: if request.auth != null &&
                           request.auth.uid == userId;
    }
  }
}
```

3. Click **Publish**

### 4. Verify SHA-1 Certificate (Already Done)

Your SHA-1 certificate should already be configured:

```
D6:AE:E3:B9:3C:22:5E:D0:68:B8:6C:E6:53:F7:E6:30:80:4C:D4:08
```

If you need to add it:

1. Go to **Project Settings** → **Your apps**
2. Find your Android app
3. Click **Add fingerprint**
4. Paste the SHA-1 above

## Data Structure

The app saves data to Firestore in this format:

### Collection: `app_entries`

```javascript
{
  "userId": "xY8zAb123...",           // Firebase User ID
  "appName": "Instagram",              // Human-readable app name
  "packageName": "com.instagram.android", // Android package
  "reason": "Check messages from friends", // User's reason
  "rating": 2,                         // 1-5 (1=intentional, 5=regretful)
  "timestamp": "2024-10-23T13:15:00.000Z", // ISO 8601 UTC
  "createdAt": Timestamp(...)         // Firestore server timestamp
}
```

### Collection: `users` (auto-created on sign-in)

```javascript
{
  "userId": "xY8zAb123...",
  "email": "user@example.com",
  "displayName": "John Doe",
  "photoURL": "https://...",
  "lastUpdated": Timestamp(...)
}
```

## Syncing with Chrome Extension

Since both your Android app and Chrome extension now use the same Firebase project:

### Shared Authentication

Users sign in with the same Google account across both platforms.

### Shared Database

Query the same data from Chrome extension:

```javascript
// Get user's app entries
const db = firebase.firestore();
const entriesRef = db.collection("app_entries");

const userEntries = await entriesRef
    .where("userId", "==", firebase.auth().currentUser.uid)
    .orderBy("timestamp", "desc")
    .limit(100)
    .get();

userEntries.forEach((doc) => {
    console.log(doc.id, "=>", doc.data());
});
```

### Real-Time Updates (Optional)

Listen for new entries in real-time:

```javascript
entriesRef
    .where("userId", "==", currentUser.uid)
    .orderBy("timestamp", "desc")
    .onSnapshot((snapshot) => {
        snapshot.docChanges().forEach((change) => {
            if (change.type === "added") {
                console.log("New entry:", change.doc.data());
            }
        });
    });
```

## Testing the App

### 1. Install the App

```bash
./gradlew installDebug
```

### 2. Test Google Sign-In

1. Open the app
2. Click "Sign in with Google"
3. Select your Google account
4. Verify login successful
5. Check dashboard shows your name, email, and profile picture

### 3. Test App Monitoring

1. Search for an app (e.g., "Chrome")
2. Check the box to monitor it
3. Click "Start Monitoring" at the bottom
4. Grant "Usage Access" permission when prompted
5. Open Chrome (or the app you selected)
6. You should see the full-screen prompt:
    - "Why are you opening Chrome?"
    - Text input for reason
    - Dropdown for intentionality rating (1-5)
    - "Continue to App" button
    - "Go Back to Home" button

### 4. Verify Data in Firebase

1. Go to Firebase Console
2. Navigate to **Firestore Database**
3. Click on **Data** tab
4. You should see:
    - `app_entries` collection with your entries
    - `users` collection with your profile

## Troubleshooting

### Sign-In Fails

**Error**: "Sign-in failed" or "Invalid credential"

**Solution**:

1. Verify Google Authentication is enabled in Firebase Console
2. Check SHA-1 certificate is added
3. Ensure `google-services.json` is up to date

### Data Not Saving

**Error**: "Permission denied" in Firestore

**Solution**:

1. Check security rules are set up correctly (see step 3 above)
2. Verify user is authenticated
3. Check Firebase Console → **Firestore Database** → **Usage** for errors

### Prompt Not Appearing

**Error**: No prompt when opening monitored app

**Solution**:

1. Grant "Usage Access" permission in Android Settings
2. Verify monitoring service is running (notification visible)
3. Check app is selected in the app list

## Files to Review

-   **`FIREBASE_SETUP.md`** - Detailed Firebase setup instructions
-   **`FIREBASE_MIGRATION.md`** - Complete migration details
-   **`README.md`** - Updated project documentation
-   **`app/build.gradle.kts`** - Firebase dependencies
-   **`app/src/main/java/com/nibodhdaware/intentionality/firebase/`** - Firebase code

## Next Steps

1. ✅ **Install the app** - `./gradlew installDebug`
2. 🔧 **Enable Firebase Auth** - Firebase Console → Authentication
3. 🔧 **Create Firestore Database** - Firebase Console → Firestore
4. 🔧 **Set security rules** - Copy from above
5. ✅ **Test the app** - Sign in, monitor apps, check data
6. 🚀 **Sync with Chrome extension** - Use same Firebase project

---

**You're all set! 🎉**

The app is now using Firebase and ready to sync with your Chrome extension!
