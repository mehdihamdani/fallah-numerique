---
name: Earth & Oasis AgriTech
colors:
  surface: '#f3fced'
  surface-dim: '#d4ddcf'
  surface-bright: '#f3fced'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#edf6e8'
  surface-container: '#e8f1e2'
  surface-container-high: '#e2ebdc'
  surface-container-highest: '#dce5d7'
  on-surface: '#161e15'
  on-surface-variant: '#3f493e'
  inverse-surface: '#2a3329'
  inverse-on-surface: '#eaf4e5'
  outline: '#6f7a6d'
  outline-variant: '#bfcaba'
  surface-tint: '#006e26'
  primary: '#006422'
  on-primary: '#ffffff'
  primary-container: '#1e7e34'
  on-primary-container: '#c9ffc6'
  inverse-primary: '#7edb85'
  secondary: '#ac3311'
  on-secondary: '#ffffff'
  secondary-container: '#ff6e48'
  on-secondary-container: '#651400'
  tertiary: '#794b00'
  on-tertiary: '#ffffff'
  tertiary-container: '#9a6100'
  on-tertiary-container: '#ffeedf'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#99f89e'
  primary-fixed-dim: '#7edb85'
  on-primary-fixed: '#002106'
  on-primary-fixed-variant: '#00531b'
  secondary-fixed: '#ffdbd1'
  secondary-fixed-dim: '#ffb4a1'
  on-secondary-fixed: '#3c0800'
  on-secondary-fixed-variant: '#881f00'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#f3fced'
  on-background: '#161e15'
  surface-variant: '#dce5d7'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.03em
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 34px
    fontWeight: '800'
    lineHeight: 42px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 13px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.25rem
  gutter-mobile: 0.75rem
  margin: 2rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

### Brand Personality
This design system bridges ancestral Algerian farming heritage with cutting-edge digital intelligence. It embodies resilience, fertility, grounded authenticity, and technological optimism. It serves agricultural engineers, farm managers, cooperative leads, and field producers across diverse Algerian biomes—from the fertile Mitidja plains to the expansive date-palm oases of Biskra and El Oued. The visual atmosphere balances agricultural earthiness with precision data interfaces, making complex telemetry (soil moisture, IoT cold chain, livestock telemetry, NDVI satellite scanning) accessible and empowering.

### Design Movement
The visual strategy unites **Modern Agri-Corporate Utility** with **Tactile Biophilic Precision**. Rather than generic enterprise SaaS, the interface leverages structured hexagonal grid logic derived directly from honeycomb and cellular agricultural systems, warm earthen foundational neutrals, high-contrast semantic indicators, and tactile metric modules.

## Colors

### Palette Philosophy
The palette draws life directly from the Algerian agricultural landscape and the brand mark:
- **Primary Green (`#1E7E34`) & Sprout Accent (`#38B000`)**: Symbolize crop vigor, photosynthesis, and digital operational efficiency. Used for key actions, positive statuses, and high-priority navigations.
- **Terracotta Clay (`#D9532F`)**: Grounded earthen red reflecting the fertile clay soils, Saharan adobe brickwork, and artificial intelligence alert telemetry.
- **Oasis Amber (`#F59E0B`)**: Represents Saharan sun, honey harvests, pollination cycles, and active warnings.
- **Cool Cold-Storage Slate (`#2A6F97`) & Deep Charcoal (`#1C241B`)**: Provide technical balance for refrigeration monitoring, machinery management, and sharp typographic readability.

### Surface Tones
- **Canvas Base**: `#FAFAF7` (Warm Agricultural Chalk)
- **Card & Tile Surface**: `#FFFFFF` (Pure Solar White)
- **Muted Section / Container**: `#F2F5F0` (Sprout Tint Neutral)
- **Subtle Border Line**: `#E1E7DE` (Tilled Soil Wash)

## Typography

### Structural Decisions
`Plus Jakarta Sans` provides a contemporary geometric architecture with friendly, rounded counters that mirror the organic nature of agronomy while maintaining tabular clarity for field readings.

### Bilingual Considerations
When rendered in Arabic script, use corresponding humanist geometries (such as Alexandria or IBM Plex Sans Arabic) matching the same baseline x-height and heavy stroke weights for titles, ensuring field equipment dashboards maintain legibility in intense outdoor sunlight.

## Layout & Spacing

### Layout Model
The layout adopts a flexible 12-column grid on desktop screens (1024px+), collapsing to an 8-column layout on tablet devices (768px-1023px) and a streamlined 4-column flow on mobile handsets (320px-767px). 

### Outdoor & Tactical Density
To accommodate gloved hands, sunlight glare, and tablet usage across fields, critical interaction targets enforce a minimum hit footprint of 48px. Spacing between metric tiles relies on standard `space-md` gaps to maintain visual breathing room between IoT readings.

## Elevation & Depth

### Visual Hierarchy Strategy
The system prioritizes sharp ambient lighting inspired by North African daylight, rejecting heavy dark drop shadows in favor of tinted, crisp elevation rings and tonal separation:

- **Level 0 (Soil/Ground)**: `#FAFAF7` background canvas with zero elevation.
- **Level 1 (Field Cards & IoT Monitors)**: `#FFFFFF` surface with a crisp structural hairline `1px solid #E1E7DE` and an ambient glow: `0 2px 8px -2px rgba(30, 126, 52, 0.06), 0 1px 3px 0 rgba(28, 36, 27, 0.04)`.
- **Level 2 (Active Hubs & Floating Quick Actions)**: `#FFFFFF` surface accompanied by `0 10px 24px -4px rgba(30, 126, 52, 0.10), 0 4px 8px -2px rgba(28, 36, 27, 0.05)`.
- **Level 3 (Modal Alerts & Soil Telemetry Drawers)**: High-contrast elevation utilizing `0 20px 32px -8px rgba(28, 36, 27, 0.16)`.

## Shapes

### Shape Language & Hexagonal Motifs
Interactive components leverage a medium `0.5rem` (8px) base radius for buttons, form inputs, and metrics cards (`roundedness: 2`), ensuring a balance between modern software design and functional ergonomics.

To honor the brand logo's hexagonal honeycomb pattern:
- **Category Badges & Status Nodes**: Feature micro-beveled or clipped hexagonal icon badges (`clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%)`).
- **Data Card Accents**: Use asymmetrical rounded corners (`rounded-tl-2xl rounded-br-2xl`) on highlight cards to echo natural leaf curvature.

## Components

### Buttons
- **Primary Button**: Background `#1E7E34`, text `#FFFFFF`, bold weight. Subtle hover transitions to `#166228` with an active state scale down of `0.98`.
- **Secondary (Terracotta AI / Emergency Action)**: Background `#D9532F`, text `#FFFFFF`, used for field diagnostics, anomaly alerts, or telemetry overrides.
- **Tertiary / Ghost Button**: Hairline border `1.5px solid #1E7E34`, transparent background, text `#1E7E34`. Hover gains a `#F2F5F0` background tint.

### Badges & Category Tags
- Hexagonal badge containers for sector categorization (AI / Cold Storage / Livestock / Machinery / Apiculture).
- Status chips:
  - *Optimal*: Background `#E8F5E9`, text `#1E7E34`, border `#C8E6C9`.
  - *Warning / Heat Stress*: Background `#FEF3C7`, text `#B45309`, border `#FDE68A`.
  - *Critical Soil / Frost Alert*: Background `#FEE2E2`, text `#B91C1C`, border `#FECACA`.

### Cards & Telemetry Tiles
Cards utilize white backgrounds, subtle border separators (`#E1E7DE`), and dedicated status header bars. High-priority cards (such as Greenhouse Cold-Chain or Soil PH level) employ an inner left-side indicator border (4px solid `#38B000` or `#D9532F`).

### Form Inputs & Selectors
- Background: `#FFFFFF`.
- Resting border: `1.5px solid #D4DCD0`.
- Focus border: `2px solid #1E7E34` with an ambient sprout focus ring (`0 0 0 3px rgba(56, 176, 0, 0.18)`).
- Labels are anchored above the input in `label-md` using `#1C241B`.

### Lists & Checkboxes
Checkboxes incorporate `0.25rem` corner radiuses with solid `#1E7E34` fill on check, accompanied by a clean white stroke checkmark. Table rows in list views feature hover highlights using `#F2F5F0` and thin hairline bottom dividers.