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
- [ ] Upper 🎙 (next to Alt) opens speech panel; Baigti finishes and Atšaukti discards dictation. Verify LT/EN, live microphone level, and Cyrillic blocking in LT mode.
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

## iOS v0.1 release gate
- [x] GitHub Actions macOS build succeeded for simulator and unsigned device app (run 43059201642).
- [x] CI artifact includes `DevBoard-iOS-v0.1-Simulator.zip` and `DevBoard-iOS-v0.1-unsigned.ipa`.
- [ ] Sign the containing app and keyboard extension using valid Apple provisioning profiles.
- [ ] Test keyboard enablement, typing, emoji, settings and switching on a physical iPhone.
- [ ] Verify App Group entitlements and accessibility; review before public distribution.
