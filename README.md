# DevBoard ⌨️

A customizable developer keyboard for Android. Built for Termux, terminals, SSH and everyday typing.

![DevBoard preview](docs/devboard-demo.gif)

**Version:** 0.2.0 · [Download APK](releases/devboard-debug.apk) · [Try the simulator](https://krutas.github.io/Android-keyboard-for-developers/simulator.html)

## What it does

- Terminal keys: Esc, Ctrl, Alt, Tab, arrows, Home, PgUp/PgDn, Delete and optional F1–F12.
- Lithuanian/English typing, numbers, symbols and a popup for punctuation.
- 19 themes, keyboard sizes, spacing and customizable layouts.
- Named profiles, snippets, JSON layout import/export and optional per-app profiles.
- Clipboard history (off by default), emoji picker and speech-to-text controls.
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

## Install on Android

1. [Download the APK](releases/devboard-debug.apk) and install it.
2. Open **DevBoard**, enable it in Android keyboard settings, and select it as your keyboard.
3. Open any text field. Use **Settings** to customize it or **Profiles** to import layouts.

This is a **debug-signed developer build**, not an app-store release. It needs on-device testing. Some terminal shortcuts depend on the receiving app. Files are shared through Android's chooser, not directly uploaded to an active chat.

## Build

Requires JDK 17, Android SDK 35 and its build tools.

```bash
export JAVA_HOME="/path/to/jdk17"
export ANDROID_HOME="/path/to/android-sdk"
./gradlew :app:assembleDebug :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy

Preferences are local. Clipboard recording is opt-in and disabled in password fields. Speech recognition uses the Android system service and may require internet access. The web simulator uses temporary local file links and does **not** upload files to a server.

See [testing checklist](docs/TESTING.md) and [source code](app/src/main/java/lt/krutas/devboard/). License: [MIT](LICENSE).

## iPhone version

An initial native iOS keyboard extension and companion app are in [ios/](ios/README.md). They include LT typing, terminal controls, emoji, themes and file sharing from the companion app. **The iOS app is source-only** until it is built and tested in Xcode. Apple's keyboard restrictions prevent direct microphone recording, universal file attachment and Android-style per-app detection.
