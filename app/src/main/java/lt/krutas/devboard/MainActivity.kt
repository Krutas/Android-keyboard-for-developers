package lt.krutas.devboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private lateinit var column: LinearLayout
    private lateinit var keyboardPreview: LinearLayout
    private lateinit var themeLabel: TextView
    private fun px(dp: Int) = (resources.displayMetrics.density * dp).roundToInt()
    private fun button(label: String, color: Int = Color.rgb(42, 80, 132), click: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 13f
        setTextColor(Color.WHITE); setBackgroundColor(color)
        setOnClickListener { click() }
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); draw() }
    override fun onResume() { super.onResume(); if (::keyboardPreview.isInitialized) preview() }
    private fun draw() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(16, 24, 39)) }
        column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(22), px(16), px(26))
        }
        scroll.addView(column); setContentView(scroll)
        title("DEVBOARD  ·  ANDROID KEYBOARD", 22)
        detail("An installable terminal keyboard with local settings. The browser simulator remains the design reference.")
        title("Enable keyboard", 17)
        column.addView(button("1 · Enable DevBoard in Android settings") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        column.addView(button("2 · Choose DevBoard as input method") {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })
        column.addView(button("Edit keyboard layout · tap to swap") {
            startActivity(Intent(this, LayoutEditorActivity::class.java))
        })
        column.addView(button("+ Combo · manage shortcuts") {
            startActivity(Intent(this, ComboEditorActivity::class.java))
        })
        column.addView(button("⭐ Saved snippets · manage text") {
            startActivity(Intent(this, SnippetsActivity::class.java))
        })
        title("Live keyboard preview", 17)
        keyboardPreview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(px(4), px(8), px(4), px(8))
        }
        column.addView(keyboardPreview)
        themeLabel = detail("", false)
        title("Appearance", 17)
        val rail = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val options = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        SettingsStore.themes.forEach { theme ->
            options.addView(button(theme.name, theme.keyColor()) {
                SettingsStore.prefs(this).edit().putString("theme", theme.name).apply()
                preview()
            }.apply { setTextColor(theme.textColor()); textSize = 12f },
                LinearLayout.LayoutParams(px(145), px(52)).apply { setMargins(px(2), px(2), px(6), px(2)) })
        }
        rail.addView(options); column.addView(rail)
        slider("Font size", "font", 10, 22, SettingsStore.font(this))
        slider("Key height (dp)", "height", 30, 54, SettingsStore.keyHeight(this))
        slider("Key spacing (dp)", "gap", 0, 8, SettingsStore.gap(this))
        slider("Corner radius (dp)", "radius", 0, 18, SettingsStore.radius(this))
        checkbox("Terminal shortcut rows", "extras", SettingsStore.showExtras(this))
        checkbox("Save clipboard history while keyboard is active", "clipboard_opt_in", SettingsStore.captureClipboard(this))
        detail("Clipboard history is opt-in. Android restricts background clipboard reads. Password fields are excluded.")
        title("Language & dictation", 17)
        val current = SettingsStore.language(this)
        val language = button("Language: ${if (current == "lt-LT") "Lithuanian (LT)" else "English (EN)"}") {
            val next = if (SettingsStore.language(this) == "lt-LT") "en-US" else "lt-LT"
            SettingsStore.prefs(this).edit().putString("language", next).apply()
            draw()
        }
        column.addView(language)
        detail("Long-press Space to switch languages on the keyboard. Long-press 1–9 for ą č ę ė į š ų ū ž.")
        detail("Microphone uses the installed Android speech recognition service. Permission is requested when you tap 🎙.")
        column.addView(button("Test microphone permission") { startActivity(Intent(this, MicPermissionActivity::class.java)) })
        title("Privacy", 17)
        detail("Offline key input. No analytics or custom network requests. Speech recognition may use the connected recognizer's cloud services. No API key is needed.")
        column.addView(button("Reset keyboard layout", Color.rgb(109, 55, 58)) {
            SettingsStore.prefs(this).edit().remove("keyboard_rows").apply()
            Toast.makeText(this, "Default layout restored", Toast.LENGTH_SHORT).show()
            preview()
        })
        preview()
    }
    private fun title(t: String, size: Int) {
        column.addView(TextView(this).apply {
            text = t; textSize = size.toFloat(); setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, px(14), 0, px(7))
        })
    }
    private fun detail(t: String, attach: Boolean = true): TextView {
        val text = TextView(this).apply {
            text = t; textSize = 12f; setTextColor(Color.rgb(174, 191, 215))
            setPadding(0, px(4), 0, px(9))
        }
        if (attach) column.addView(text) else column.addView(text)
        return text
    }
    private fun slider(name: String, key: String, min: Int, max: Int, initial: Int) {
        val label = detail("$name: $initial")
        val seek = SeekBar(this).apply {
            this.max = max - min
            progress = (initial - min).coerceIn(0, max - min)
        }
        column.addView(seek)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = min + progress
                label.text = "$name: $value"
                if (fromUser) { SettingsStore.prefs(this@MainActivity).edit().putInt(key, value).apply(); preview() }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }
    private fun checkbox(name: String, key: String, value: Boolean) {
        column.addView(CheckBox(this).apply {
            text = name; isChecked = value; setTextColor(Color.WHITE)
            setOnCheckedChangeListener { _, checked ->
                SettingsStore.prefs(this@MainActivity).edit().putBoolean(key, checked).apply(); preview()
            }
        })
    }
    private fun preview() {
        if (!::keyboardPreview.isInitialized) return
        val theme = SettingsStore.theme(this)
        themeLabel.text = "Theme: ${theme.name} · Live preview"
        keyboardPreview.removeAllViews()
        keyboardPreview.setBackgroundColor(theme.bg())
        SettingsStore.rows(this).forEachIndexed { index, row ->
            if (index < 2 && !SettingsStore.showExtras(this)) return@forEachIndexed
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            row.forEachIndexed { indexInRow, label ->
                val chip = TextView(this).apply {
                    text = label
                    gravity = Gravity.CENTER
                    textSize = (SettingsStore.font(this@MainActivity) * .70f).coerceAtLeast(7f)
                    typeface = Typeface.MONOSPACE
                    setTextColor(if (theme.colorful) listOf("#44F8DE", "#FA83A9", "#FFDE60", "#9C96FF").map(Color::parseColor)[indexInRow % 4] else theme.textColor())
                    background = GradientDrawable().apply {
                        setColor(if (index < 2) theme.specialColor() else theme.keyColor())
                        cornerRadius = px(SettingsStore.radius(this@MainActivity)).toFloat()
                        setStroke(px(1), theme.borderColor())
                    }
                }
                line.addView(chip, LinearLayout.LayoutParams(0, px((SettingsStore.keyHeight(this) * .65f).roundToInt()), if (label == "Space") 4f else 1f).apply {
                    setMargins(px(1), px(2), px(1), 0)
                })
            }
            keyboardPreview.addView(line)
        }
    }
}
