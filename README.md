# DevBoard ⌨️

A customizable developer keyboard for Android. Built for Termux, terminals, SSH and everyday typing.

![DevBoard preview](docs/devboard-demo.gif)

**Android:** 0.2.2 · **iOS:** 0.1 · [Download APK](releases/DevBoard-v0.2.2.apk) · [Try the simulator](https://krutas.github.io/Android-keyboard-for-developers/simulator.html)

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

## Install on Android

1. [Download the APK](releases/DevBoard-v0.2.2.apk) and install it.
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

Built APK: `app/build/outputs/apk/debug/app-debug.apk`, distributed as `releases/DevBoard-v0.2.2.apk`.

## Privacy

Preferences are local. Clipboard recording is opt-in and disabled in password fields. Speech recognition uses the Android system service and may require internet access. The web simulator uses temporary local file links and does **not** upload files to a server.

See [testing checklist](docs/TESTING.md) and [source code](app/src/main/java/lt/krutas/devboard/). License: [MIT](LICENSE).

## iPhone version

An initial native iOS keyboard extension and companion app are in [ios/](ios/README.md). [Download the iOS v0.1 build artifact](https://github.com/Krutas/Android-keyboard-for-developers/actions/runs/43059201642) (GitHub Actions, expires after 30 days). They include LT typing, terminal controls, emoji, themes and file sharing from the companion app. **iOS v0.1 compiles successfully in GitHub Actions**, producing a simulator app and unsigned IPA; neither is a signed, device-tested release. Apple's keyboard restrictions prevent direct microphone recording, universal file attachment and Android-style per-app detection.
