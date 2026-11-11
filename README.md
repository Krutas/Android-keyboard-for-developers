# DevBoard · Android Keyboard for Developers

An experimental **Android system keyboard** for programming, Termux, SSH terminals and power users. Designed and iterated first in the interactive Samsung Galaxy A56-inspired browser simulator.

![DevBoard simulator demo](docs/devboard-demo.gif)

## Try the simulator

- **[Interactive simulator (GitHub Pages)](https://krutas.github.io/Android-keyboard-for-developers/simulator.html)** (hosted with GitHub Pages).
- **[Simulator HTML source](docs/simulator.html)**. For local use, download the single HTML file and open it in a browser. Microphone dictation generally requires an HTTPS/localhost origin and a supporting browser.
- **[Preview APK](releases/devboard-debug.apk)** (compiled and debug-signed). The APK is a development/debug build, not a Play Store release.

> GitHub README pages do not execute embedded JavaScript or allow interactive iframes. The animated GIF above is therefore the in-README review. The full interactive simulator runs on GitHub Pages (or a local web server).

## Android features (initial native preview)

| Feature | Android APK | Web simulator |
| --- | --- | --- |
| Install as a system keyboard (`InputMethodService`) | ✅ | Browser-only |
| Terminal shortcut rows, arrows, Esc, Tab, PgUp/PgDn | ✅ | ✅ |
| Numbers, Lithuanian letters on long-press, Space language menu | ✅ | ✅ |
| `123` → symbols, `1/2` → more symbols, `ABC` → letters | ✅ | ✅ |
| Settings: 19 themes, font size, key height, spacing and corners | ✅ | ✅ |
| Colorful ASCII theme | ✅ | ✅ |
| Tap two keys to swap positions | ✅ (layout editor) | ✅ (Configure) |
| Custom combos and one-touch command actions | ✅ | ✅ |
| Saved text snippets | ✅ | ✅ |
| Clipboard history | ✅ opt-in, subject to Android limits | ✅ in-app history |
| Microphone / speech-to-text (LT / EN) | Android SpeechRecognizer | Browser Web Speech API |
| Real voice level indicator | SpeechRecognizer RMS | Web Audio levels where permitted |
| Picture insertion | Not yet | Temporary local blob link |

**Note:** Modifier combinations and navigation key events depend on the receiving Android app. Some terminal clients do not honor every Ctrl, Alt, function, or system shortcut. Native keyboard integration is not a guarantee that those shortcuts work across all apps. Microphone accuracy and language availability depend on the device's installed speech recognition service.

## Install the APK on Android

1. Download [`releases/devboard-debug.apk`](releases/devboard-debug.apk) or the latest APK from **Actions → Android build → DevBoard-debug-APK**.
2. Open the APK on your Android phone. Android may ask permission to install apps from that source.
3. Open **DevBoard** → **Enable DevBoard in Android settings**, then **Choose DevBoard as input method**.
4. In any text field, the DevBoard keyboard will appear. Try `123`, long-press a number, or long-press **Space** to switch LT / EN.
5. Tap 🎙 for dictation. The app requests Android's standard microphone permission. The recognition engine might use a network service; no OpenAI API is used.

**Security:** Android warns whenever a keyboard is enabled because input methods can see typed text. This app stores local preferences and does not implement custom network calls or analytics. Speech recognition is delegated to the system speech service. Clipboard history is **off by default** and requires opt-in. It is disabled inside password fields; clipboard access is subject to Android permissions and restrictions.

## Build from source

Requirements: JDK 17, Android SDK Platform 35, Android SDK Build Tools 35, and Gradle Wrapper (included).

```bash
export ANDROID_HOME="$HOME/Android/Sdk"  # or your SDK path
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. Native Android sources are in `app/src/main/java/lt/krutas/devboard/`.

## Simulator development

`docs/simulator.html` is a standalone HTML/CSS/JS simulator with a white document canvas, keyboard profiles, theme previews, editing, reusable combos, temporary image links, clipboard, and dictation where the browser supports it.

```bash
cd docs
python3 -m http.server 8000
# http://localhost:8000/simulator.html
```

For microphone requests on a different device, use a trusted HTTPS host. A GitHub HTML file viewer cannot run the script directly.

## Project architecture

```text
app/src/main/
  AndroidManifest.xml       Android service and permission declaration
  java/lt/krutas/devboard/
    DevBoardImeService.kt   native keyboard and SpeechRecognizer
    SettingsStore.kt        layouts, palettes, local persistence
    MainActivity.kt         settings, themes, keyboard preview
    LayoutEditorActivity.kt tap-to-swap editor
    ComboEditorActivity.kt  shortcut editor
    SnippetsActivity.kt     saved text snippets
    MicPermissionActivity.kt Android runtime microphone permission
  res/xml/method.xml        IME declaration
docs/simulator.html         interactive browser simulator
docs/devboard-demo.gif      animated README demo
.github/workflows/          Android APK build and optional Pages deployment
```

## Status / limitations

- This is a **developer preview**. No Play Store signature or app store release yet. Debug builds are not appropriate for distributing as production keyboard apps.
- Terminal shortcuts need testing against Termux, Termius, and other target applications.
- Rich content image commit, auto layout per-app, and import of web JSON configuration into the Android app are future work. The native editor currently saves its layout separately from the web simulator.
- The web preview uses Samsung A56-like viewport proportions, not a real Android emulator or a physical millimeter-accurate display.
- The GitHub Pages workflow publishes the web simulator after each update to docs.

License: see [LICENSE](LICENSE).
