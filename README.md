# DevBoard ⌨️

A native developer keyboard for Android and iOS, with an interactive web simulator. Built for terminals and everyday typing.

![DevBoard preview](docs/devboard-demo.gif)

**DevBoard 3.1** · [Android APK](https://github.com/Krutas/Android-keyboard-for-developers/releases/download/v3.1/DevBoard-v3.1.apk) · [Web simulator](https://krutas.github.io/Android-keyboard-for-developers/simulator.html)

If DevBoard is already installed, uninstall it before installing 3.1. The signing certificate changed. Uninstalling clears the previous installation's local settings and profiles. After installation, enable DevBoard in Android input settings and select it in the keyboard picker.

3.1 updates the Android keyboard, its settings and the web simulator. See the [release notes](docs/CHANGELOG.md) and the [3.1 specification](docs/superpowers/plans/2026-11-12-v31-android-web.md). The Space key has decorative angle brackets. Tap anywhere to insert one space or hold it to choose the language.

## Features

- Esc, Ctrl, Alt, Tab, arrows, Home, End, Delete, PgUp/PgDn and optional F1–F8.
- Separate snippet and clipboard menus; Android clipboard history is opt-in and excluded from password fields.
- Native layout editors: tap two keys to swap, hold for Delete, confirm Reset. Web and Android preserve variants in profile JSON.
- Android: 19 themes, appearance controls, named profiles, JSON import/export and per-app profiles.
- iOS: native SwiftUI companion settings, three themes, two profiles, App Group preferences and a UIKit keyboard extension.
- Offline emoji picker and recent emoji. Rendering depends on OS fonts.
- Android file chooser/share sheet and iOS companion Share Sheet. Web file links are local previews.

## Android build and install

Requires JDK 17, Android SDK 35 and build tools.

```bash
./gradlew :app:testDebugUnitTest :app:testPreviewUnitTest :app:assembleDebug :app:assemblePreview :app:lintDebug
```

The developer build is debug-signed. The Android APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## iOS and browser verification

See [iOS build/install instructions](ios/README.md). macOS CI creates a simulator ZIP and an **unsigned** IPA. The IPA needs Apple signing/provisioning before installation on a physical iPhone.

Browser regression tests use Playwright and Chromium:

```bash
PLAYWRIGHT_MODULE=/path/to/playwright node tests/web-parity.cjs
```

CI runs the same tests and captures a screenshot. For interactive microphone tests, use HTTPS or localhost in a supported browser.

## Privacy and limits

Settings and snippets are local. DevBoard makes no custom keystroke network requests. Speech recognition may use the system/browser provider's cloud service. iOS Full Access remains disabled in v0.3; the clipboard feature is unavailable there. Password fields may use the system keyboard. Terminal key support depends on the receiving app.

[Release history](docs/CHANGELOG.md) · [Test checklist](docs/TESTING.md) · [License](LICENSE)
