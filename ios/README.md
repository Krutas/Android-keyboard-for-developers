# DevBoard for iPhone (source preview)

The iOS port lives beside the Android source. It has a SwiftUI settings/companion app and a native `UIInputViewController` keyboard extension.

## Included

- LT/EN letters, Lithuanian number-key long presses, number and symbol layers.
- Terminal toolbar: Esc, Tab, Ctrl, Alt, arrows, Home, Delete, PgUp/PgDown.
- Shift, Caps Lock, automatic sentence capital letters after punctuation and space.
- Long-press comma/period symbols, emoji and a reusable text snippet.
- Dark, light and Matrix themes; Standard/Terminal profile selection.
- System keyboard switching via 🌐; a system Share Sheet file picker in the companion app.
- Local preferences in an App Group; `RequestsOpenAccess = false` by default.

## Build on macOS

1. Install **Xcode** and [XcodeGen](https://github.com/yonaskolb/XcodeGen).
2. In this folder run `xcodegen generate` and open `DevBoard.xcodeproj`.
3. Set your Apple Development Team for **both targets**: DevBoard and DevBoardKeyboard.
4. Create/register an App Group called `group.lt.krutas.devboard`, enable it in both targets' entitlements, or change the group and bundle IDs to available identifiers.
5. Run the containing app on an iPhone. Then open **Settings → General → Keyboard → Keyboards → Add New Keyboard → DevBoard**.
6. In a text field hold 🌐 and select DevBoard. The app is not yet archived as an IPA.

## Platform limits

- **No microphone in a third-party keyboard extension.** 🎙️ switches to the next installed keyboard for iOS dictation. This is not direct speech-to-text inside DevBoard.
- **No universal file attachment from a keyboard.** The companion app has a document picker and Share Sheet.
- **No guaranteed hardware-style key events.** Some terminal apps may interpret inserted ANSI sequences; other apps may ignore them.
- Password and some phone fields use the Apple keyboard. Apps can also disable third-party keyboards.
- iOS does not give a keyboard the host app's identity for Android-style automatic profile switching.
- This is an uncompiled source implementation. Physical iPhone and Xcode tests, accessibility, App Group signing and App Store review remain.

References: [Apple custom keyboard guide](https://developer.apple.com/documentation/uikit/creating-a-custom-keyboard), [custom keyboard restrictions](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/CustomKeyboard.html).

The iOS code is a first implementation, not full Android feature parity. Do not publish it as a verified iOS release until signed, built and tested on iPhone.
