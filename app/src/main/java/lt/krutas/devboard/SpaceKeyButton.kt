package lt.krutas.devboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import kotlin.math.roundToInt

/** One Space touch target; brackets are decoration in both the IME and preview. */
internal class SpaceKeyButton(context: Context) : Button(context) {
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun configureLabels(maxFontSp: Int) {
        setSingleLine(true)
        // Single-line TextView otherwise uses a huge scrolling text layout. Its
        // centering scroll moves custom edge drawing outside the visible key.
        setHorizontallyScrolling(false)
        gravity = Gravity.CENTER
        includeFontPadding = false
        val labelPadding = (26 * resources.displayMetrics.density).roundToInt()
        setPadding(labelPadding, 0, labelPadding, 0)
        setAutoSizeTextTypeUniformWithConfiguration(8, maxFontSp.coerceAtLeast(9), 1, TypedValue.COMPLEX_UNIT_SP)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        edgePaint.color = currentTextColor
        edgePaint.typeface = typeface
        edgePaint.textSize = textSize
        val inset = resources.displayMetrics.density * 12f
        val baseline = scrollY + height / 2f - (edgePaint.ascent() + edgePaint.descent()) / 2f
        edgePaint.textAlign = Paint.Align.LEFT
        canvas.drawText("<", scrollX + inset, baseline, edgePaint)
        edgePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(">", scrollX + width - inset, baseline, edgePaint)
    }
}
