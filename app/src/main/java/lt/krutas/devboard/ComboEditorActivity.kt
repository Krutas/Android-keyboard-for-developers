package lt.krutas.devboard

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.view.KeyEvent
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class ComboEditorActivity : Activity() {
    private lateinit var main: LinearLayout
    private lateinit var labelField: EditText
    private lateinit var comboField: EditText
    private var editing: Int = -1
    private fun size(dp: Int): Int = (resources.displayMetrics.density * dp).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = ScrollView(this).apply { setBackgroundColor(Color.rgb(16, 24, 40)) }
        main = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(size(15), size(25), size(15), size(20)) }
        view.addView(main); setContentView(view); render()
    }
    private fun render() {
        main.removeAllViews()
        val recorder = ShortcutRecorder()
        main.addView(TextView(this).apply {
            text = "+ COMBO · Saved shortcuts"; textSize = 21f; setTextColor(Color.WHITE)
        })
        main.addView(TextView(this).apply {
            text = "Action: tap the keys below in sequence, or record a physical shortcut in the Action field. Text actions accept type:your text. Receiving apps decide which shortcuts they handle."
            textSize = 12f; setTextColor(Color.LTGRAY); setPadding(0, size(9), 0, size(15))
        })
        labelField = EditText(this).apply { hint = "Button title (e.g. Copy)"; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        comboField = EditText(this).apply { hint = "Action (e.g. Ctrl+C)"; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        comboField.showSoftInputOnFocus = false
        comboField.setOnKeyListener { _, code, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener true
            if (code in listOf(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT, KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT)) {
                val modifier = when (code) {
                    KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> "Ctrl"
                    KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> "Alt"
                    else -> "Shift"
                }
                comboField.setText(listOfNotNull(
                    if (event.isCtrlPressed || modifier == "Ctrl") "Ctrl" else null,
                    if (event.isAltPressed || modifier == "Alt") "Alt" else null,
                    if (event.isShiftPressed || modifier == "Shift") "Shift" else null
                ).joinToString("+"))
                return@setOnKeyListener true
            }
            val key = when (code) {
                KeyEvent.KEYCODE_DPAD_LEFT -> "←"
                KeyEvent.KEYCODE_DPAD_RIGHT -> "→"
                KeyEvent.KEYCODE_DPAD_UP -> "↑"
                KeyEvent.KEYCODE_DPAD_DOWN -> "↓"
                KeyEvent.KEYCODE_DEL -> "Backspace"
                KeyEvent.KEYCODE_FORWARD_DEL -> "Delete"
                KeyEvent.KEYCODE_MOVE_HOME -> "Home"
                KeyEvent.KEYCODE_MOVE_END -> "End"
                else -> KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")
            }
            val action = listOfNotNull(if (event.isCtrlPressed) "Ctrl" else null, if (event.isAltPressed) "Alt" else null, if (event.isShiftPressed) "Shift" else null, key).joinToString("+")
            if (Shortcut.parse(action) != null) comboField.setText(action)
            true
        }
        main.addView(labelField); main.addView(comboField)
        val recordKeys = listOf("Ctrl", "Alt", "Shift") + ('A'..'Z').map { it.toString() } + ('0'..'9').map { it.toString() } +
            listOf("Esc", "Tab", "Space", "Enter", "Backspace", "Delete", "Home", "End", "←", "↑", "↓", "→") + (1..8).map { "F$it" }
        recordKeys.chunked(6).forEach { keys ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            keys.forEach { key ->
                row.addView(Button(this).apply {
                    text = key; isAllCaps = false; textSize = 10f; minWidth = 0; minimumWidth = 0; setPadding(0, 0, 0, 0)
                    setOnClickListener {
                        comboField.setText(recorder.press(key))
                    }
                }, LinearLayout.LayoutParams(0, size(42), 1f))
            }
            main.addView(row)
        }
        main.addView(Button(this).apply { text = "Save combo"; setOnClickListener { save() } })
        main.addView(Button(this).apply { text = "Done"; setOnClickListener { finish() } })
        SettingsStore.combos(this).forEachIndexed { index, combo ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            line.addView(Button(this).apply {
                text = "${combo.label}  ·  ${combo.action}"; isAllCaps = false
                setOnClickListener { recorder.reset(); editing = index; labelField.setText(combo.label); comboField.setText(combo.action) }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            line.addView(CheckBox(this).apply {
                isChecked = combo.enabled
                contentDescription = "Show ${combo.label} on keyboard"
                setOnCheckedChangeListener { _, enabled ->
                    val combos = SettingsStore.combos(this@ComboEditorActivity)
                    if (index in combos.indices) {
                        combos[index] = combos[index].copy(enabled = enabled)
                        SettingsStore.saveCombos(this@ComboEditorActivity, combos)
                    }
                }
            })
            line.addView(Button(this).apply {
                text = "✕"; setOnClickListener {
                    val combos = SettingsStore.combos(this@ComboEditorActivity)
                    combos.removeAt(index); SettingsStore.saveCombos(this@ComboEditorActivity, combos)
                    editing = -1; render()
                }
            })
            main.addView(line)
        }
    }
    private fun save() {
        val title = labelField.text.toString().trim().take(15)
        val action = comboField.text.toString().trim().take(150)
        if (title.isBlank() || action.isBlank()) { Toast.makeText(this, "Enter name and action", Toast.LENGTH_SHORT).show(); return }
        if (!action.startsWith("type:", true) && Shortcut.parse(action) == null) {
            Toast.makeText(this, "Choose a supported key and modifiers", Toast.LENGTH_SHORT).show(); return
        }
        val combos = SettingsStore.combos(this)
        if (editing >= 0 && editing < combos.size) combos[editing] = Combo(title, action, combos[editing].enabled)
        else combos.add(Combo(title, action, true))
        SettingsStore.saveCombos(this, combos.take(24))
        editing = -1; render()
    }
}
