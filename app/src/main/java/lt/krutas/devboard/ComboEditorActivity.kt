package lt.krutas.devboard

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
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
        main.addView(TextView(this).apply {
            text = "+ COMBO · Saved shortcuts"; textSize = 21f; setTextColor(Color.WHITE)
        })
        main.addView(TextView(this).apply {
            text = "Supported: Ctrl+A/C/X/V/Z/Y/S, Alt+Tab, Esc, Tab or type:your text. Some shortcuts depend on the receiving app."
            textSize = 12f; setTextColor(Color.LTGRAY); setPadding(0, size(9), 0, size(15))
        })
        labelField = EditText(this).apply { hint = "Button title (e.g. Copy)"; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        comboField = EditText(this).apply { hint = "Action (e.g. Ctrl+C)"; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        main.addView(labelField); main.addView(comboField)
        main.addView(Button(this).apply { text = "Save combo"; setOnClickListener { save() } })
        main.addView(Button(this).apply { text = "Done"; setOnClickListener { finish() } })
        SettingsStore.combos(this).forEachIndexed { index, combo ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            line.addView(Button(this).apply {
                text = "${combo.label}  ·  ${combo.action}"; isAllCaps = false
                setOnClickListener { editing = index; labelField.setText(combo.label); comboField.setText(combo.action) }
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
        val combos = SettingsStore.combos(this)
        if (editing >= 0 && editing < combos.size) combos[editing] = Combo(title, action, combos[editing].enabled)
        else combos.add(Combo(title, action, true))
        SettingsStore.saveCombos(this, combos.take(24))
        editing = -1; render()
    }
}
