# Intentionality Design System (Single Source of Truth)

> **Core Philosophy:** Mindfulness, Calm, and Intentionality. The interface should act as a "Zen space" that encourages reflection rather than friction.

---

## 🎨 1. Color Palette (The Teal Aesthetic)
The palette is derived from the primary app icon to ensure 100% cohesiveness across **Mobile, Web, and Chrome Extension**.

### Core Colors
| Role | Hex | Usage |
| :--- | :--- | :--- |
| **Primary (Soft Teal)** | `#80CBC4` | Primary actions, highlights, branding. |
| **Deep Teal** | `#004D46` | Containers, secondary accents, text headers. |
| **Mist Teal** | `#80CBC4` @ 10% | Subtle backgrounds, glassmorphism, card overlays. |
| **Background (Dark)** | `#121212` | True dark background for focus and calm. |
| **Surface (Dark)** | `#1E1E1E` | Elevated surfaces (cards, top bars, extensions). |
| **Text (Primary)** | `#E3E3E3` | Main readability. |
| **Text (Secondary)** | `#CACACA` | Muted hints, package names, secondary labels. |

---

## 📝 2. Typography
A unified typography system ensures the brand feels the same on a 6-inch screen or a 27-inch monitor.

*   **Primary Font:** `Inter` or `Plus Jakarta Sans` (Neutral, modern, and highly legible).
*   **Headline Styling:** Use slightly wider letter spacing for headers to create a "breathable" feel.
*   **Scale:**
    *   **Headline Large:** 32sp/px (Bold) - Screen titles.
    *   **Headline Medium:** 24sp/px (SemiBold) - Section headers.
    *   **Body Large:** 16sp/px (Normal) - Main content.
    *   **Label Small:** 12sp/px (Medium) - Captions, tags.

---

## 🔲 3. Shapes & Geometry
Avoid sharp corners to maintain the "calm" atmosphere.

*   **Standard Corners:** `16.dp/px` - All cards and main containers.
*   **Input/Button Corners:** `12.dp/px` - Search bars, text fields, and primary buttons.
*   **Circle:** `CircleShape` - Only for profile pictures, avatars, and FABs.
*   **Borders:** Use subtle teal-tinted borders (`Deep Teal` @ 20% alpha) instead of harsh grays.

---

## ✨ 4. User Interaction & "Soft Touches"
These principles separate "Intentionality" from a generic utility app.

### A. The Zen Ritual (Intention Prompt)
The intention check is a ceremony, not just a form.
*   **Full Screen Zenith:** Use a full-screen layout with a soft, pulsing Teal gradient background (`Soft Teal` to `Background`).
*   **Breathing Pulse:** The main emoji and text field should have a subtle, slow scale animation (1.0x to 1.05x) to encourage a breath before typing.
*   **Haptic Ticks:** Provide a distinct physical "tick" when selecting distraction ratings.

### B. Motion & Feedback
*   **Staggered Entrance:** Lists should slide in vertically and fade in with a staggered delay (50ms per item) to feel organic.
*   **Teal Glow:** Instead of heavy drop shadows, use a soft teal outer glow for active elements or primary buttons.
*   **Scroll-Based Fade:** On the Dashboard, the usage graph should smoothly fade and shrink as the user scrolls down, giving more space to the app list.

---

## 📱 5. Cross-Platform Cohesion

### Mobile (Android)
*   **Touch Targets:** All primary buttons must be exactly `56.dp` in height.
*   **Navigation:** Use a clean top bar with a search field and a settings icon for easy configuration.

### Chrome Extension
*   **Mini-Dashboard:** The extension popup should mirror the top section of the Android app (Soft Teal header).
*   **Consistency:** Use the same `12.dp` rounded search bar and `16.dp` cards found in the mobile app.

### Website
*   **Mist Backgrounds:** Use `Mist Teal` backgrounds for section breaks to keep the site feeling light and consistent.
*   **Spaciousness:** Double the vertical padding compared to standard websites to emphasize "Mindfulness" and room to breathe.

---

## 🎯 6. Design Principles (The "Don'ts")
*   ❌ **No Purple:** Avoid purple or harsh blues. Stick to the Teal spectrum.
*   ❌ **No Pill Shapes:** Use the `12.dp` rounded rectangle for search bars, NOT the fully rounded pill shape.
*   ❌ **No Hard Shadows:** Use high-blur, low-opacity shadows with a hint of Teal.
*   ❌ **No Clutter:** If an element doesn't help the user be intentional, remove it.

---

## 🔄 7. Versioning
*   **v1.0 (January 2026):** Unified "Soft Teal" system established across Mobile, Web, and Extension.
