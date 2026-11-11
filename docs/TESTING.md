# DevBoard acceptance test checklist

## Simulator (Chrome, secure origin)
- [ ] The Galaxy A56-sized document accepts typed letters, numbers, symbols, and Enter.
- [ ] \`123\` opens symbols, \`1/2\` opens more symbols, and \`ABC\` returns to letters.
- [ ] Long press number 1–9 for Lithuanian letters; hold Space for Lithuanian/English.
- [ ] Open Settings and change 19 themes, font size, key height, gap, and radius. Preview updates.
- [ ] Configure: tap two keys to exchange their locations; reload and verify persistence.
- [ ] + Combo: create, invoke, edit and delete a shortcut.
- [ ] Clipboard recent items and named saved snippets insert into the document.
- [ ] 📎 accepts a TXT/PDF/ZIP/image and displays name, MIME type, and size; the link is local only.
- [ ] Upper 🎙 opens microphone recording controls in the browser; bottom 😀 opens emoji picker. Dictation asks for permission, shows voice level, and transcribes in a supporting browser.
- [ ] Settings → Choose keyboard opens a clearly marked demo picker, not a fabricated installed-keyboard list.
- [ ] Export web JSON for import on Android.

## Samsung Galaxy A56 (real APK, manual validation required)
- [ ] Install debug APK and enable DevBoard under Android input-method settings.
- [ ] Select DevBoard from the system IME picker; use it in a normal text field.
- [ ] Installed keyboards list shows enabled and disabled IMEs; tapping invokes Android's picker/settings.
- [ ] Settings appearance changes update native keyboard layout and font.
- [ ] 123, 1/2, ABC, Shift, Backspace hold, Ctrl/Alt, arrow keys, Tab, Esc, Home/Del, PgUp/PgDn behave as expected.
- [ ] Fn toolbar sends F1–F12, Insert and forward Delete where the target app supports them.
- [ ] Named snippets insert text; clipboard capture is opt-in, can be cleared, and is unavailable in password fields.
- [ ] Profiles clone/select/delete; JSON Terminal layout imports and exports; 'space' from web JSON becomes Space.
- [ ] Per-app mapping activates a named profile only for configured packages (e.g. com.termux).
- [ ] 📎 invokes Android's file picker and share chooser. It does **not** silently attach a file to an app.
- [ ] Top-right combined LT 🎙 / EN 🎙 button opens speech panel; Baigti finishes and Atšaukti discards dictation. Verify LT/EN, live microphone level, and Cyrillic blocking in LT mode.
- [ ] Bottom 😀 opens nine scrollable emoji categories plus Recent, inserts whole ZWJ/skin-tone/flag sequences in the active field, preserves scrolling while inserting multiple emoji, ⌫ deletes, and ABC closes picker. Test saved layouts migrated from old microphone placement.
- [ ] Rotate phone and test portrait/landscape, return from Settings, disable/reenable keyboard, and reopen apps.

## Release gate
- [x] Gradle \`:app:assembleDebug\` succeeds on Linux/WSL with Android SDK 35 and JDK 17.
- [ ] Manual device testing completed on Android.
- [ ] Signed release APK and store review (developer preview is debug-signed only).

Known constraint: Android apps decide which hardware key events and content MIME types they accept. The web simulator cannot access installed IMEs, and its blob links are local to that browser session.

## Android v0.2.2 layout regression
- [ ] Verify every row below the first has a fully visible lower key border (no overlap with the following row).
- [ ] Check the lowest row, portrait and landscape, with minimum and maximum key height and spacing settings.
- [ ] Test typing and long presses after the row-margin change.

## iOS v0.1 release gate (historical)
- [x] GitHub Actions macOS build succeeded for simulator and unsigned device app (run 43059201642).
- [x] CI artifact includes `DevBoard-iOS-v0.1-Simulator.zip` and `DevBoard-iOS-v0.1-unsigned.ipa`.
- [ ] Sign the containing app and keyboard extension using valid Apple provisioning profiles.
- [ ] Test keyboard enablement, typing, emoji, settings and switching on a physical iPhone.
- [ ] Verify App Group entitlements and accessibility; review before public distribution.

- [ ] Terminal shortcut row contains no microphone key, including when loading older saved layouts; voice dictation remains available by tapping the combined LT 🎙 button; long-press changes LT/EN.

- [ ] Space key visually contains `<` on its left and `>` on its right; tapping its left/center/right zones types `<` / space / `>` in letters and symbols modes. Saved layouts have no extra bracket keys.

## Android v0.2.3 smoke checks

- [ ] Press any key, Space edge, Backspace: observe quick shrink/fade and recovery.
- [ ] Backspace still repeats when held; Space edge zones insert `<`, space, `>`; center long press changes language.
- [ ] Top right `LT 🎙` starts dictation and long press opens language picker.
- [ ] Launcher icon shows the updated RGB keyboard artwork.
- [ ] Web simulator key presses animate; JavaScript console is clear.

## Android v0.2.4 and iOS v0.2: Space single-line regression

- [ ] **One key only:** bottom-row Space has `<` at its left, `Space` (iOS: `space`) centered, and `>` at its right on **the same horizontal line**. No label wrapping, clipping or three separate keys.
- [ ] Left **22%** of Space inserts `<`; center inserts a regular space; right **22%** inserts `>`.
- [ ] Repeat touch tests in portrait, landscape, narrow widths, and with the largest font-size setting (Android).
- [ ] Switch between letters, number/symbol pages and emoji: the Space layout and all three touch zones remain consistent.
- [ ] Android: long-press center opens language picker; shorter presses still insert exactly one space. Haptic/press animation and Backspace hold remain functional.
- [ ] iOS: Space key still triggers sentence capitalization after punctuation. App and keyboard extension both advertise `0.2` / build `2`.
- [ ] Web simulator: browser Space label uses a three-column single-line layout; click the three zones and inspect typed text.
- [ ] Run Android Gradle `assembleDebug` (and `lintDebug` if available); verify `DevBoard-v0.2.4.apk` signature and version.
- [x] GitHub Actions [run 43094577722](https://github.com/Krutas/Android-keyboard-for-developers/actions/runs/43094577722) built iOS v0.2 Simulator ZIP and unsigned IPA. Verified app **and** keyboard extension version `0.2` / build `2` in both archives. Unsigned IPA **does not install on an iPhone** without valid Apple signing.
- [ ] Real hardware: test emoji display and note any missing glyphs or ZWJ sequences. App uses OS emoji fonts; Unicode 18.0 appearance is not guaranteed.
