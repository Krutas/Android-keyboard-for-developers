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
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.InputType
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.GridView
import android.widget.BaseAdapter
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.ScrollView
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
    private var capsLock = false
    private var lastShiftTap = 0L
    private var punctuationPopup: PopupWindow? = null
    private var deleteRepeated = false
    private var repeatingDelete = false
    private val repeatHandler = Handler(Looper.getMainLooper())
    private val repeatDeletion = object : Runnable {
        override fun run() {
            if (!repeatingDelete) return
            deleteRepeated = true
            deleteBackward()
            repeatHandler.postDelayed(this, 65L)
        }
    }
    private var symbols = 0
    private var passwordField = false
    private var mic: SpeechRecognizer? = null
    private var listening = false
    private var micPanelVisible = false
    private var micStatus = "🎙 Pasiruošęs"
    private var micStatusView: TextView? = null
    private var emojiOpen = false
    private var emojiCategory = 1
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
            setPadding(dp(3), dp(5), dp(3), dp(17))
        }
        render()
        // Let the final row scroll into view on phones with limited IME height.
        return ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            addView(container, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lastInput = info
        ctrl = false; alt = false; shifted = false; capsLock = false; symbols = 0; emojiOpen = false
        val variation = (info?.inputType ?: 0) and InputType.TYPE_MASK_VARIATION
        passwordField = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        shifted = shouldCapitalize()
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
        punctuationPopup?.dismiss()
        punctuationPopup = null
        repeatingDelete = false
        repeatHandler.removeCallbacks(repeatDeletion)
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
        action("+ Combo", 68) { startActivity(Intent(this, ComboEditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        action("Fn", 34) {
            val prefs = SettingsStore.prefs(this)
            prefs.edit().putBoolean("function_keys", !prefs.getBoolean("function_keys", false)).apply()
            render()
        }
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


    private fun micPanel(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(5), dp(8), dp(5))
            background = shape(palette.specialColor())
        }
        val micIcon = TextView(this).apply {
            text = "🎙"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(palette.textColor())
        }
        panel.addView(micIcon, LinearLayout.LayoutParams(dp(43), dp(43)))
        micStatusView = TextView(this).apply {
            text = micStatus
            textSize = 12f
            maxLines = 2
            setTextColor(palette.textColor())
        }
        panel.addView(micStatusView, LinearLayout.LayoutParams(0, dp(48), 1f))
        fun control(label: String, onTap: () -> Unit) {
            val b = Button(this).apply {
                text = label
                isAllCaps = false
                textSize = 12f
                setTextColor(palette.textColor())
                background = shape(palette.keyColor())
                minHeight = 0; minimumHeight = 0
                setPadding(dp(3), 0, dp(3), 0)
                setOnClickListener { onTap() }
            }
            panel.addView(b, LinearLayout.LayoutParams(dp(68), dp(41)))
        }
        control("Baigti") {
            if (listening) {
                updateMicStatus("🎙 Atpažįstu...")
                mic?.stopListening()
            }
        }
        control("Atšaukti") { stopMic() }
        return panel
    }

    private fun emojiPanel(): View {
        val groups = EmojiCatalog.groups(this)
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val close = Button(this).apply {
            text = "ABC"
            isAllCaps = false
            textSize = 14f
            setTextColor(palette.textColor())
            background = shape(palette.specialColor())
            setOnClickListener { emojiOpen = false; render() }
        }
        controls.addView(close, LinearLayout.LayoutParams(0, dp(39), 2f))
        val heading = TextView(this).apply {
            text = "Emoji"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(palette.textColor())
        }
        controls.addView(heading, LinearLayout.LayoutParams(0, dp(39), 4f))
        val backspace = Button(this).apply {
            text = "⌫"
            textSize = 19f
            setTextColor(palette.textColor())
            background = shape(palette.specialColor())
            setOnClickListener { deleteBackward() }
        }
        controls.addView(backspace, LinearLayout.LayoutParams(0, dp(39), 2f))
        panel.addView(controls)

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val grid = GridView(this).apply {
            numColumns = 8
            verticalSpacing = dp(2)
            horizontalSpacing = dp(1)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            isVerticalScrollBarEnabled = false
            setBackgroundColor(palette.bg())
        }
        fun show(category: Int) {
            emojiCategory = category
            val options = if (category == 0) EmojiCatalog.recent(this) else groups[category - 1].items
            heading.text = if (category == 0) "Neseniai naudoti" else groups[category - 1].name
            grid.adapter = object : BaseAdapter() {
                override fun getCount(): Int = options.size
                override fun getItem(position: Int): Any = options[position]
                override fun getItemId(position: Int): Long = position.toLong()
                override fun getView(position: Int, old: View?, parent: ViewGroup): View {
                    val cell = old as? TextView ?: TextView(this@DevBoardImeService).apply {
                        gravity = Gravity.CENTER
                        textSize = 25f
                        setTextColor(palette.textColor())
                    }
                    cell.text = options[position].value
                    cell.contentDescription = options[position].name
                    return cell
                }
            }
            grid.setSelection(0)
            for (index in 0 until tabs.childCount) {
                (tabs.getChildAt(index) as Button).alpha = if (index == category) 1f else 0.6f
            }
        }
        val tabIcons = listOf("🕘") + groups.map { it.icon }
        tabIcons.forEachIndexed { index, icon ->
            val tab = Button(this).apply {
                text = icon
                textSize = 21f
                minWidth = 0
                minimumWidth = 0
                setPadding(0, 0, 0, 0)
                background = shape(palette.specialColor())
                setOnClickListener { show(index) }
            }
            tabs.addView(tab, LinearLayout.LayoutParams(dp(43), dp(39)).apply {
                setMargins(dp(1), dp(1), dp(1), dp(1))
            })
        }
        panel.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(tabs)
        })
        grid.onItemClickListener = android.widget.AdapterView.OnItemClickListener { _, _, index, _ ->
            val options = if (emojiCategory == 0) EmojiCatalog.recent(this)
                          else groups[emojiCategory - 1].items
            val emoji = options.getOrNull(index)?.value ?: return@OnItemClickListener
            currentInputConnection?.commitText(emoji, 1)
            EmojiCatalog.remember(this, emoji)
            if (!capsLock) shifted = shouldCapitalize()
        }
        panel.addView(grid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(239)))
        show(if (emojiCategory == 0 && EmojiCatalog.recent(this).isEmpty()) 1 else emojiCategory)
        return panel
    }

    private fun updateMicStatus(status: String) {
        micStatus = status
        micStatusView?.text = status
        micIndicator?.text = if (listening) "🎙 LIVE" else if (SettingsStore.language(this) == "lt-LT") "LT" else "EN"
    }

    private fun activeModifiers(): Int =
        (if (ctrl) KeyEvent.META_CTRL_ON else 0) or
        (if (alt) KeyEvent.META_ALT_ON else 0) or
        (if (shifted || capsLock) KeyEvent.META_SHIFT_ON else 0)

    private fun releaseModifiers() {
        ctrl = false; alt = false; shifted = false
        render()
    }

    private fun functionStrip(): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val names = (1..12).map { "F" + it } + listOf("Ins", "Del")
        names.forEachIndexed { index, label ->
            val code = when (label) {
                "Ins" -> KeyEvent.KEYCODE_INSERT
                "Del" -> KeyEvent.KEYCODE_FORWARD_DEL
                else -> KeyEvent.KEYCODE_F1 + index
            }
            val b = button(label, true, index)
            b.setOnClickListener {
                if (SettingsStore.prefs(this).getBoolean("haptic", false))
                    b.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                hardware(code, activeModifiers())
                releaseModifiers()
            }
            row.addView(b, LinearLayout.LayoutParams(dp(50), dp(31)).apply {
                setMargins(dp(2), dp(1), dp(2), dp(3))
            })
        }
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
    }

    private fun deleteBackward() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) ic.commitText("", 1)
        else ic.deleteSurroundingTextInCodePoints(1, 0)
    }

    private fun render() {
        if (!::container.isInitialized) return
        palette = SettingsStore.theme(this)
        container.setBackgroundColor(palette.bg())
        container.removeAllViews()
        container.addView(bar())
        if (micPanelVisible) container.addView(micPanel())
        if (emojiOpen) {
            container.addView(emojiPanel())
            return
        }
        if (SettingsStore.prefs(this).getBoolean("function_keys", false)) container.addView(functionStrip())
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
                val special = index < 2 || label.length > 1 || label in listOf("⇧", "⌫", "↵", "🎙", "😀")
                val b = button(label, special, index * 13 + col)
                b.isSoundEffectsEnabled = SettingsStore.prefs(this).getBoolean("sound", false)
                b.setOnClickListener {
                    if (label == "⌫" && deleteRepeated) { deleteRepeated = false; return@setOnClickListener }
                    if (SettingsStore.prefs(this).getBoolean("haptic", false)) {
                        b.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    keyPressed(label)
                }
                if (label == "⌫") {
                    b.setOnTouchListener { _, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                deleteRepeated = false
                                repeatingDelete = true
                                repeatHandler.postDelayed(repeatDeletion, 450L)
                            }
                            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                                repeatingDelete = false
                                repeatHandler.removeCallbacks(repeatDeletion)
                            }
                        }
                        false
                    }
                }
                if (index == 2 && SettingsStore.language(this) == "lt-LT" && symbols == 0) {
                    SettingsStore.ltNumbers[label]?.let { letter ->
                        b.setOnLongClickListener { v -> showLetterPopup(v, letter); true }
                    }
                }
                if (label == "Space") b.setOnLongClickListener { v -> showLanguagePopup(v); true }
                if (label == "," || label == ".") {
                    b.setOnLongClickListener { v ->
                        val choices = if (label == ",") listOf(",", ";", ":", "„", "'")
                                      else listOf(".", "?", "!", "…", ":")
                        showPunctuationPopup(v, choices)
                        true
                    }
                }
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
            val digitHint = if (symbols == 0 && SettingsStore.language(this@DevBoardImeService) == "lt-LT")
                SettingsStore.ltNumbers[label] else null
            val effective = if (label.length == 1 && label[0].isLetter()) {
                if (shifted || capsLock) label.uppercase() else label.lowercase()
            } else label
            text = if (digitHint != null) SpannableString("$digitHint\n$label").apply {
                setSpan(RelativeSizeSpan(0.69f), 0, digitHint.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else effective
            if (digitHint != null) {
                setLineSpacing(-dp(3).toFloat(), 1f)
                gravity = Gravity.CENTER
            }
            isAllCaps = false
            textSize = SettingsStore.font(this@DevBoardImeService).toFloat()
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, 0)
            setTextColor(if (palette.colorful) colorful(position) else palette.textColor())
            minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            val selected = (label == "Ctrl" && ctrl) || (label == "Alt" && alt) || (label == "⇧" && (shifted || capsLock))
            background = shape(if (selected) Color.rgb(35, 124, 186) else if (special) palette.specialColor() else palette.keyColor())
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
    private fun showPunctuationPopup(anchor: View, choices: List<String>) {
        punctuationPopup?.dismiss()
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(3), dp(3), dp(3), dp(3))
            background = shape(palette.specialColor())
        }
        choices.forEachIndexed { index, mark ->
            val key = button(mark, true, index).apply {
                textSize = 18f
                setOnClickListener {
                    punctuationPopup?.dismiss()
                    punctuationPopup = null
                    commit(mark)
                }
            }
            strip.addView(key, LinearLayout.LayoutParams(dp(43), dp(42)).apply {
                setMargins(dp(1), 0, dp(1), 0)
            })
        }
        val width = dp(choices.size * 45 + 6)
        val popup = PopupWindow(strip, width, dp(48), true).apply {
            isOutsideTouchable = true
            elevation = dp(5).toFloat()
            setBackgroundDrawable(shape(palette.specialColor()))
        }
        val xy = IntArray(2)
        anchor.getLocationOnScreen(xy)
        val x = (xy[0] + anchor.width / 2 - width / 2)
            .coerceIn(0, (resources.displayMetrics.widthPixels - width).coerceAtLeast(0))
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, (xy[1] - dp(53)).coerceAtLeast(0))
        punctuationPopup = popup
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
    private fun shouldCapitalize(): Boolean {
        if (passwordField || symbols != 0) return false
        if (((lastInput?.inputType ?: 0) and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return false
        // Do not force sentence capitalization into command-line sessions.
        if (lastInput?.packageName == "com.termux") return false
        val previous = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString() ?: return false
        return previous.isBlank() || Regex("[.!?][\\s]+$").containsMatchIn(previous)
    }
    private fun commit(text: String) {
        currentInputConnection?.commitText(text, 1) ?: return
        if (!capsLock) shifted = shouldCapitalize()
        if (::container.isInitialized) render()
    }
    private fun hardware(key: Int, mods: Int = 0) {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, key, 0, mods))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, key, 0, mods))
    }

    private fun keyPressed(value: String) {
        val key = value.lowercase()
        when (value) {
            "Ctrl" -> { ctrl = !ctrl; render(); return }
            "Alt" -> { alt = !alt; render(); return }
            "⇧" -> {
                val now = SystemClock.uptimeMillis()
                if (capsLock) {
                    capsLock = false
                    shifted = false
                    lastShiftTap = 0L
                } else if (now - lastShiftTap <= 420L) {
                    capsLock = true
                    shifted = false
                    lastShiftTap = 0L
                } else {
                    shifted = !shifted
                    lastShiftTap = now
                }
                render()
                return
            }
            "⌫" -> { deleteBackward(); return }
            "Space" -> { ctrl = false; alt = false; commit(" "); return }
            "Esc" -> { hardware(KeyEvent.KEYCODE_ESCAPE, activeModifiers()); releaseModifiers(); return }
            "↵", "Enter" -> { hardware(KeyEvent.KEYCODE_ENTER, activeModifiers()); releaseModifiers(); return }
            "Tab" -> { hardware(KeyEvent.KEYCODE_TAB, activeModifiers()); releaseModifiers(); return }
            "Home" -> { hardware(KeyEvent.KEYCODE_MOVE_HOME, activeModifiers()); releaseModifiers(); return }
            "End" -> { hardware(KeyEvent.KEYCODE_MOVE_END, activeModifiers()); releaseModifiers(); return }
            "PgUp" -> { hardware(KeyEvent.KEYCODE_PAGE_UP, activeModifiers()); releaseModifiers(); return }
            "PgDn" -> { hardware(KeyEvent.KEYCODE_PAGE_DOWN, activeModifiers()); releaseModifiers(); return }
            "Ins" -> { hardware(KeyEvent.KEYCODE_INSERT, activeModifiers()); releaseModifiers(); return }
            "Del", "Delete" -> { hardware(KeyEvent.KEYCODE_FORWARD_DEL, activeModifiers()); releaseModifiers(); return }
            "←" -> { hardware(KeyEvent.KEYCODE_DPAD_LEFT, activeModifiers()); releaseModifiers(); return }
            "→" -> { hardware(KeyEvent.KEYCODE_DPAD_RIGHT, activeModifiers()); releaseModifiers(); return }
            "↑" -> { hardware(KeyEvent.KEYCODE_DPAD_UP, activeModifiers()); releaseModifiers(); return }
            "↓" -> { hardware(KeyEvent.KEYCODE_DPAD_DOWN, activeModifiers()); releaseModifiers(); return }
            "123" -> { symbols = 1; render(); return }
            "ABC" -> { symbols = 0; render(); return }
            "1/2" -> { symbols = 2; render(); return }
            "2/2" -> { symbols = 1; render(); return }
            "🎙", "🎙️" -> { startMic(); return }
            "😀", "😊", "Emoji" -> { emojiOpen = !emojiOpen; render(); return }
        }
        val functionMatch = Regex("^F([1-9]|1[0-2])$").matchEntire(value)
        if (functionMatch != null) {
            hardware(KeyEvent.KEYCODE_F1 + functionMatch.groupValues[1].toInt() - 1, activeModifiers())
            releaseModifiers(); return
        }
        if (ctrl || alt) {
            if (ctrl && key in listOf("a", "c", "v", "x", "z", "y")) {
                applyCombo("Ctrl+" + value.uppercase())
            } else {
                val code = if (value.length == 1) KeyEvent.keyCodeFromString("KEYCODE_" + value.uppercase()) else KeyEvent.KEYCODE_UNKNOWN
                if (code != KeyEvent.KEYCODE_UNKNOWN) hardware(code, activeModifiers()) else commit(value)
            }
            releaseModifiers()
            return
        }
        commit(if (shifted || capsLock) value.uppercase() else if (value.length == 1 && value[0].isLetter()) value.lowercase() else value)
        ctrl = false
        alt = false
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
        val popup = PopupMenu(this, micIndicator ?: container)
        if (savedOnly) {
            val saved = SettingsStore.snippetEntries(this)
            if (saved.isEmpty()) { toast("No snippets. Create one in Settings."); return }
            saved.take(20).forEach { item ->
                popup.menu.add(item.title.take(42)).setOnMenuItemClickListener {
                    commit(item.text)
                    true
                }
            }
        } else {
            val current = clipboard.primaryClip?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
            val historical = SettingsStore.clipboardHistory(this).filter { it != current }
            val texts = (listOf(current) + historical).filter { it.isNotBlank() }
            if (texts.isEmpty()) { toast("Clipboard is empty"); return }
            texts.take(15).forEach { text ->
                popup.menu.add(text.take(42).replace('\n', ' ')).setOnMenuItemClickListener {
                    commit(text)
                    true
                }
            }
        }
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
        if (listening) {
            micPanelVisible = true
            render()
            return
        }
        if (passwordField) { toast("Diktavimas negalimas slaptažodžio lauke"); return }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, MicPermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("Telefone nėra kalbos atpažinimo paslaugos")
            return
        }
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
            micPanelVisible = true
            updateMicStatus("🎙 Kalbėkite...")
            render()
            mic?.startListening(intent)
        } catch (error: Exception) {
            listening = false
            micPanelVisible = false
            updateMicStatus("Mikrofono klaida")
            render()
            toast(error.message ?: "Nepavyko paleisti mikrofono")
        }
    }

    /** Cancels recording without inserting a late speech recognition result. */
    private fun stopMic() {
        if (listening) mic?.cancel()
        listening = false
        micPanelVisible = false
        micStatusView = null
        updateMicStatus("🎙 Pasiruošęs")
        if (::container.isInitialized) render()
    }

    override fun onReadyForSpeech(params: Bundle?) {
        if (listening) updateMicStatus("🎙 Kalbėkite...")
    }
    override fun onBeginningOfSpeech() {
        if (listening) updateMicStatus("🎙 Klausau...")
    }
    override fun onRmsChanged(rmsdB: Float) {
        if (!listening) return
        val count = (rmsdB.coerceIn(0f, 10f) / 2f).roundToInt().coerceIn(1, 5)
        updateMicStatus("🎙 " + "▮".repeat(count) + "▯".repeat(5 - count))
    }
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        if (listening) updateMicStatus("🎙 Atpažįstu...")
    }
    override fun onError(error: Int) {
        if (!listening) return // Ignore callbacks after the user cancels.
        listening = false
        micPanelVisible = false
        micStatusView = null
        render()
        toast("Diktavimo klaida $error. Patikrinkite kalbą ir mikrofoną.")
    }
    override fun onResults(results: Bundle?) {
        if (!listening) return // A cancelled session must never insert text.
        listening = false
        micPanelVisible = false
        micStatusView = null
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (text.isNullOrBlank()) {
            render()
            toast("Kalba neatpažinta")
            return
        }
        if (SettingsStore.language(this) == "lt-LT" && Regex("[\\u0400-\\u052F]").containsMatchIn(text)) {
            render()
            toast("LT režime kirilicos tekstas neįterpiamas")
        } else {
            commit(text + " ")
        }
    }
    override fun onPartialResults(partialResults: Bundle?) {
        if (!listening) return
        val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
        updateMicStatus("🎙 " + text.take(32))
    }
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
