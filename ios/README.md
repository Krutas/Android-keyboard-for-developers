# DevBoard for iPhone v0.1 (unsigned preview)

The iOS port lives beside the Android source. It has a SwiftUI settings/companion app and a native `UIInputViewController` keyboard extension.

## Included

- LT/EN letters, Lithuanian number-key long presses, number and symbol layers.
- Terminal toolbar: Esc, Tab, Ctrl, Alt, arrows, Home, Delete, PgUp/PgDown.
- Shift, Caps Lock, automatic sentence capital letters after punctuation and space.
- Long-press comma/period symbols, emoji and a reusable text snippet.
- Dark, light and Matrix themes; Standard/Terminal profile selection.
- System keyboard switching via 🌐; a system Share Sheet file picker in the companion app.
- Local preferences in an App Group; `RequestsOpenAccess = false` by default.

## Download built packages

[GitHub Actions iOS v0.1 build](https://github.com/Krutas/Android-keyboard-for-developers/actions/runs/43059201642) **succeeded** and packages:
- `DevBoard-iOS-v0.1-Simulator.zip`: an iOS Simulator `.app` for Xcode testing.
- `DevBoard-iOS-v0.1-unsigned.ipa`: a device-targeted app bundle with the keyboard extension, **not signed**.

The unsigned IPA **cannot be installed normally on an iPhone**. You must sign the app and extension with valid Apple credentials and provisioning profiles using Xcode or an appropriate signing workflow. The workflow's artifacts expire after 30 days.

## Build on macOS

1. Install **Xcode** and [XcodeGen](https://github.com/yonaskolb/XcodeGen).
2. In this folder run `xcodegen generate` and open `DevBoard.xcodeproj`.
3. Set your Apple Development Team for **both targets**: DevBoard and DevBoardKeyboard.
4. Create/register an App Group called `group.lt.krutas.devboard`, enable it in both targets' entitlements, or change the group and bundle IDs to available identifiers.
5. Run the containing app on an iPhone. Then open **Settings → General → Keyboard → Keyboards → Add New Keyboard → DevBoard**.
6. In a text field hold 🌐 and select DevBoard. A signed IPA requires an Apple signing configuration; CI provides an unsigned IPA package.

## Platform limits

- **No microphone in a third-party keyboard extension.** 🎙️ switches to the next installed keyboard for iOS dictation. This is not direct speech-to-text inside DevBoard.
- **No universal file attachment from a keyboard.** The companion app has a document picker and Share Sheet.
- **No guaranteed hardware-style key events.** Some terminal apps may interpret inserted ANSI sequences; other apps may ignore them.
- Password and some phone fields use the Apple keyboard. Apps can also disable third-party keyboards.
- iOS does not give a keyboard the host app's identity for Android-style automatic profile switching.
- CI builds the source, but physical iPhone tests, accessibility, App Group signing and App Store review remain.

References: [Apple custom keyboard guide](https://developer.apple.com/documentation/uikit/creating-a-custom-keyboard), [custom keyboard restrictions](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/CustomKeyboard.html).

The iOS code is a first implementation, not full Android feature parity. Do not publish it as a verified iOS release until signed, built and tested on iPhone.

Emoji picker: offline Unicode 18.0 RGI catalog, nine categories, Recent (48 entries), virtualized scrolling. Appearance varies by OS emoji font support. Source: https://www.unicode.org/Public/emoji/latest/emoji-test.txt (Unicode License).
