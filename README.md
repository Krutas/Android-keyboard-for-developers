# DevBoard ⌨️

A customizable developer keyboard for Android. Built for Termux, terminals, SSH and everyday typing.

![DevBoard preview](docs/devboard-demo.gif)

**Android:** 0.2.4 · **iOS:** 0.2 · [Download APK](releases/DevBoard-v0.2.4.apk) · [Try the simulator](https://krutas.github.io/Android-keyboard-for-developers/simulator.html)

## What it does

- Terminal keys: Esc, Ctrl, Alt, Tab, arrows, Home, PgUp/PgDn, Delete and optional F1–F12.
- Lithuanian/English typing, numbers, symbols and a popup for punctuation.
- 19 themes, keyboard sizes, spacing and customizable layouts.
- Named profiles, snippets, JSON layout import/export and optional per-app profiles.
- Clipboard history (off by default), 3,963 Unicode 18.0 emojis in nine categories, recent emojis and speech-to-text controls.
- Attach any file using Android's file picker and share sheet.
- Switch to any enabled installed keyboard through Android's system picker.

## Fixed in 0.2.0

- Restored Lithuanian letters on the number row.
- Fixed the slightly clipped bottom row.
- Replaced **End** with **Delete**.
- Shift returns to lowercase after one capital letter; Caps Lock stays on when selected.
- Added sentence capitalization after a period and space.
- Long-press **,** or **.** to pick extra symbols.
- Moved 🎙️ voice input beside **Alt**, with Finish/Cancel controls.
- Replaced the bottom microphone key with an 😀 emoji panel.

Emoji appearance depends on OS font support; newly released Unicode 18.0 symbols may not render on older devices. Emoji data is bundled offline from [Unicode](https://www.unicode.org/Public/emoji/latest/emoji-test.txt) under the [Unicode License](https://www.unicode.org/license.txt).

## New in 0.2.1

- Emoji picker expanded to 3,963 Unicode emoji variants in 9 categories, plus recent emoji.
- APK filename changed to `DevBoard-v0.2.1.apk`.

## New in 0.2.2

- Fixed overlapping keyboard rows: added bottom spacing so lower key edges remain visible.
- Android version updated to 0.2.2 (versionCode 4).
- iOS version tracked separately as 0.1; its unsigned CI build is available as an Actions artifact.

## New in 0.2.3

- Removed the microphone key from the terminal shortcut row. The top-right toolbar shows `LT 🎙` (or `EN 🎙`) in one button: tap for dictation, long-press to switch language.
- Embedded `<` and `>` touch zones inside the Space key: tap the left/right edge to type the bracket, or the center for a space. Existing saved layouts also hide the old microphone key.
- Added tactile press animations to Android keyboard buttons and the web simulator.
- Updated the Android app icon to the RGB mechanical keyboard design.

## Fixed in Android 0.2.4 / New in iOS 0.2

- **Space is now a single-line key on both platforms:** the `<` character is painted on the left, `Space` remains centered, and `>` sits on the right. No text padding or wrapping, including narrow layouts.
- **Three distinct tap zones:** tapping the left 22% inserts `<`, the center inserts a space, and the right 22% inserts `>`. Normal text entry remains unchanged.
- **Android:** the existing long-press on Space still selects the keyboard language. Native press animation, microphone control, clipboard, profiles and emoji picker are preserved.
- **iOS:** added the same three-zone Space key to the native keyboard extension across letter, number, symbol and emoji modes. Updated `CFBundleShortVersionString` to `0.2` and build number to `2`.
- **Web simulator:** keeps all three labels on one line by using a three-column layout for the Space key.
- **Testing:** added a width/orientation regression checklist for bracket placement and touch targets.

The emoji catalog includes Unicode 18.0 entries, but **some emoji can appear as boxes or incomplete sequences when the device OS lacks matching glyphs**. This release does not ship an emoji font or claim to fix that device-dependent limitation.

See [release history](docs/CHANGELOG.md) and [test checklist](docs/TESTING.md).

## Install on Android

1. [Download the APK](releases/DevBoard-v0.2.4.apk) and install it.
2. Open **DevBoard**, enable it in Android keyboard settings, and select it as your keyboard.
3. Open any text field. Use **Settings** to customize it or **Profiles** to import layouts.

Despite the cleaner filename, this remains a **debug-signed developer build**, not an app-store release. It needs on-device testing. Some terminal shortcuts depend on the receiving app. Files are shared through Android's chooser, not directly uploaded to an active chat.

## Build

Requires JDK 17, Android SDK 35 and its build tools.

```bash
export JAVA_HOME="/path/to/jdk17"
export ANDROID_HOME="/path/to/android-sdk"
./gradlew :app:assembleDebug :app:lintDebug
```

Built APK: `app/build/outputs/apk/debug/app-debug.apk`, distributed as `releases/DevBoard-v0.2.4.apk`.

## Privacy

Preferences are local. Clipboard recording is opt-in and disabled in password fields. Speech recognition uses the Android system service and may require internet access. The web simulator uses temporary local file links and does **not** upload files to a server.

See [testing checklist](docs/TESTING.md) and [source code](app/src/main/java/lt/krutas/devboard/). License: [MIT](LICENSE).

## iPhone version

The native iOS keyboard extension and SwiftUI companion app are in [ios/](ios/README.md). **iOS v0.2** adds a single-line `<  space  >` key with three touch zones. Its [macOS CI workflow](https://github.com/Krutas/Android-keyboard-for-developers/actions/workflows/ios.yml) builds an iOS Simulator package and an **unsigned** iPhone IPA. The latter requires Apple signing/provisioning to install on a real iPhone. Android-only features (native voice input and unrestricted file selection from the keyboard) are restricted by iOS.
