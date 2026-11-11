# DevBoard release notes

## Android 0.2.4 / iOS 0.2 (2026-11-11)

**Fixed:** The `<`, Space and `>` legends previously relied on whitespace in the Android button label, which could wrap on narrow screens. Android now draws the brackets as independent glyphs at the edges of a single Space button, while the normal Space title stays centered.

**iOS update:** The native iOS keyboard now has a single Space key containing three horizontally aligned noninteractive labels, with touch-location routing that inserts `<` from the left edge, space from the center, and `>` from the right edge. The app and extension build metadata are 0.2 (2).

**Web:** The interactive simulator uses a fixed three-column one-line layout for the Space key.

**Retained:** Android voice input, Ctrl/Alt/terminal keys, emoji picker, profiles, clipboard, key-press animation and Space-center long-press for LT/EN.

**Known limitation:** Unicode 18.0 glyphs are not bundled as a font. Certain emoji may render incorrectly on operating systems whose emoji fonts are too old. Not addressed in this version.

**Builds:** Android v0.2.4 uses Gradle on Linux/WSL (debug-signed test APK). iOS v0.2 is built by macOS GitHub Actions (Simulator ZIP and unsigned iPhone IPA). An unsigned IPA needs Apple signing before device installation.

## Android 0.2.3
- Top-right combined LT/EN voice input, microphone removed from terminal shortcut row.
- `<` and `>` touch zones inside Space, plus web/native key-press animations and new RGB app icon.

## Android 0.2.2
- Corrected keyboard row spacing and clipping.

## Android 0.2.1
- Expanded bundled Unicode emoji catalog and recent emoji list.

## iOS 0.1
- Initial SwiftUI companion app and native third-party keyboard extension.
