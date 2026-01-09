# UI Fixes and Firebase Sync Implementation ✅

## Overview

Fixed UI issues in the scheduling feature and implemented proper Firebase data synchronization with the exact schema format.

## Issues Fixed

### 1. ✅ Text Wrapping in Overlay Repeat Interval

**Problem:** Text "Show overlay every X minutes" was wrapping awkwardly

**Solution:**

-   Added `maxLines = 2` to text elements
-   Changed "minutes" to singular "minute" when interval = 1
-   Applied to both AppConfigScreen slider text and summary card

**Files Modified:**

-   `app/src/main/java/com/nibodhdaware/intentionality/ui/appconfig/AppConfigScreen.kt`

**Changes:**

```kotlin
// Before
Text(text = "Show overlay every $intervalMinutes minutes")

// After
Text(
    text = "Show overlay every $intervalMinutes ${if (intervalMinutes == 1) "minute" else "minutes"}",
    maxLines = 2
)
```

### 2. ✅ Settings Icon and Interval Text Visibility

**Problem:**

-   Settings icon was not visible in monitored apps list
-   "Every X min" text was not prominent enough

**Solution:**

-   Made settings icon larger (40dp) with 24dp icon size
-   Added primary color tint to settings icon
-   Made interval text semi-bold with primary color
-   Added explicit sizing and spacing
-   Added `maxLines = 1` to prevent wrapping

**Files Modified:**

-   `app/src/main/java/com/nibodhdaware/intentionality/ui/home/HomeScreen.kt`

**Changes:**

```kotlin
// Settings Icon
IconButton(
    onClick = { onConfigure(packageName) },
    modifier = Modifier.size(40.dp)  // Larger clickable area
) {
    Icon(
        imageVector = Icons.Filled.Settings,
        contentDescription = "Configure",
        tint = MaterialTheme.colorScheme.primary,  // Blue color
        modifier = Modifier.size(24.dp)  // Larger icon
    )
}

// Interval Text
Text(
    text = "Every ${monitoredApp!!.intervalMinutes} min",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.primary,  // Blue color
    fontWeight = FontWeight.SemiBold,  // Bold
    maxLines = 1
)
```

### 3. ✅ Firebase Data Sync with Exact Schema

**Problem:** Firebase submission needed to match the exact schema format:

```javascript
{
  dumbReason: "slightly_distracted",  // string
  reason: "Anime life lessons",       // string
  sessionDuration: 23.87,             // number
  timestamp: Timestamp,               // Firestore Timestamp
  url: "https://www.youtube.com/...", // string
  packageName: "com.app.package"      // string (additional)
}
```

**Solution:**

#### A. Updated Firebase Schema

**File:** `app/src/main/java/com/nibodhdaware/intentionality/firebase/FirebaseManager.kt`

**Changes:**

1. **Removed** human-readable timestamp string format
2. **Changed** to Firestore Timestamp type
3. **Removed** title, description, userAgent fields (old schema)
4. **Added** exact field names: dumbReason, reason, sessionDuration, timestamp, url
5. **Added** url parameter support
6. **Updated** getAppEntries to handle both Timestamp and String formats

```kotlin
suspend fun saveAppEntry(
    appName: String,
    packageName: String,
    reason: String,
    dumbReason: String,
    sessionDuration: Double,
    url: String = ""  // NEW: Optional URL parameter
): Result<Unit> {
    return try {
        val userId = getCurrentUser()?.uid ?: return Result.failure(Exception("User not signed in"))

        // Create Firestore Timestamp (native format)
        val timestamp = com.google.firebase.Timestamp.now()

        // Match the exact schema provided
        val entry = hashMapOf(
            "dumbReason" to dumbReason,           // e.g., "slightly_distracted"
            "reason" to reason,                   // e.g., "Anime life lessons"
            "sessionDuration" to sessionDuration, // e.g., 23.87 (number)
            "timestamp" to timestamp,             // Firestore Timestamp object
            "url" to if (url.isNotEmpty()) url else "app://$packageName",
            "packageName" to packageName          // Additional field
        )

        firestore.collection("users")
            .document(userId)
            .collection("activities")
            .add(entry)
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Timestamp Format:**

-   **Stored:** Firestore Timestamp object (native format)
-   **Example:** `October 5, 2025 at 10:37:23 AM UTC+5:30` (displayed in Firebase Console)
-   **Type:** `com.google.firebase.Timestamp`

#### B. Added URL Input Field

**File:** `app/src/main/java/com/nibodhdaware/intentionality/ui/prompt/IntentionOverlayView.kt`

**Changes:**

1. Added `url` state variable
2. Added URL input field (optional)
3. Updated Continue button to pass URL

```kotlin
var url by remember { mutableStateOf("") }

// URL Input Field (added after reason input)
OutlinedTextField(
    value = url,
    onValueChange = { url = it },
    modifier = Modifier.fillMaxWidth(),
    placeholder = { Text("URL (optional)", color = Color.White.copy(alpha = 0.4f)) },
    singleLine = true,
    // ... styling
)

// Continue button
Button(
    onClick = {
        val sessionDuration = (System.currentTimeMillis() - appOpenTime) / 1000.0

        scope.launch {
            FirebaseManager.saveAppEntry(
                appName = appName,
                packageName = packageName,
                reason = reason,
                dumbReason = selectedDistraction,
                sessionDuration = sessionDuration,
                url = url.ifBlank { "app://$packageName" }  // Use URL or default
            )
            onProceed(reason, 0)
        }
    }
)
```

#### C. Updated Read Logic

**File:** `app/src/main/java/com/nibodhdaware/intentionality/firebase/FirebaseManager.kt`

**Changes:**

-   Handle both Firestore Timestamp and String timestamp formats
-   Convert Timestamp to human-readable string for display
-   Support backward compatibility with old schema

```kotlin
suspend fun getAppEntries(limit: Int = 100): Result<List<AppEntry>> {
    // ...
    val entries = snapshot.documents.mapNotNull { doc ->
        try {
            // Handle both Firestore Timestamp and String formats
            val timestampObj = doc.get("timestamp")
            val timestampString = when (timestampObj) {
                is com.google.firebase.Timestamp -> {
                    // Convert to readable format
                    val date = timestampObj.toDate()
                    SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a z", Locale.ENGLISH)
                        .format(date)
                }
                is String -> timestampObj
                else -> ""
            }

            AppEntry(
                // ... field mappings with backward compatibility
                reason = doc.getString("reason") ?: doc.getString("description") ?: "",
                // ...
            )
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error parsing activity entry", e)
            null
        }
    }
    // ...
}
```

## Firebase Data Structure

### Collection Path

```
users/{userId}/activities/{documentId}
```

### Document Schema

```javascript
{
  dumbReason: "slightly_distracted",  // Distraction level (required)
  reason: "Anime life lessons",       // User's reason (required)
  sessionDuration: 23.87,             // Time in seconds (calculated)
  timestamp: Timestamp(1728115643),   // Firestore Timestamp (auto-generated)
  url: "https://www.youtube.com/...", // URL or app:// (optional)
  packageName: "com.x8bit.bitwarden"  // App package name (additional)
}
```

### Field Details

| Field             | Type      | Required | Example                 | Notes                     |
| ----------------- | --------- | -------- | ----------------------- | ------------------------- |
| `dumbReason`      | String    | ✅ Yes   | `"slightly_distracted"` | Distraction level key     |
| `reason`          | String    | ✅ Yes   | `"Anime life lessons"`  | User's intention/reason   |
| `sessionDuration` | Number    | ✅ Yes   | `23.87`                 | Seconds on overlay screen |
| `timestamp`       | Timestamp | ✅ Yes   | Firestore Timestamp     | Auto-generated            |
| `url`             | String    | ❌ No    | `"https://..."`         | URL or `app://{package}`  |
| `packageName`     | String    | ✅ Yes   | `"com.app.name"`        | Android package name      |

### Distraction Level Options

-   `productive` → 🎯 Productive
-   `slightly_distracted` → 😅 Slightly Distracted
-   `pretty_distracted` → 😬 Pretty Distracted
-   `very_distracted` → 😫 Very Distracted
-   `extremely_distracted` → 🤦 Extremely Distracted

## User Flow

1. **User Opens Monitored App**

    - Overlay appears based on interval/schedule
    - Timer starts counting session duration

2. **User Fills Form**

    - Enters reason (required): "Anime life lessons"
    - Optionally enters URL: "https://www.youtube.com/..."
    - Selects distraction level (required): "Slightly Distracted"

3. **User Clicks Continue**
    - Session duration calculated: `(currentTime - openTime) / 1000.0`
    - Data saved to Firebase:
        ```javascript
        {
          dumbReason: "slightly_distracted",
          reason: "Anime life lessons",
          sessionDuration: 23.87,
          timestamp: Timestamp.now(),
          url: "https://www.youtube.com/...",
          packageName: "com.youtube.android"
        }
        ```
    - App launches
    - Overlay closes

## Example Firebase Document

```javascript
// Document ID: auto-generated (e.g., "a1b2c3d4e5f6")
{
  dumbReason: "slightly_distracted",
  reason: "Anime life lessons",
  sessionDuration: 23.87,
  timestamp: October 31, 2025 at 12:24:00 PM UTC+5:30,
  url: "https://www.youtube.com/results?search_query=anime%20life%20lessons",
  packageName: "com.google.android.youtube"
}
```

## Build Status

✅ **BUILD SUCCESSFUL**

```
BUILD SUCCESSFUL in 1m 17s
39 actionable tasks: 7 executed, 32 up-to-date
```

Only deprecation warnings (cosmetic, not errors)

## Files Modified

1. ✅ `app/src/main/java/com/nibodhdaware/intentionality/ui/appconfig/AppConfigScreen.kt`

    - Fixed text wrapping
    - Better singular/plural handling

2. ✅ `app/src/main/java/com/nibodhdaware/intentionality/ui/home/HomeScreen.kt`

    - Made settings icon more visible
    - Improved interval text styling

3. ✅ `app/src/main/java/com/nibodhdaware/intentionality/firebase/FirebaseManager.kt`

    - Updated schema to exact format
    - Added Firestore Timestamp support
    - Added URL parameter
    - Added backward compatibility
    - Added Log import

4. ✅ `app/src/main/java/com/nibodhdaware/intentionality/ui/prompt/IntentionOverlayView.kt`
    - Added URL input field
    - Updated submit to include URL
    - Added url state variable

## Testing Checklist

### UI Testing

-   [ ] Open AppConfigScreen
-   [ ] Verify "Show overlay every 1 minute" (singular) displays correctly
-   [ ] Verify "Show overlay every 5 minutes" (plural) displays correctly
-   [ ] Check text doesn't wrap awkwardly
-   [ ] Navigate to HomeScreen
-   [ ] Verify settings icon is visible and blue
-   [ ] Verify "Every X min" text is visible and blue/bold
-   [ ] Click settings icon to open config

### Firebase Testing

-   [ ] Open monitored app
-   [ ] Fill in reason: "Test reason"
-   [ ] Fill in URL (optional): "https://example.com"
-   [ ] Select distraction level: "Slightly Distracted"
-   [ ] Click Continue
-   [ ] Open Firebase Console
-   [ ] Navigate to: `users/{userId}/activities`
-   [ ] Verify latest document has:
    -   ✅ `dumbReason`: "slightly_distracted" (string)
    -   ✅ `reason`: "Test reason" (string)
    -   ✅ `sessionDuration`: (number, e.g., 15.43)
    -   ✅ `timestamp`: (Firestore Timestamp)
    -   ✅ `url`: "https://example.com" (string)
    -   ✅ `packageName`: (string)

### Backward Compatibility

-   [ ] Verify old entries still display in app
-   [ ] Verify app handles documents without `url` field
-   [ ] Verify app handles old string timestamp format

## Notes

-   **Firestore Timestamp** is the native format and displays properly in Firebase Console
-   **URL field** is optional - defaults to `app://{packageName}` if not provided
-   **Schema is exact match** to the format you specified
-   **Backward compatibility** maintained for reading old entries
-   All changes are non-breaking and compile successfully

The implementation is complete and ready for testing! 🎉
