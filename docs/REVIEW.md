# Simulator review notes

- The README uses `devboard-demo.gif` because GitHub sanitizes scripts and embedded iframes.
- The full demo runs in `simulator.html` on GitHub Pages.
- Test: Configure → tap source key → tap destination → both keys swap.
- Test: + Combo → select Ctrl+C → Save → tap new combo.
- Test: 123 / 1/2 / ABC switches symbols; long-press 1–9 in LT mode for accented letters.
- Microphone needs HTTPS, permission, supported Web Speech API and recognition service.
- On Android, enable DevBoard in system input settings and select it as keyboard.

- Test: 📎 Attach file → choose TXT, PDF, ZIP, or image → verify filename/MIME/size → Insert link. The link is local only, not a network upload.
- Native: 📎 launches the system file picker, then a share chooser; users must select the destination app.

- Android Settings: list all installed IMEs with enabled/disabled state; tap enabled opens Android system picker, tap disabled opens Input Method Settings. User selects keyboard in OS dialog.
- Simulator Settings: "Choose keyboard" opens clearly labeled demo options only. The web page cannot read Android-installed keyboards or activate another IME.

- Test Profile Manager → new profile → swap keys → close/reopen → import web-exported JSON → ensure Space types a real space.
- Enable per-app mapping: use package `com.termux`, assign a dedicated profile, verify it activates only in that app.
- Toggle Fn, F1–F12 / Insert / Delete, Shift+arrow selection, long-press Backspace and optional feedback.
- Named snippets: save title + text, tap title from keyboard ⭐ menu, verify full text is inserted.
