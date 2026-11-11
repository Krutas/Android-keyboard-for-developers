package lt.krutas.devboard

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Saved text snippets persist locally and never become clipboard history. */
class SnippetsActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var textField: EditText
    private var editing = -1
    private fun dp(value: Int) = (resources.displayMetrics.density * value).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(20, 28, 44)) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(25), dp(15), dp(20))
        }
        scroll.addView(root)
        setContentView(scroll)
        redraw()
    }

    private fun redraw() {
        root.removeAllViews()
        root.addView(TextView(this).apply {
            text = "⭐ Saved snippets"
            textSize = 22f
            setTextColor(Color.WHITE)
        })
        root.addView(TextView(this).apply {
            text = "Save text to insert with one tap from the ⭐ keyboard menu."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(7), 0, dp(11))
        })
        textField = EditText(this).apply {
            hint = "Text to insert"
            minLines = 3
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        root.addView(textField)
        root.addView(Button(this).apply {
            text = "Save snippet"
            isAllCaps = false
            setOnClickListener {
                val value = textField.text.toString().take(2000)
                if (value.isBlank()) {
                    Toast.makeText(this@SnippetsActivity, "Enter text to insert", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val items = SettingsStore.snippetEntries(this@SnippetsActivity)
                val title = value.trim().replace(Regex("\\s+"), " ").take(50)
                val saved = SavedSnippet(title, value)
                if (editing in items.indices) items[editing] = saved
                else if (items.size < 30) items.add(0, saved)
                else {
                    Toast.makeText(this@SnippetsActivity, "Maximum 30 snippets", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                SettingsStore.saveSnippetEntries(this@SnippetsActivity, items)
                editing = -1
                redraw()
            }
        })
        SettingsStore.snippetEntries(this).forEachIndexed { index, snippet ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            line.addView(Button(this).apply {
                text = snippet.text.replace(Regex("\\s+"), " ").take(85)
                isAllCaps = false
                setOnClickListener {
                    editing = index
                    textField.setText(snippet.text)
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            line.addView(Button(this).apply {
                text = "✕"
                setOnClickListener {
                    AlertDialog.Builder(this@SnippetsActivity)
                        .setTitle("Delete snippet?")
                        .setMessage(snippet.title)
                        .setPositiveButton("Delete") { _, _ ->
                            val items = SettingsStore.snippetEntries(this@SnippetsActivity)
                            if (index in items.indices) items.removeAt(index)
                            SettingsStore.saveSnippetEntries(this@SnippetsActivity, items)
                            editing = -1
                            redraw()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            })
            root.addView(line)
        }
        root.addView(Button(this).apply {
            text = "Done"
            setOnClickListener { finish() }
        })
    }
}
