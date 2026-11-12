# DevBoard iOS 0.2.6 developer preview

Native SwiftUI companion app and UIKit keyboard extension. Includes LT/EN PC and QWERTY variants, Shift/Caps mapping, F1–F8, enabled combo strip and companion recorder/editor, text-only snippets, arrow/backspace repeat, layout swap/delete/reset, live preview and optional press animations. Space always inserts a space; its brackets are decorative. Hold Space or LT/EN mic for language selection.

The keyboard microphone button switches to another installed keyboard for system dictation. **It does not record audio.**

Full Android parity is not claimed. Clipboard access is currently blocked by `RequestsOpenAccess: false`; approval to enable optional Full Access is pending. Basic typing and reading companion settings use the existing App Group. Shared keyboard-side writes require Full Access; language/Fn switches remain available in the session. See [the complete audit and limitations](../docs/PARITY-0.2.6.md).

## Build and packages

On macOS, install Xcode and XcodeGen, run `xcodegen generate` in `ios/`, then build the DevBoard scheme. Set an Apple team for **both** targets and provision `group.lt.krutas.devboard` (or replace bundle/group IDs with your own).

The [iOS CI workflow](https://github.com/Krutas/Android-keyboard-for-developers/actions/workflows/ios.yml) runs shared mapping tests and builds:

- `DevBoard-iOS-v0.2.6-Simulator.zip`: simulator app with keyboard extension.
- `DevBoard-iOS-v0.2.6-unsigned.ipa`: device app and extension, **not installable normally until signed**.

CI checks version `0.2.6` (build `4`) for app and extension in both builds before uploading. Packages must only be published after the exact source revision passes CI. CI artifacts expire after 30 days.

## Enable on iPhone

After signing and installing: Settings → General → Keyboard → Keyboards → Add New Keyboard → DevBoard. In a text field, hold 🌐 to select it. Password and certain phone fields use Apple's keyboard; apps may disallow third-party keyboards.

Terminal mode inserts ANSI/control text for compatible terminal apps. It does not send universal hardware events. Standard mode cannot provide vertical cursor navigation, hardware F-keys, universal Ctrl shortcuts or Shift selection. iOS also restricts host-app identification, direct file attachment and keyboard microphone access. Share files from the companion app.

Native touch behavior, orientation, paste approval, App Group provisioning, accessibility and physical iPhone behavior still need manual testing. Emoji use OS fonts; newly introduced glyphs may be missing.

References: [Apple custom keyboards](https://developer.apple.com/documentation/uikit/creating-a-custom-keyboard), [restrictions](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/CustomKeyboard.html), [open access](https://developer.apple.com/documentation/uikit/configuring-open-access-for-a-custom-keyboard).
