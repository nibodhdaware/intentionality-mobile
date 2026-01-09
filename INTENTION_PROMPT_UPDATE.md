# Intention Prompt Update

## ✅ Changes Implemented

The full-screen intention prompt has been updated with your new design!

---

## 🎨 New Design

### Title

```
Why do you want to open
[App Name]?
```

**Changed from:** "Why are you opening"  
**Changed to:** "Why do you want to open"

### Dropdown Options

**New Label:** "How dumb is this reason? 🤔"

**Options (1-5):**

1. **Actually Productive! 🎯** (rating: 1)
2. **Slightly Distracted 😅** (rating: 2)
3. **Pretty Distracted 😬** (rating: 3)
4. **Very Distracted 😫** (rating: 4)
5. **Extremely Distracted 🤦‍♂️** (rating: 5)

**Replaced:** The old 1-5 intentionality scale

-   ❌ "1 - Very intentional"
-   ❌ "2 - Somewhat intentional"
-   ❌ "3 - Not intentional"
-   ❌ "4 - Mindless"
-   ❌ "5 - Regretful"

### Buttons

#### Proceed Button (Primary)

-   **Text:** "Proceed"
-   **Behavior:**
    -   Only enabled when BOTH fields are filled
    -   Saves data to Supabase database
    -   Launches the app the user wanted to open
    -   Closes the prompt
-   **Style:** Primary blue background, bold text

#### Go Back Button (Secondary)

-   **Text:** "Go Back"
-   **Behavior:**
    -   Returns to home screen
    -   Does NOT save data
    -   Does NOT open the app
-   **Style:** Outlined button, primary color text

---

## 📱 Full Layout

```
┌────────────────────────────────────────┐
│                                        │
│    Why do you want to open             │
│    [App Name]?                         │
│                                        │
│    ┌────────────────────────────────┐ │
│    │ Your reason                    │ │
│    │                                │ │
│    │ [Text input area...]           │ │
│    │                                │ │
│    └────────────────────────────────┘ │
│                                        │
│    ┌────────────────────────────────┐ │
│    │ How dumb is this reason? 🤔    │ │
│    │ Actually Productive! 🎯      ▼ │ │
│    └────────────────────────────────┘ │
│                                        │
│    ┌────────────────────────────────┐ │
│    │         Proceed                │ │ (Primary)
│    └────────────────────────────────┘ │
│                                        │
│    ┌────────────────────────────────┐ │
│    │         Go Back                │ │ (Outlined)
│    └────────────────────────────────┘ │
│                                        │
└────────────────────────────────────────┘
```

---

## 🔄 User Flow

### When a monitored app is opened:

1. **Prompt appears** (full-screen)
2. **User fills in:**

    - Why they want to open the app (text)
    - How productive/distracted they are (dropdown)

3. **User clicks "Proceed":**

    - ✅ Data saved to Supabase `app_entries` table
    - ✅ App launches
    - ✅ Prompt closes

4. **OR User clicks "Go Back":**
    - ❌ No data saved
    - ❌ App doesn't launch
    - ✅ Returns to home screen

---

## 💾 Database Storage

Data saved to Supabase `app_entries` table:

```sql
{
  "app_name": "Instagram",
  "package_name": "com.instagram.android",
  "reason": "Check messages from friends",
  "rating": 2,  -- Maps to "Slightly Distracted 😅"
  "timestamp": "2024-10-23T14:30:00.000Z",
  "user_id": "firebase-user-uid"
}
```

### Rating Values Mapping

| Option                  | Value |
| ----------------------- | ----- |
| Actually Productive! 🎯 | 1     |
| Slightly Distracted 😅  | 2     |
| Pretty Distracted 😬    | 3     |
| Very Distracted 😫      | 4     |
| Extremely Distracted 🤦‍♂️ | 5     |

**Note:** The database still uses integer values (1-5), but the UI now shows the new labels with emojis.

---

## ✨ Features

### Validation

-   **Both fields required:** "Proceed" button is disabled until both the reason text and dropdown option are selected
-   **Visual feedback:** Button appears enabled/disabled based on validation

### Smart Button Behavior

-   **"Proceed"** → Saves data + Opens app
-   **"Go Back"** → Goes to home screen (no app launch)

### Emojis

All dropdown options include relevant emojis for visual appeal:

-   🎯 (target) for productive
-   😅 😬 😫 🤦‍♂️ for increasing levels of distraction

---

## 🎨 Design Specs

Following the design guidelines:

### Text Field

-   **Height:** Auto (min 3 lines)
-   **Corner radius:** `12.dp`
-   **Border:** Primary color (50% when unfocused)
-   **Placeholder:** "Type your reason..."

### Dropdown

-   **Height:** `56.dp`
-   **Corner radius:** `12.dp`
-   **Border:** Primary color (50% when unfocused)
-   **Label:** "How dumb is this reason? 🤔"

### Buttons

-   **Height:** `56.dp`
-   **Corner radius:** `12.dp`
-   **Spacing:** `12.dp` between buttons
-   **Full width**

### Spacing

-   Top/Bottom padding: `24.dp`
-   Between components: `16.dp` / `32.dp`

---

## 🧪 Testing

### Test Scenarios

1. **Open a monitored app**

    - Prompt should appear immediately
    - Both fields should be empty

2. **Try clicking "Proceed" without filling fields**

    - Button should be disabled
    - Nothing should happen

3. **Fill only the text field**

    - Button should still be disabled

4. **Fill only the dropdown**

    - Button should still be disabled

5. **Fill both fields**

    - Button should become enabled
    - Click "Proceed" → App launches, data saves

6. **Click "Go Back"**
    - Should return to home screen
    - App should NOT launch

---

## 🚀 Build Status

✅ **Build Successful** (1m 12s)  
✅ **Ready to Install**

### Install Command

```bash
./gradlew installDebug
```

---

## 📝 Code Changes Summary

### Files Modified

1. **`IntentionPromptActivity.kt`**
    - Changed title text
    - Updated dropdown options (5 new options with emojis)
    - Renamed button from "Continue" to "Proceed"
    - Renamed "Go Back to Home" to "Go Back"
    - Added app launch after data save
    - Updated function names (`onSubmit` → `onProceed`)

### Key Changes

```kotlin
// Old options
"1 - Very intentional"
"2 - Somewhat intentional"
...

// New options
"Actually Productive! 🎯"
"Slightly Distracted 😅"
"Pretty Distracted 😬"
"Very Distracted 😫"
"Extremely Distracted 🤦‍♂️"

// Old button
Text("Continue")

// New button
Text("Proceed")
```

---

## 💡 Future Enhancements

Potential improvements for later:

-   [ ] Add a "Skip" option that saves with a default reason
-   [ ] Show a subtle animation when the prompt appears
-   [ ] Add haptic feedback on button press
-   [ ] Display previous reasons for the same app
-   [ ] Add statistics (e.g., "You opened this 5 times today")
-   [ ] Customize emojis per user preference

---

**The new intention prompt is ready to test! 🎉**

Install the app and try opening a monitored app to see the updated design in action.





