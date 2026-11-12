# DevBoard release notes

## 0.2.6 developer preview (source; release verification pending)

- Native LT/EN PC/QWERTY, PC Shift symbols and independent Caps Lock. Added missing Latin x to LT QWERTY.
- Native Fn/Combo previews, Android shortcut recorder/validation, iOS companion Combo editor and enabled shortcut strip.
- iOS snippet list, layout editor, held-arrow/backspace repeat and press-animation control. Space brackets are decorative on all platforms.
- Fixed Android End import, per-variant layout persistence/profile JSON, clipboard-listener lifetime and repeat cleanup. Fixed iOS forward Delete at the end of text.
- Added Android JUnit, browser interaction tests and macOS Swift mapping tests. Android 0.2.6 (8), iOS 0.2.6 (4).
- **Unfinished:** iOS clipboard requires approval for optional Full Access. Device testing and Apple signing remain. See [audit and limitations](PARITY-0.2.6.md). Local Android JUnit/build/lint, APK signature/version, browser regression and five publication-gate checks passed. No successful iOS build or published 0.2.6 packages are claimed here yet.

## Android 0.2.5 / iOS 0.2.5 (2026-11-12)

- Android APK: `releases/DevBoard-v0.2.5.apk` (debug-signed developer build).
- iOS unsigned IPA: `releases/DevBoard-iOS-v0.2.5-unsigned.ipa` (requires Apple signing and provisioning).
- iOS Simulator: `releases/DevBoard-iOS-v0.2.5-Simulator.zip`.
- Source fixes since 0.2.4: long-press Shift Caps Lock, raised punctuation popups for `.` and `,`, and tighter single-line `< Space >` labeling.
- Web simulator: Fn / Combo controls, keyboard preview, language layout selectors, button-press animation toggle and permission-state handling.
- Builds reported successful and artifacts committed to `main` (`94594ec`). Physical Android/iPhone testing remains pending; unsigned IPA is not directly installable on iPhone.


## Android 0.2.4 / iOS 0.2 (2026-11-11)

**Fixed:** The `<`, Space and `>` legends previously relied on whitespace in the Android button label, which could wrap on narrow screens. Android now draws the brackets as independent glyphs at the edges of a single Space button, while the normal Space title stays centered.

**iOS update:** The native iOS keyboard now has a single Space key containing three horizontally aligned noninteractive labels, with touch-location routing that inserts `<` from the left edge, space from the center, and `>` from the right edge. The app and extension build metadata are 0.2 (2).

**Web:** The interactive simulator uses a fixed three-column one-line layout for the Space key.

**Retained:** Android voice input, Ctrl/Alt/terminal keys, emoji picker, profiles, clipboard, key-press animation and Space-center long-press for LT/EN.

**Known limitation:** Unicode 18.0 glyphs are not bundled as a font. Certain emoji may render incorrectly on operating systems whose emoji fonts are too old. Not addressed in this version.

**Builds verified:** Android v0.2.4 passed Gradle `assembleDebug` and `lintDebug`; the APK version and signing block were checked ([release](https://github.com/Krutas/Android-keyboard-for-developers/releases/tag/v0.2.4)). iOS v0.2 passed [macOS GitHub Actions](https://github.com/Krutas/Android-keyboard-for-developers/actions/runs/43094577722); the Simulator ZIP and unsigned IPA both include version `0.2` (build `2`) for the app and extension ([iOS release](https://github.com/Krutas/Android-keyboard-for-developers/releases/tag/ios-v0.2)). An unsigned IPA still needs Apple signing before device installation.

## Android 0.2.3
- Top-right combined LT/EN voice input, microphone removed from terminal shortcut row.
- `<` and `>` touch zones inside Space, plus web/native key-press animations and new RGB app icon.

## Android 0.2.2
- Corrected keyboard row spacing and clipping.

## Android 0.2.1
- Expanded bundled Unicode emoji catalog and recent emoji list.

## iOS 0.1
- Initial SwiftUI companion app and native third-party keyboard extension.
