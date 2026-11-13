# DevBoard 0.3 audit and verification

This is a developer preview, not a claim of verified device or store readiness. The existing web behavior defines the requested PC/QWERTY variants. LT QWERTY is the project's Lithuanian arrangement, with extra keys for Latin letters; it is not a certified implementation of a national keyboard standard.

| Requested behavior | Android | iOS | Web |
| --- | --- | --- | --- |
| LT PC and LT QWERTY | Independent saved layouts; PC number holds and dedicated QWERTY letters | Same mapping in native extension and companion editor | Same mapping; variant edits now persist |
| EN PC and EN QWERTY | Independent selection per language | Independent selection per language | Independent selection per language |
| Shift / Caps Lock | PC number/punctuation Shift; Caps affects letters | Same; tap, double tap and hold | Same; actual legends and preview follow Shift |
| Fn / enabled Combo preview | F1–F8, enabled combo strip, full settings preview | F1–F8, enabled combo strip, companion preview/editor | F1–F8, enabled combos, live preview |
| Combo recording/edit/disable/delete | Touch recorder and physical key capture; validated actions | Companion touch recorder, modifiers, edit/disable/delete | Touch/physical recorder, edit/disable/delete |
| Saved snippets | Separate text-only editor and insertion menu | Companion text-only list and keyboard insertion menu | Separate text-only editor and insertion drawer |
| Clipboard | Current item; opt-in history, clearing; disabled in password fields | **Unavailable in v0.3:** `RequestsOpenAccess: false`; optional Full Access is excluded | Explicit copies within the demo; history and clearing |
| Held arrows | Delay, repeat, release/cancel/IME cleanup; modifiers preserved | Repeat with release/cancel/disappearance cleanup; terminal protocol limits below | Repeat and release/cancel/blur cleanup; Shift selection |
| Edit layout | Swap, hold-delete, confirmed reset; variants preserved in JSON | Companion swap, hold-delete, confirmed reset | Swap, hold-delete, confirmed reset per variant |
| Press animations | Toggle; letter, terminal, Fn, combo and toolbar buttons | Toggle and native touch animation | Toggle; keyboard press feedback |
| Mic permission | Real runtime permission and status; installed recognizer required | Microphone unavailable to third-party keyboards; mic switches keyboard | Actual browser permission status/request; HTTPS and speech API required |
| Space | One space at every position, decorative brackets, hold for language | Same | Same |

## Audit corrections

- Preserved End as End during Android layout import; previously it became Delete.
- Preserved per-language/per-variant custom rows instead of replacing LT QWERTY during rendering. Android profile cloning, deletion and JSON include variant layouts.
- Restored Latin `x` in LT QWERTY, caught by alphabet-coverage tests.
- Made PC Shift output, key legends and Caps Lock consistent; symbol pages remain literal.
- Validated Android combos and added generic Ctrl/Alt/Shift hardware key dispatch; unsupported actions are rejected when saved.
- Prevented iOS forward Delete at the document end from deleting the previous character.
- Kept snippets distinct from clipboard history, with exact multiline insertion.
- Stopped Android clipboard observation when its input view finishes; repeat callbacks stop when their keys disappear.
- Corrected iOS Space behavior to the already agreed decorative brackets used by Android/web after 0.2.4.
- Stopped Android arrow/Backspace repeat when the finger slides outside a key.
- Corrected the iOS companion preview to show the actual native toolbar and one horizontal enabled-Combo strip.
- Removed path filters from required workflows so every main commit receives Android, web and iOS CI; Pages is also required by the publisher.

## Verification record

Android follow-up on 2026-11-12: [Space/Fn visibility audit](ANDROID-0.3-AUDIT.md), with native-rendered before/after evidence, Android interaction regressions and remaining checklist gaps. The original mapping-only test record below predates these additional tests.

- Android: JUnit mapping/variant/shortcut-parser tests and Gradle `testDebugUnitTest`, `assembleDebug`, `lintDebug`. **Passed locally on 2026-11-12:** all three JUnit tests, debug APK build and lint (0 errors, 43 warnings). Verified local APK metadata is 0.3 (9), package lt.krutas.devboard; APK Signature Scheme v2 verification passed. Lint warnings include localization, accessibility heuristics and resources; a successful task does not mean zero warnings.
- Web: Chromium browser regression script `tests/web-parity.cjs` checks actual insertion, Shift/Caps, four variants, persistent swaps, long-press deletion and reset confirmation, Fn, combo enable state, multiline snippets, Space positions, held arrows, animation toggle and permission status. **Passed locally on 2026-11-12**, including long-press Caps Lock; a screenshot was inspected. CI captures the same screenshot as an artifact.
- iOS: `tests/MappingTests.swift` tests the pure mapping and ANSI encodings in macOS CI. The workflow builds app plus extension for simulator and device and verifies **both** bundle versions in both builds before uploading. No Xcode is available in the Linux workspace; CI success must be checked before publishing packages.
- Release publication: eleven Python tests cover dirty/unpushed source, disabled iOS Full Access, missing/failed/incomplete workflows, stale SHAs, branch/event mismatches and the v0.3 target. `tools/publish_verified_release.py` requires successful Android/iOS/web/Pages workflows for the final pushed main SHA, rechecks CI before publication, verifies APK metadata/signature and compares its manifest with the local build, and checks both iOS bundle versions and disabled Full Access in both archives. Release assets come from those exact CI runs. Publication must wait for all final-SHA checks; older successful builds do not qualify.
- No physical Android or iPhone interaction, Apple signing, accessibility audit or App Store validation is claimed. Manual checks remain in TESTING.md.

## Limits and unresolved work

- iOS clipboard parity is **unfinished** and excluded from v0.3: this release keeps `RequestsOpenAccess` false. Enabling Full Access would be a separate capability change. Clipboard UI explains that restriction; no microphone permission is requested or represented as granted. Companion settings can be read from the App Group, but keyboard-side shared writes need Full Access. Language/Fn still work for the current keyboard session.
- iOS cannot send universal hardware events, invoke the host editing menu, identify the host app for automatic profiles, or use a microphone in the keyboard extension. Terminal profile inserts ANSI/control text; only compatible terminals interpret it. Standard profile supports character navigation, contextual Home/End, and guarded forward delete; vertical navigation, Shift selection, F-keys and OS shortcuts are limited or unavailable. Unsupported operations show an explanation.
- iOS retains its three native themes and two profiles. It does not implement Android's 19-theme appearance controls, arbitrary profile management, per-app mappings or cross-platform JSON import/export. These remain broader parity gaps.
- Android hardware shortcuts depend on the target editor. Microphone availability/language/network behavior depend on the installed speech service. The build is debug-signed, not a store release.
- Web is a simulator, not an installed IME. It cannot activate system keyboards, run OS shortcuts or upload files via its temporary blob links. Microphone denial, service absence and actual speech recognition still need interactive browser/device tests.
- All platforms use OS emoji fonts; recent Unicode glyphs and sequences may not render.
- Native touch timing, orientation, narrow layouts, App Group provisioning, iOS paste approval and dictation require device tests. Automated mapping/build/browser tests do not establish those results.

Apple references: [Custom keyboard restrictions](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/CustomKeyboard.html), [open access](https://developer.apple.com/documentation/uikit/configuring-open-access-for-a-custom-keyboard), [text position API](https://developer.apple.com/documentation/uikit/uitextdocumentproxy/adjusttextposition(bycharacteroffset:)).
