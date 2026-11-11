package lt.krutas.devboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

/** A real Android IME. The web simulator is a design tool, not an Android WebView keyboard. */
class DevBoardImeService : InputMethodService(), RecognitionListener {
    private lateinit var container: LinearLayout
    private var palette: Palette = SettingsStore.themes[0]
    private var ctrl = false
    private var alt = false
    private var shifted = false
    private var symbols = 0
    private var passwordField = false
    private var mic: SpeechRecognizer? = null
    private var listening = false
    private var micIndicator: TextView? = null
    private var lastInput: EditorInfo? = null
    private val clipboard by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener { rememberClipboard() }
    private var clipboardListenerAdded = false

    private fun dp(v: Int): Int = (resources.displayMetrics.density * v).roundToInt()
    private fun shape(fill: Int, stroke: Int = palette.borderColor()): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(SettingsStore.radius(this@DevBoardImeService)).toFloat()
            setStroke(dp(1), stroke)
        }

    override fun onCreateInputView(): View {
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3), dp(5), dp(3), dp(7))
        }
        render()
        return container
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lastInput = info
        val variation = (info?.inputType ?: 0) and InputType.TYPE_MASK_VARIATION
        passwordField = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        if (SettingsStore.captureClipboard(this) && !clipboardListenerAdded) {
            clipboard.addPrimaryClipChangedListener(clipboardListener)
            clipboardListenerAdded = true
        } else if (!SettingsStore.captureClipboard(this) && clipboardListenerAdded) {
            clipboard.removePrimaryClipChangedListener(clipboardListener)
            clipboardListenerAdded = false
        }
        if (::container.isInitialized) render()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        stopMic()
        super.onFinishInputView(finishingInput)
    }
    override fun onDestroy() {
        stopMic()
        mic?.destroy()
        mic = null
        if (clipboardListenerAdded) clipboard.removePrimaryClipChangedListener(clipboardListener)
        super.onDestroy()
    }

    private fun bar(): LinearLayout {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(4))
        }
        fun action(label: String, width: Int = 0, run: () -> Unit) {
            val button = Button(this).apply {
                text = label; isAllCaps = false
                setTextColor(palette.textColor())
                textSize = 13f
                minHeight = 0; minimumHeight = 0; minWidth = 0; minimumWidth = 0
                setPadding(dp(2), 0, dp(2), 0)
                background = shape(palette.specialColor())
                setOnClickListener { run() }
            }
            val lp = LinearLayout.LayoutParams(if (width > 0) dp(width) else 0, dp(34), if (width > 0) 0f else 1f)
            lp.setMargins(dp(2), 0, dp(2), 0)
            line.addView(button, lp)
        }
        action("⚙", 39) { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        action("+ Combo", 74) { startActivity(Intent(this, ComboEditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        action("📎", 39) { startActivity(Intent(this, FileShareActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        action("📋", 39) { openClipboard(false) }
        action("⭐", 39) { openClipboard(true) }
        micIndicator = TextView(this).apply {
            text = if (listening) "🎙 LISTENING" else if (SettingsStore.language(this@DevBoardImeService) == "lt-LT") "LT" else "EN"
            textSize = 11f
            setTextColor(palette.textColor())
            gravity = Gravity.CENTER
            setOnClickListener { startMic() }
            background = shape(palette.specialColor())
        }
        line.addView(micIndicator, LinearLayout.LayoutParams(0, dp(34), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        return line
    }

    private fun render() {
        if (!::container.isInitialized) return
        palette = SettingsStore.theme(this)
        container.setBackgroundColor(palette.bg())
        container.removeAllViews()
        container.addView(bar())
        val combos = SettingsStore.combos(this)
        if (combos.isNotEmpty()) {
            val strip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            combos.forEach { combo ->
                val b = button(combo.label, true, 0)
                b.setOnClickListener { applyCombo(combo.action) }
                strip.addView(b, LinearLayout.LayoutParams(dp(92), dp(32)).apply { setMargins(dp(2), 0, dp(2), dp(2)) })
            }
            val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(strip) }
            container.addView(scroll)
        }
        val rows = when (symbols) {
            1 -> SettingsStore.symbols1
            2 -> SettingsStore.symbols2
            else -> SettingsStore.rows(this, lastInput?.packageName)
        }
        rows.forEachIndexed { index, keys ->
            if (!SettingsStore.showExtras(this) && index < 2) return@forEachIndexed
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            keys.forEachIndexed { col, label ->
                val special = index < 2 || label.length > 1 || label in listOf("⇧", "⌫", "↵", "🎙")
                val b = button(label, special, index * 13 + col)
                b.setOnClickListener { keyPressed(label) }
                if (index == 2 && SettingsStore.language(this) == "lt-LT" && symbols == 0) {
                    SettingsStore.ltNumbers[label]?.let { letter ->
                        b.setOnLongClickListener { v -> showLetterPopup(v, letter); true }
                    }
                }
                if (label == "Space") b.setOnLongClickListener { v -> showLanguagePopup(v); true }
                val weight = if (label == "Space") 4f else 1f
                row.addView(b, LinearLayout.LayoutParams(0, dp(SettingsStore.keyHeight(this)), weight).apply {
                    val margin = dp(SettingsStore.gap(this@DevBoardImeService))
                    setMargins(margin, margin, margin, 0)
                })
            }
            container.addView(row)
        }
    }

    private fun button(label: String, special: Boolean, position: Int): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = SettingsStore.font(this@DevBoardImeService).toFloat()
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, 0)
            setTextColor(if (palette.colorful) colorful(position) else palette.textColor())
            minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            background = shape(if (special) palette.specialColor() else palette.keyColor())
            if (label == "Space") contentDescription = "Space. Long press to change language"
        }
    }
    private fun colorful(index: Int): Int = listOf("#3DDCFF", "#FF6DAF", "#F9D44D", "#87FAAC", "#C9A7FF", "#FF9B4C")
        .map(Color::parseColor)[(index % 6 + 6) % 6]

    private fun showLetterPopup(anchor: View, letter: String) {
        val menu = PopupMenu(this, anchor)
        menu.menu.add(letter)
        menu.menu.add(letter.uppercase())
        menu.setOnMenuItemClickListener { item -> commit(item.title.toString()); true }
        menu.show()
    }
    private fun showLanguagePopup(anchor: View) {
        val menu = PopupMenu(this, anchor)
        menu.menu.add("🇱🇹 Lithuanian")
        menu.menu.add("🇬🇧 English")
        menu.setOnMenuItemClickListener { item ->
            SettingsStore.prefs(this).edit().putString("language", if (item.title.toString().contains("Lithuanian")) "lt-LT" else "en-US").apply()
            render(); true
        }
        menu.show()
    }
    private fun commit(text: String) { currentInputConnection?.commitText(text, 1) }
    private fun hardware(key: Int, mods: Int = 0) {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, key, 0, mods))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, key, 0, mods))
    }
    private fun keyPressed(value: String) {
        val key = value.lowercase()
        when (value) {
            "Ctrl" -> { ctrl = !ctrl; toast("Ctrl ${if (ctrl) "ON" else "OFF"}"); return }
            "Alt" -> { alt = !alt; toast("Alt ${if (alt) "ON" else "OFF"}"); return }
            "⇧" -> { shifted = !shifted; return }
            "Esc" -> { hardware(KeyEvent.KEYCODE_ESCAPE); ctrl = false; alt = false; return }
            "⌫" -> { currentInputConnection?.deleteSurroundingTextInCodePoints(1, 0); return }
            "Space" -> { commit(" "); return }
            "↵", "Enter" -> { hardware(KeyEvent.KEYCODE_ENTER); return }
            "Tab" -> { hardware(KeyEvent.KEYCODE_TAB); return }
            "Home" -> { hardware(KeyEvent.KEYCODE_MOVE_HOME); return }
            "End" -> { hardware(KeyEvent.KEYCODE_MOVE_END); return }
            "PgUp" -> { hardware(KeyEvent.KEYCODE_PAGE_UP); return }
            "PgDn" -> { hardware(KeyEvent.KEYCODE_PAGE_DOWN); return }
            "←" -> { hardware(KeyEvent.KEYCODE_DPAD_LEFT); return }
            "→" -> { hardware(KeyEvent.KEYCODE_DPAD_RIGHT); return }
            "↑" -> { hardware(KeyEvent.KEYCODE_DPAD_UP); return }
            "↓" -> { hardware(KeyEvent.KEYCODE_DPAD_DOWN); return }
            "123" -> { symbols = 1; render(); return }
            "ABC" -> { symbols = 0; render(); return }
            "1/2" -> { symbols = 2; render(); return }
            "2/2" -> { symbols = 1; render(); return }
            "🎙", "🎙️" -> { startMic(); return }
        }
        if (ctrl || alt) {
            val mods = (if (ctrl) KeyEvent.META_CTRL_ON else 0) or (if (alt) KeyEvent.META_ALT_ON else 0)
            if (ctrl && key in listOf("a", "c", "v", "x", "z", "y")) {
                applyCombo("Ctrl+${value.uppercase()}")
            } else {
                val code = if (value.length == 1) KeyEvent.keyCodeFromString("KEYCODE_${value.uppercase()}") else KeyEvent.KEYCODE_UNKNOWN
                if (code != KeyEvent.KEYCODE_UNKNOWN) hardware(code, mods) else commit(value)
            }
            ctrl = false; alt = false
            return
        }
        commit(if (shifted) value.uppercase() else if (value.length == 1 && value[0].isLetter()) value.lowercase() else value)
        shifted = false
    }

    private fun applyCombo(raw: String) {
        val cmd = raw.trim().uppercase().replace(" ", "")
        val ic = currentInputConnection ?: return
        when (cmd) {
            "CTRL+A" -> ic.performContextMenuAction(android.R.id.selectAll)
            "CTRL+C" -> ic.performContextMenuAction(android.R.id.copy)
            "CTRL+X" -> ic.performContextMenuAction(android.R.id.cut)
            "CTRL+V" -> ic.performContextMenuAction(android.R.id.paste)
            "CTRL+Z" -> hardware(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON)
            "CTRL+Y" -> hardware(KeyEvent.KEYCODE_Y, KeyEvent.META_CTRL_ON)
            "ESC" -> hardware(KeyEvent.KEYCODE_ESCAPE)
            "TAB" -> hardware(KeyEvent.KEYCODE_TAB)
            "CTRL+S" -> hardware(KeyEvent.KEYCODE_S, KeyEvent.META_CTRL_ON)
            "ALT+TAB" -> hardware(KeyEvent.KEYCODE_TAB, KeyEvent.META_ALT_ON)
            else -> if (raw.startsWith("type:", true)) commit(raw.substring(5)) else {
                toast("Unsupported combo: $raw")
            }
        }
    }

    private fun openClipboard(savedOnly: Boolean) {
        if (passwordField) { toast("Clipboard unavailable in password fields"); return }
        val recent = if (savedOnly) SettingsStore.snippets(this) else {
            val current = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
            val historical = SettingsStore.clipboardHistory(this).filter { it != current }
            (listOf(current) + historical).filter { it.isNotBlank() }
        }
        if (recent.isEmpty()) { toast("Clipboard is empty. Save snippets in Settings."); return }
        val popup = PopupMenu(this, micIndicator ?: container)
        recent.take(15).forEach { s -> popup.menu.add(s.take(42).replace('\n', ' ')).setOnMenuItemClickListener { commit(s); true } }
        popup.show()
    }
    private fun rememberClipboard() {
        if (!SettingsStore.captureClipboard(this) || passwordField) return
        val item = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0) ?: return
        val value = item.coerceToText(this).toString()
        if (value.isBlank() || value.length > 1000) return
        val items = SettingsStore.clipboardHistory(this)
        items.remove(value); items.add(0, value)
        SettingsStore.saveClipboardHistory(this, items.take(20))
    }
    private fun toast(s: String) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show() }

    private fun startMic() {
        if (listening) { stopMic(); return }
        if (passwordField) { toast("Dictation disabled in password fields"); return }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, MicPermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { toast("No speech recognition service installed"); return }
        try {
            mic?.destroy()
            mic = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, SettingsStore.language(this@DevBoardImeService))
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, SettingsStore.language(this@DevBoardImeService))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            listening = true
            micIndicator?.text = "🎙 ▂▂▂▂"
            mic?.startListening(intent)
        } catch (error: Exception) {
            listening = false
            micIndicator?.text = "MIC ERROR"
            toast(error.message ?: "Speech service failed")
        }
    }
    private fun stopMic() {
        if (listening) mic?.stopListening()
        listening = false
        micIndicator?.text = if (SettingsStore.language(this) == "lt-LT") "LT" else "EN"
    }
    override fun onReadyForSpeech(params: Bundle?) { micIndicator?.text = "🎙 READY" }
    override fun onBeginningOfSpeech() { micIndicator?.text = "🎙 LISTENING" }
    override fun onRmsChanged(rmsdB: Float) {
        val count = (rmsdB.coerceIn(0f, 10f) / 2f).roundToInt().coerceIn(1, 5)
        micIndicator?.text = "🎙 " + "▮".repeat(count) + "▯".repeat(5 - count)
    }
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { micIndicator?.text = "🎙 PROCESSING" }
    override fun onError(error: Int) { listening = false; micIndicator?.text = "MIC ERROR"; toast("Dictation error $error. Check language, microphone and connection.") }
    override fun onResults(results: Bundle?) {
        listening = false
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (text.isNullOrBlank()) { micIndicator?.text = "NO SPEECH"; return }
        if (SettingsStore.language(this) == "lt-LT" && Regex("[\\u0400-\\u052F]").containsMatchIn(text)) {
            micIndicator?.text = "LANGUAGE ERROR"; toast("Cyrillic blocked in LT mode")
        } else { commit(text + " "); micIndicator?.text = "✓ DONE" }
    }
    override fun onPartialResults(partialResults: Bundle?) {
        val value = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
        micIndicator?.text = "🎙 ${value.take(14)}"
    }
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
