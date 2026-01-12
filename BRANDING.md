# Prometheus Branding & Design System

## Brand Identity

**App Name:** Prometheus
**Tagline:** AI-Powered Fitness & Nutrition Coaching
**Design Philosophy:** Premium Coaching Software with Glassmorphism aesthetics

---

## Color Palette

### Primary Brand Colors

| Name | Hex | RGB | Usage |
|------|-----|-----|-------|
| **Prometheus Orange** | `#E67E22` | rgb(230, 126, 34) | Primary brand color, CTAs, highlights |
| **Prometheus Orange Bright** | `#F39C12` | rgb(243, 156, 18) | Hover/Active states |
| **Prometheus Orange Dark** | `#D35400` | rgb(211, 84, 0) | Pressed states, contrast |
| **Prometheus Orange Glow** | `#E67E22` | rgb(230, 126, 34) | Glow effects |

### Dark Mode Colors

#### Backgrounds & Surfaces
| Name | Hex | Usage |
|------|-----|-------|
| **Background** | `#141414` | Main app background |
| **Surface** | `#1C1C1C` | Cards, elevated surfaces |
| **Surface Variant** | `#262626` | Secondary surfaces |

#### Text Colors
| Name | Hex | Usage |
|------|-----|-------|
| **Text Primary** | `#FAFAFA` | Primary text, headings |
| **Text Secondary** | `#999999` | Secondary text, labels |
| **Text Tertiary** | `#666666` | Muted text, placeholders |
| **Unselected** | `#666666` | Inactive states |

#### Glass Effects
| Name | Hex | Usage |
|------|-----|-------|
| **Glass Base** | `#333333` | Glass background |
| **Glass Border** | `#FFFFFF` (10% alpha) | Glass borders |
| **Glass Hover** | `#404040` | Hover states |

### Light Mode Colors

#### Backgrounds & Surfaces
| Name | Hex | Usage |
|------|-----|-------|
| **Background** | `#FAFAFA` | Main app background |
| **Surface** | `#FFFFFF` | Cards, elevated surfaces |
| **Surface Variant** | `#F5F5F5` | Secondary surfaces |

#### Text Colors
| Name | Hex | Usage |
|------|-----|-------|
| **Text Primary** | `#1A1A1A` | Primary text |
| **Text Secondary** | `#4A4A4A` | Secondary text |
| **Text Gray** | `#666666` | Muted text |
| **Unselected** | `#BBBBBB` | Inactive states |

### Semantic Colors

| Name | Hex | Usage |
|------|-----|-------|
| **Success** | `#4CAF50` | Positive feedback, completed |
| **Error** | `#E53935` | Errors, warnings |
| **Warning** | `#FFC107` | Caution states |
| **Info** | `#2196F3` | Informational |

### Nutrition Macro Colors

| Macro | Hex | Visual |
|-------|-----|--------|
| **Calories** | `#F97316` | Orange |
| **Protein** | `#3B82F6` | Blue |
| **Carbs** | `#10B981` | Green |
| **Fat** | `#FBBF24` | Yellow |

### Border Colors

| Mode | Hex |
|------|-----|
| **Dark** | `#333333` |
| **Light** | `#E0E0E0` |

---

## Typography

### Font Families

#### Space Grotesk (Headings)
- **Usage:** Display text, headings, titles
- **Style:** Modern, geometric sans-serif
- **Weights:** Variable (300-700)
- **File:** `spacegrotesk_variable.ttf`

#### Poppins (Body)
- **Usage:** Body text, labels, buttons
- **Style:** Clean, friendly geometric sans-serif
- **Weights:** Regular (400), Medium (500), SemiBold (600), Bold (700)
- **Files:** `poppins_regular.ttf`, `poppins_medium.ttf`, `poppins_semibold.ttf`, `poppins_bold.ttf`

#### Inter (Legacy/Fallback)
- **Usage:** Backward compatibility
- **Weights:** Regular, Medium, SemiBold, Bold

### Type Scale

| Style | Font | Weight | Size | Line Height |
|-------|------|--------|------|-------------|
| Display Large | Space Grotesk | Bold | 36sp | 44sp |
| Display Medium | Space Grotesk | Bold | 32sp | 40sp |
| Display Small | Space Grotesk | SemiBold | 28sp | 36sp |
| Headline Large | Space Grotesk | SemiBold | 24sp | 32sp |
| Headline Medium | Space Grotesk | SemiBold | 20sp | 28sp |
| Headline Small | Space Grotesk | Medium | 18sp | 26sp |
| Title Large | Poppins | SemiBold | 20sp | 28sp |
| Title Medium | Poppins | Medium | 16sp | 24sp |
| Title Small | Poppins | Medium | 14sp | 20sp |
| Body Large | Poppins | Regular | 16sp | 24sp |
| Body Medium | Poppins | Regular | 14sp | 20sp |
| Body Small | Poppins | Regular | 12sp | 16sp |
| Label Large | Poppins | Medium | 14sp | 20sp |
| Label Medium | Poppins | Medium | 12sp | 16sp |
| Label Small | Poppins | Medium | 10sp | 14sp |

---

## Shape System

| Size | Radius | Usage |
|------|--------|-------|
| Extra Small | 8dp | Chips, tags, small elements |
| Small | 12dp | Buttons, small cards |
| Medium | 16dp | Standard cards, inputs |
| Large | 24dp | Large cards, modals |
| Extra Large | 28dp | Bottom sheets, full-screen |

**Base Radius:** 24dp (1.5rem) - Coaching Software standard

---

## Effects & Modifiers

### Glow Effects

```kotlin
// Subtle Glow
prometheusGlowSubtle()
// Radius: 20dp, Alpha: 0.2

// Standard Glow
prometheusGlow()
// Radius: 30dp, Alpha: 0.3

// Intense Glow
prometheusGlowIntense()
// Radius: 40dp, Alpha: 0.4
```

### Glassmorphism Effects

#### Standard Glass
```kotlin
glassBackground()
```
- Frosted glass with vertical gradient
- White border at 10-18% opacity
- Background: 85-95% alpha

#### Premium Glass
```kotlin
glassPremium()
```
- Enhanced glass with inner glow
- Radial gradient overlay
- Premium surface treatment

#### Glass with Orange Accent
```kotlin
glassCardAccent()
```
- Orange border accent (15% alpha)
- Standard glass background
- Used for featured cards

#### Glass with Glow
```kotlin
glassWithOrangeGlow()
```
- Orange glow effect (25dp radius, 25% alpha)
- Combines glass + glow

#### Elevated Glass
```kotlin
glassElevated()
```
- Shadow effect for depth
- Higher elevation appearance

### Chat-Specific Glass

```kotlin
// User messages - Orange tint
glassUserMessage()

// AI/Coach messages - Dark frosted
glassAiMessage()

// Input area - Premium panel
glassInputArea()
```

---

## Logo & Icons

### App Icon
- **Type:** Adaptive Icon (Android 8.0+)
- **Background:** White (`#FFFFFF`)
- **Foreground:** Orange flame (`#FF6600`)
- **Shape:** Adaptive (circle, squircle, etc.)

### Flame Logo
- **File:** `logo_flame.png`
- **Size:** 500x500px (source)
- **Color:** Prometheus Orange
- **Style:** Two-flame design, bold and dynamic
- **Available densities:** mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi

### Background Image
- **File:** `gradient_bg_dark.jpg`
- **Size:** 1080x1920px
- **Style:** Dark gradient with subtle orange tint at top
- **Usage:** Dark mode app background

---

## Design Tokens

### Spacing
| Token | Value |
|-------|-------|
| xs | 4dp |
| sm | 8dp |
| md | 16dp |
| lg | 24dp |
| xl | 32dp |

### Elevation
| Level | Shadow |
|-------|--------|
| Low | 2dp |
| Medium | 4dp |
| High | 8dp |

### Animation
| Type | Duration |
|------|----------|
| Fast | 150ms |
| Normal | 300ms |
| Slow | 500ms |

---

## UI Components Style Guide

### Buttons
- **Primary:** Orange background, white text, 12dp radius
- **Secondary:** Glass background, orange text
- **Ghost:** Transparent, orange text

### Cards
- **Standard:** Glass background, 16dp radius
- **Featured:** Glass + orange accent border
- **Elevated:** Glass elevated with shadow

### Input Fields
- **Background:** Glass or surface variant
- **Border:** 1dp, border color
- **Focus:** Orange border accent
- **Radius:** 12dp

### Navigation
- **Bottom Nav:** Glass background
- **Active:** Orange icon + label
- **Inactive:** Gray icon + label

---

## File Structure

```
src/main/
├── java/.../ui/theme/
│   ├── Color.kt          # All color definitions
│   ├── Type.kt           # Typography definitions
│   ├── Shape.kt          # Shape definitions
│   ├── Theme.kt          # Theme configuration
│   └── Modifiers.kt      # UI effect modifiers
├── res/
│   ├── font/
│   │   ├── poppins_*.ttf
│   │   ├── spacegrotesk_variable.ttf
│   │   └── inter_*.ttf
│   ├── drawable/
│   │   ├── gradient_bg_dark.jpg
│   │   └── logo_flame.png
│   ├── mipmap-*/
│   │   ├── ic_launcher.webp
│   │   └── ic_launcher_round.webp
│   └── values/
│       ├── colors.xml
│       └── themes.xml
```

---

## Implementation Notes

### Theme Usage
```kotlin
@Composable
fun MyComponent() {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes

    // Use theme values
    Text(
        text = "Hello",
        style = typography.headlineMedium,
        color = colors.onBackground
    )
}
```

### Applying Modifiers
```kotlin
Box(
    modifier = Modifier
        .glassBackground()
        .prometheusGlow()
)
```

### Theme Switching
- Dark mode: Default enabled
- Stored in DataStore preferences
- System-wide theme support

---

## Brand Voice

- **Tone:** Professional, motivating, supportive
- **Language:** Clear, concise, action-oriented
- **Personality:** Expert coach, not a drill sergeant

---

*Last Updated: January 2025*
*Version: 1.0*
