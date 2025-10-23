# Intentionality - Mindful App Usage Tracker

A native Android app built with Kotlin and Jetpack Compose that helps users track their app usage with mindful intention prompts.

## Features

### 1. **Authentication**

-   Login screen with Google Sign-In via Firebase Auth
-   "Skip for now" development mode for testing (anonymous auth)
-   Session management with automatic login state persistence

### 2. **App Selection Dashboard**

-   Browse and select installed apps to monitor
-   Beautiful Material Design 3 UI with custom dark theme
-   Search through all launcher apps (excludes system apps)
-   User profile display (name, email, photo) in top right corner
-   Graph area (coming soon) with smooth fade animations
-   Save selections to local database (Room)

### 3. **Background Monitoring**

-   Foreground service monitors selected apps using UsageStatsManager
-   30-second cooldown between prompts for the same app
-   Efficient 2-second polling interval
-   Persistent notification while monitoring is active

### 4. **Intention Prompts**

-   Full-screen activity appears when a monitored app is opened
-   Prompt: "Why are you opening [App Name]?"
-   Text input for user's reason
-   5-point intentionality scale:
    -   1 - Very intentional
    -   2 - Somewhat intentional
    -   3 - Not intentional
    -   4 - Mindless
    -   5 - Regretful
-   "Continue to App" button to proceed
-   "Go Back to Home" button to return to home screen

### 5. **Data Storage**

-   All entries saved to Firebase Firestore with:
    -   `appName`: Display name of the app
    -   `packageName`: Android package identifier
    -   `reason`: User's text input
    -   `rating`: 1-5 intentionality score
    -   `timestamp`: UTC timestamp
    -   `userId`: Firebase user ID
    -   `createdAt`: Server timestamp

## Tech Stack

-   **Language**: Kotlin
-   **UI Framework**: Jetpack Compose with Material Design 3
-   **Architecture**: MVVM with ViewModels and StateFlow
-   **Local Database**: Room
-   **Backend**: Firebase (Firestore, Auth)
-   **Image Loading**: Coil
-   **Async**: Kotlin Coroutines
-   **Minimum SDK**: Android 8.0 (API 26)
-   **Target SDK**: Android 14 (API 36)

## Setup Instructions

### 1. Firebase Configuration

**IMPORTANT**: You must download `google-services.json` from your Firebase project and place it in the `app/` folder.

See **[FIREBASE_SETUP.md](FIREBASE_SETUP.md)** for detailed instructions on:

-   Adding Android app to your Firebase project
-   Downloading `google-services.json`
-   Enabling Firebase Authentication (Google provider)
-   Setting up Cloud Firestore
-   Configuring security rules

#### Quick Setup Checklist

1. ✅ Add Android app to Firebase Console
2. ✅ Download `google-services.json` → place in `app/` folder
3. ✅ Enable Google Authentication
4. ✅ Create Firestore database
5. ✅ Set up security rules
6. ✅ Add SHA-1 certificate (from `SHA_CERTIFICATES.txt`)

#### Firestore Collections

The app uses the following Firestore collections:

**`app_entries`**: Stores each app usage entry

```javascript
{
  "userId": "firebase-user-uid",
  "appName": "Instagram",
  "packageName": "com.instagram.android",
  "reason": "Check messages from friends",
  "rating": 2,
  "timestamp": "2024-10-23T12:34:56.789Z",
  "createdAt": ServerTimestamp
}
```

**`users`** (optional): Stores user profile information

```javascript
{
  "userId": "firebase-user-uid",
  "email": "user@example.com",
  "displayName": "John Doe",
  "photoURL": "https://...",
  "lastUpdated": ServerTimestamp
}
```

### 2. Required Permissions

The app requires one special permission that users must grant manually:

1. **Usage Access** (`PACKAGE_USAGE_STATS`)
    - Allows the app to detect which apps are in the foreground
    - Users will be prompted to enable this in Settings

## Building and Running

### Prerequisites

-   Android Studio Hedgehog or later
-   Android SDK 26+
-   Kotlin 2.0.21+
-   Firebase project with `google-services.json`

### Build Steps

1. **Clone the repository** (if applicable):

```bash
cd /Users/nibodhdaware/AndroidStudioProjects/intentionality
```

2. **Add `google-services.json`**:

    - Download from Firebase Console
    - Place in `app/google-services.json`

3. **Open the project in Android Studio**

4. **Sync Gradle dependencies**:

    - File → Sync Project with Gradle Files

5. **Build the APK**:

```bash
./gradlew clean
./gradlew assembleDebug
```

6. **Install on device**:

```bash
./gradlew installDebug
```

Or use Android Studio's Run button (▶️)

## Usage Guide

### First Launch

1. **Login**: Use "Skip for now" for development (anonymous auth), or sign in with Google
2. **View Dashboard**: See your profile in top right, search bar at top
3. **Select Apps**: Check the apps you want to monitor
4. **Grant Permission**: Tap "Start Monitoring" and allow Usage Access permission
5. **Start Monitoring**: Monitoring begins after permission is granted

### During Use

-   When you open a monitored app, a full-screen prompt will appear
-   Fill in why you're opening the app
-   Select your intentionality level (1-5)
-   Tap "Continue to App" to proceed and save the entry
-   Tap "Go Back to Home" to cancel and return to home screen

### Stopping Monitoring

-   Tap "Stop Monitoring" button at the bottom of the dashboard
-   The service will stop and the notification will disappear

## Project Structure

```
app/src/main/java/com/nibodhdaware/intentionality/
├── IntentionalityApp.kt           # Application class
├── MainActivity.kt                # Main entry point
├── database/                      # Room database
│   ├── AppDatabase.kt
│   ├── MonitoredApp.kt
│   ├── MonitoredAppDao.kt
│   └── MonitoredAppRepository.kt
├── firebase/                      # Firebase integration
│   ├── FirebaseManager.kt        # Firebase instances
│   └── FirestoreRepository.kt    # Firestore operations
├── navigation/                    # Navigation setup
│   └── NavGraph.kt
├── service/                       # Background services
│   └── AppMonitorService.kt      # Main monitoring service
└── ui/                            # UI components
    ├── applist/                   # App selection dashboard
    │   ├── AppListScreen.kt
    │   └── AppListViewModel.kt
    ├── auth/                      # Authentication
    │   ├── LoginScreen.kt
    │   └── LoginViewModel.kt
    ├── prompt/                    # Intention prompt
    │   └── IntentionPromptActivity.kt
    └── theme/                     # Material Design theme
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## Theme Colors

The app uses a custom Material Design 3 dark theme:

-   **Background**: `#18344A` (Deep Blue)
-   **Primary**: `#295B7A` (Medium Blue)
-   **Accent**: `#4A90A4` (Light Blue)
-   **Surface**: `#1F3A4F` (Dark Surface)
-   **On Background**: `#E2E8F0` (Light Text)

## Troubleshooting

### Build Errors

#### "google-services.json not found"

**Solution**: Download `google-services.json` from Firebase Console and place in `app/` folder.

#### "Default FirebaseApp is not initialized"

**Solution**:

1. Ensure `google-services.json` is in the correct location (`app/` folder)
2. Sync Gradle files
3. Clean and rebuild

### Runtime Issues

#### Google Sign-In not working

**Solution**:

1. Verify SHA-1 certificate is added to Firebase Console
2. Check that OAuth client is enabled
3. Ensure `google-services.json` contains the correct OAuth client ID

#### Prompts Not Appearing

-   Ensure "Usage Access" permission is granted
-   Check that the monitoring service is running (notification visible)
-   Verify the app is selected in the app list

#### Usage Stats Not Working

-   Grant "Usage Access" permission in Android Settings
-   Try toggling the permission off and on
-   Restart the app

#### Firestore Connection Issues

-   Check internet connection
-   Verify Firebase project is active
-   Check Firestore security rules
-   View logs in Firebase Console

## Syncing with Chrome Extension

Since this app uses Firebase, you can easily sync data with a Chrome extension on the same Firebase project:

1. **Shared Authentication**: Users sign in with the same Google account
2. **Shared Database**: Both platforms access the same Firestore collections
3. **Cross-Platform Insights**: View app usage from both Android and web

Example Firestore query in Chrome extension:

```javascript
const entriesRef = firebase.firestore().collection("app_entries");
const userEntries = await entriesRef
    .where("userId", "==", currentUser.uid)
    .orderBy("timestamp", "desc")
    .limit(100)
    .get();
```

## Future Enhancements

-   [ ] Analytics dashboard with usage graphs
-   [ ] Weekly/monthly reports
-   [ ] Custom prompt messages per app
-   [ ] Export data to CSV/JSON
-   [ ] Widget for quick monitoring toggle
-   [ ] Cross-platform sync with web dashboard
-   [ ] Machine learning for pattern detection
-   [ ] App usage time tracking

## License

This project is for personal use. All rights reserved.

## Credits

Built with ❤️ using:

-   [Jetpack Compose](https://developer.android.com/jetpack/compose)
-   [Firebase](https://firebase.google.com)
-   [Room Database](https://developer.android.com/training/data-storage/room)
-   [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
-   [Coil](https://coil-kt.github.io/coil/)
