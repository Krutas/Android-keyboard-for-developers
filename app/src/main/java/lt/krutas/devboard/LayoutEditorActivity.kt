package lt.krutas.devboard

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.roundToInt

class LayoutEditorActivity : Activity() {
    private lateinit var root: LinearLayout
    private var keys: MutableList<MutableList<String>> = mutableListOf()
    private var selected: Pair<Int, Int>? = null
    private fun dp(n: Int) = (resources.displayMetrics.density * n).roundToInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keys = SettingsStore.rows(this)
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(18, 26, 40)) }
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(5), dp(20), dp(5), dp(15)) }
        scroll.addView(root); setContentView(scroll)
        render()
    }
    private fun render() {
        val theme = SettingsStore.theme(this)
        root.removeAllViews()
        root.addView(TextView(this).apply {
            text = "EDIT LAYOUT · Tap first key, then destination to swap"
            textSize = 16f; setTextColor(Color.WHITE); setPadding(dp(9), dp(6), dp(9), dp(13))
        })
        keys.forEachIndexed { ri, row ->
            val strip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            row.forEachIndexed { ci, text ->
                val picked = selected == (ri to ci)
                val b = Button(this).apply {
                    this.text = text; isAllCaps = false; textSize = 11f
                    minWidth = 0; minHeight = 0; minimumWidth = 0; minimumHeight = 0
                    setPadding(0, 0, 0, 0)
                    setTextColor(theme.textColor())
                    setBackgroundColor(if (picked) Color.rgb(56, 105, 203) else theme.keyColor())
                    setOnClickListener {
                        val previous = selected
                        if (previous == null) selected = ri to ci
                        else if (previous == (ri to ci)) selected = null
                        else {
                            val old = keys[previous.first][previous.second]
                            keys[previous.first][previous.second] = keys[ri][ci]
                            keys[ri][ci] = old
                            SettingsStore.saveRows(this@LayoutEditorActivity, keys)
                            selected = null
                        }
                        render()
                    }
                }
                strip.addView(b, LinearLayout.LayoutParams(0, dp(43), if (text == "Space") 3f else 1f).apply {
                    setMargins(dp(1), dp(3), dp(1), dp(3))
                })
            }
            root.addView(strip)
        }
        root.addView(Button(this).apply {
            text = "Reset to defaults"
            setOnClickListener { keys = SettingsStore.copyDefault(); SettingsStore.saveRows(this@LayoutEditorActivity, keys); selected = null; render() }
        })
        root.addView(Button(this).apply { text = "Done"; setOnClickListener { finish() } })
    }
}
