# Intentionality - Design Guidelines

> A comprehensive guide to maintain visual consistency across the Intentionality app

Last Updated: October 23, 2024

---

## 🎨 Color Palette

### Dark Theme (Primary)

```kotlin
// Core Colors
val BackgroundDark = Color(0xFF18344A)      // Deep blue background
val PrimaryDark = Color(0xFF295B7A)         // Medium blue (primary actions)
val AccentDark = Color(0xFF4A90A4)          // Light blue (accents, highlights)
val SurfaceDark = Color(0xFF1F3A4F)         // Elevated surfaces (cards, top bars)
val OnBackgroundDark = Color(0xFFE2E8F0)    // Text on background
val OnPrimaryDark = Color(0xFFFFFFFF)       // Text on primary color
val OnSurfaceDark = Color(0xFFE2E8F0)       // Text on surfaces
```

### Color Usage

| Element              | Color           | Usage                              |
| -------------------- | --------------- | ---------------------------------- |
| **App Background**   | `#18344A`       | Main screen background             |
| **Surfaces**         | `#1F3A4F`       | Cards, top bars, elevated elements |
| **Primary Actions**  | `#295B7A`       | Buttons, borders, primary icons    |
| **Accents**          | `#4A90A4`       | Highlights, graph elements         |
| **Text (Primary)**   | `#E2E8F0`       | Headings, body text                |
| **Text (Secondary)** | `#E2E8F0` @ 60% | Subtitles, hints                   |
| **Borders**          | Primary @ 50%   | Input fields, dividers             |

---

## 📝 Typography

### Text Styles

```kotlin
// Headings
headlineLarge      // App title, major headings
headlineMedium     // Section titles
headlineSmall      // Subsection titles

// Body
titleLarge         // Emphasized text
titleMedium        // Card titles, button text
titleSmall         // Labels

bodyLarge          // Primary content
bodyMedium         // Secondary content
bodySmall          // Captions, hints
```

### Font Weights

-   **Bold** (`FontWeight.Bold`): Major headings, CTAs
-   **SemiBold** (`FontWeight.SemiBold`): Section headers, emphasis
-   **Medium** (`FontWeight.Medium`): Button text, labels
-   **Normal** (default): Body text

---

## 🔲 Shapes & Corners

### Corner Radius Standards

```kotlin
// Component Corner Radii
RoundedCornerShape(12.dp)  // ✅ Search bars, text fields (rounded rectangle)
RoundedCornerShape(16.dp)  // ✅ Cards, containers
RoundedCornerShape(8.dp)   // ✅ Small buttons, chips
CircleShape                // ✅ Profile pictures, FABs, icon buttons
```

### Rules

-   ❌ **AVOID** fully rounded (pill-shaped) search bars - use `12.dp` rounded rectangles instead
-   ✅ **USE** `CircleShape` only for circular elements (avatars, icon buttons)
-   ✅ **USE** `16.dp` for larger cards and containers
-   ✅ **USE** `12.dp` for input fields and search bars

---

## 🧩 Component Styles

### Search Bar

```kotlin
OutlinedTextField(
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
    shape = RoundedCornerShape(12.dp),  // Rounded rectangle, NOT fully rounded
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    ),
    leadingIcon = { Icon(Icons.Default.Search, ...) }
)
```

**Design Rules:**

-   ✅ Full width in top bar
-   ✅ Height: `56.dp`
-   ✅ Corner radius: `12.dp` (rounded rectangle)
-   ✅ Leading search icon
-   ✅ Placeholder text: "Search apps..."

### Buttons

#### Primary Button (CTA)

```kotlin
Button(
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
    ),
    shape = RoundedCornerShape(12.dp)
) {
    Text(
        text = "Button Text",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}
```

**Specs:**

-   Height: `56.dp`
-   Corner radius: `12.dp`
-   Background: Primary color
-   Text: Bold, titleMedium
-   Full width for bottom CTAs

#### Outlined Button (Secondary)

```kotlin
OutlinedButton(
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
    colors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
) {
    Text("Secondary Action")
}
```

### Cards

```kotlin
Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.medium,  // 16.dp by default
    shadowElevation = 2.dp
) {
    // Card content
}
```

**Specs:**

-   Corner radius: `16.dp`
-   Elevation: `2.dp` (subtle)
-   Background: Surface color
-   Padding: `12.dp` inside

### Profile Picture

```kotlin
Box(
    modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer)
) {
    AsyncImage(
        model = photoUrl,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}
```

**Specs:**

-   Size: `48.dp` (top bar), `40.dp` (compact)
-   Shape: Circle
-   Border: `2.dp` primary color
-   Fallback: Person icon, 28.dp

### Text Input Fields

```kotlin
OutlinedTextField(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    )
)
```

**Specs:**

-   Corner radius: `12.dp`
-   Border: Primary color (50% when unfocused)
-   Min height: `56.dp` for single line

---

## 📏 Spacing & Layout

### Standard Spacing Scale

```kotlin
4.dp   // Extra tight (dividers, small gaps)
8.dp   // Tight (between related elements)
12.dp  // Default (between components)
16.dp  // Comfortable (screen padding, section spacing)
24.dp  // Loose (major sections)
32.dp  // Extra loose (between distinct sections)
48.dp  // Very loose (top-level spacing)
```

### Screen Padding

```kotlin
// Standard screen padding
Modifier.padding(16.dp)

// Top bar padding
Modifier.padding(16.dp)

// Bottom sheet/modal padding
Modifier.padding(24.dp)
```

### Component Spacing

```kotlin
Spacer(modifier = Modifier.height(8.dp))   // Between related items
Spacer(modifier = Modifier.height(12.dp))  // Between components
Spacer(modifier = Modifier.height(16.dp))  // Between sections
Spacer(modifier = Modifier.width(12.dp))   // Horizontal spacing
```

---

## ✨ Animations

### Scroll-Based Animations

```kotlin
// Smooth fade-out as user scrolls
val alpha by animateFloatAsState(
    targetValue = calculateAlpha(scrollOffset),
    animationSpec = tween(durationMillis = 200),
    label = "fadeAnimation"
)
```

**Specs:**

-   Duration: `200ms` (quick, responsive)
-   Easing: Default (ease-in-out)
-   Use for: Graph area fade, content transitions

### Loading States

```kotlin
CircularProgressIndicator(
    modifier = Modifier.size(24.dp),
    color = MaterialTheme.colorScheme.onPrimary
)
```

**Specs:**

-   Size: `24.dp` (in buttons), `48.dp` (standalone)
-   Color: OnPrimary (in buttons), Primary (standalone)

---

## 🖼️ Icons

### Icon Sizes

```kotlin
Icons.Default.Icon

// Sizes
Modifier.size(16.dp)  // Small (inline with text)
Modifier.size(24.dp)  // Default (buttons, list items)
Modifier.size(28.dp)  // Medium (profile fallback)
Modifier.size(48.dp)  // Large (empty states)
```

### Icon Colors

```kotlin
tint = MaterialTheme.colorScheme.primary      // Primary actions
tint = MaterialTheme.colorScheme.onSurface    // Neutral
tint = MaterialTheme.colorScheme.error        // Destructive
```

---

## 📱 Screen-Specific Designs

### Login Screen

**Layout:**

-   Centered vertically
-   Logo/title at top
-   "Sign in with Google" button (primary)
-   "Skip for now" text button below
-   Padding: `24.dp`

**Elements:**

```kotlin
Text(
    text = "Intentionality",
    style = MaterialTheme.typography.headlineLarge,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.primary
)

Text(
    text = "Be mindful of your app usage",
    style = MaterialTheme.typography.bodyLarge
)

Button(
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
) {
    Text("Sign in with Google")
}
```

### Dashboard (App List Screen)

**Layout:**

```
┌──────────────────────────────────┐
│  Top Bar (Surface, elevation 4dp)│
│  ┌────────────────────────────┐  │
│  │ [🔍] Search apps...        │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
│                                  │
│  [Graph Area - fades on scroll]  │
│                                  │
├──────────────────────────────────┤
│  Select Apps to Monitor          │
│  • Monitoring X apps             │
├──────────────────────────────────┤
│  [App 1]                    [✓]  │
│  [App 2]                    [ ]  │
│  ...                             │
└──────────────────────────────────┘
┌──────────────────────────────────┐
│  [Start Monitoring]              │
└──────────────────────────────────┘
```

**Specs:**

-   Top bar: Surface color, 4dp elevation
-   Search: Full width, 12dp corners
-   Graph: Fades to 0 when scrolling
-   List items: 12dp vertical padding
-   Bottom button: Fixed, full width

### App List Items

```kotlin
Surface(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
    color = if (isChecked)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface,
    shape = MaterialTheme.shapes.medium,
    shadowElevation = 2.dp
) {
    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon (48.dp)
        // App name + package (weighted)
        // Checkbox
    }
}
```

**Specs:**

-   Height: Auto (min 72.dp)
-   Icon size: `48.dp`
-   Padding: `12.dp`
-   Selected state: Primary container @ 30% opacity
-   Elevation: `2.dp`

### Intention Prompt (Full-Screen)

**Layout:**

```
┌──────────────────────────────────┐
│  Why are you opening             │
│  [App Name]?                     │
│                                  │
│  ┌────────────────────────────┐ │
│  │ Your reason...             │ │
│  │                            │ │
│  └────────────────────────────┘ │
│                                  │
│  [Intentionality Rating ▼]       │
│                                  │
│  ┌────────────────────────────┐ │
│  │ Continue to App            │ │
│  └────────────────────────────┘ │
│                                  │
│  ┌────────────────────────────┐ │
│  │ Go Back to Home            │ │ (Outlined)
│  └────────────────────────────┘ │
└──────────────────────────────────┘
```

**Specs:**

-   Centered content
-   Title: headlineSmall + headlineMedium (app name bold)
-   Text field: 120dp min height
-   Dropdown: Full width
-   Buttons: 56dp height, 12dp spacing
-   Padding: `24.dp`

---

## 🎯 Design Principles

### 1. Consistency

-   Use the same corner radius for similar components
-   Maintain spacing scale across all screens
-   Stick to defined color palette

### 2. Clarity

-   High contrast text (E2E8F0 on 18344A)
-   Clear visual hierarchy
-   Obvious interactive elements

### 3. Feedback

-   Button states (normal, pressed, disabled)
-   Loading indicators for async actions
-   Smooth transitions (200ms)

### 4. Accessibility

-   Touch targets: Minimum 48dp
-   Text size: Minimum 14sp
-   Color contrast: WCAG AA compliant

### 5. Material Design 3

-   Follow Material You guidelines
-   Use dynamic colors when appropriate
-   Respect system preferences

---

## 🚫 Design Don'ts

### ❌ Avoid These

1. **Fully Rounded Search Bars**

    - ❌ `RoundedCornerShape(28.dp)` for search
    - ✅ Use `RoundedCornerShape(12.dp)` instead

2. **Inconsistent Corner Radii**

    - ❌ Random values like 14.dp, 20.dp
    - ✅ Stick to 8, 12, 16, or Circle

3. **Too Many Elevations**

    - ❌ Cards at 8dp, 12dp, 16dp
    - ✅ Use 2dp (subtle) or 4dp (prominent)

4. **Custom Colors**

    - ❌ Adding random hex colors
    - ✅ Use defined theme colors

5. **Inconsistent Spacing**

    - ❌ 10dp, 14dp, 18dp, 22dp
    - ✅ Use 8, 12, 16, 24, 32

6. **Mixing Icon Sizes**
    - ❌ 20dp, 26dp, 30dp
    - ✅ Use 16, 24, 28, 48

---

## 📦 Component Library Quick Reference

### Common Patterns

```kotlin
// Primary Button (Bottom CTA)
Button(
    modifier = Modifier.fillMaxWidth().height(56.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Primary),
    shape = RoundedCornerShape(12.dp)
) { Text("Action", fontWeight = FontWeight.Bold) }

// Card with Content
Surface(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    color = Surface,
    shape = RoundedCornerShape(16.dp),
    shadowElevation = 2.dp
) { /* Content */ }

// Search/Input Field
OutlinedTextField(
    modifier = Modifier.fillMaxWidth().height(56.dp),
    shape = RoundedCornerShape(12.dp),
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Primary,
        unfocusedBorderColor = Primary.copy(alpha = 0.5f)
    )
)

// Profile Picture
Box(
    modifier = Modifier.size(48.dp).clip(CircleShape)
        .border(2.dp, Primary, CircleShape)
) { AsyncImage(...) }

// Section Header
Text(
    text = "Section Title",
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
    color = OnBackground
)
```

---

## 🔄 Version History

### v1.0 (October 23, 2024)

-   Initial design system
-   Dark theme with custom colors
-   Search bar: Full width, 12dp corners
-   Profile photo: Circular, 48dp
-   Scroll animations: 200ms fade
-   Component library defined

---

## 📝 Notes for Future Development

### When Adding New Components:

1. **Check this guide first** for existing patterns
2. **Use defined colors** from theme
3. **Follow spacing scale** (8, 12, 16, 24, 32)
4. **Match corner radii** to component type
5. **Test in dark theme** (our default)
6. **Add to this document** if it's a new pattern

### When Modifying Designs:

1. Update this document
2. Ensure consistency across all screens
3. Test existing screens for conflicts
4. Document the change in version history

---

**Remember:** Consistency > Creativity

It's better to reuse existing patterns than to create new ones. This keeps the app feeling cohesive and professional.
