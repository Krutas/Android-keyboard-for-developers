package lt.krutas.devboard

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Saved snippets are distinct from the optional system clipboard history. */
class SnippetsActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var field: EditText
    private var editing = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(20, 28, 44)) }
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 30, 18, 20) }
        scroll.addView(root); setContentView(scroll); redraw()
    }
    private fun redraw() {
        root.removeAllViews()
        root.addView(TextView(this).apply { text = "⭐ Saved snippets"; textSize = 23f; setTextColor(Color.WHITE) })
        field = EditText(this).apply {
            hint = "Text to insert with one tap"; minLines = 2
            setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY)
        }
        root.addView(field)
        root.addView(Button(this).apply { text = "Save snippet"; setOnClickListener {
            val value = field.text.toString().take(2000)
            if (value.isNotBlank()) {
                val items = SettingsStore.snippets(this@SnippetsActivity)
                if (editing in items.indices) items[editing] = value else items.add(0, value)
                SettingsStore.saveSnippets(this@SnippetsActivity, items.take(30)); editing = -1; redraw()
            }
        } })
        SettingsStore.snippets(this).forEachIndexed { index, value ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            line.addView(Button(this).apply { text = value.take(44); isAllCaps = false; setOnClickListener {
                editing = index; field.setText(value)
            } }, LinearLayout.LayoutParams(0, -2, 1f))
            line.addView(Button(this).apply { text = "✕"; setOnClickListener {
                val all = SettingsStore.snippets(this@SnippetsActivity)
                all.removeAt(index); SettingsStore.saveSnippets(this@SnippetsActivity, all); editing = -1; redraw()
            } })
            root.addView(line)
        }
        root.addView(Button(this).apply { text = "Done"; setOnClickListener { finish() } })
    }
}
