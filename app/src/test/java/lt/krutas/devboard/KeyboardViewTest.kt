package lt.krutas.devboard

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPopupMenu
import org.robolectric.util.ReflectionHelpers
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KeyboardViewTest {
    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()

    private fun key(root: View, label: String): Button = descendants(root).filterIsInstance<Button>().single { it.text.toString() == label }
    private fun hasKey(root: View, label: String) = descendants(root).filterIsInstance<Button>().any { it.text.toString() == label }

    private fun layout(root: View, width: Int = 360, height: Int = 600) {
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        root.viewTreeObserver.dispatchOnPreDraw()
    }

    private class RecordingConnection(context: Context) : BaseInputConnection(View(context), true) {
        val committed = StringBuilder()
        val events = mutableListOf<KeyEvent>()
        var deletions = 0
        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean { committed.append(text); return true }
        override fun sendKeyEvent(event: KeyEvent): Boolean { events.add(event); return true }
        override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence = committed.takeLast(n).toString()
        override fun getSelectedText(flags: Int): CharSequence? = null
        override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean { deletions++; return true }
    }

    private fun withKeyboard(configure: (DevBoardImeService) -> Unit = {}, test: (DevBoardImeService, View, RecordingConnection) -> Unit) {
        val controller = Robolectric.buildService(DevBoardImeService::class.java).create()
        val service = controller.get()
        configure(service)
        val connection = RecordingConnection(service)
        // Supply the editor connection that the Android IME manager normally binds.
        ReflectionHelpers.setField(service, "mInputConnection", connection)
        val root = service.onCreateInputView()
        val host = Robolectric.buildActivity(Activity::class.java).setup()
        host.get().setContentView(root)
        shadowOf(Looper.getMainLooper()).idle()
        service.onStartInputView(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "com.termux"
        }, false)
        layout(root)
        try { test(service, root, connection) }
        finally { controller.destroy(); host.pause().stop().destroy() }
    }

    private fun screenshot(view: View, name: String): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // A parent applies this translation when drawing its scrolled child.
        canvas.translate(-view.scrollX.toFloat(), -view.scrollY.toFloat())
        view.draw(canvas)
        val file = File("build/reports/keyboard/$name.png").apply { parentFile!!.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    private fun assertSpacePixels(space: Button, name: String) {
        val bitmap = screenshot(space, name)
        fun textPixels(left: Int, right: Int): Int = (left until right).sumOf { x ->
            (3 until bitmap.height - 3).count { y ->
                val color = bitmap.getPixel(x, y)
                Color.red(color) > 180 && Color.green(color) > 180 && Color.blue(color) > 180
            }
        }
        assertTrue("Left < must be visible: $name", textPixels(5, 25) > 3)
        assertTrue("Right > must be visible: $name", textPixels(bitmap.width - 25, bitmap.width - 5) > 3)
        assertTrue("Space label must be visible: $name", textPixels(26, bitmap.width - 26) > 10)
        assertEquals("Space must not horizontally scroll its decorations", 0, space.scrollX)
        assertEquals(1, space.lineCount)
    }

    private fun touch(view: View, action: Int, x: Float = view.width / 2f) {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x, view.height / 2f, 0)
        view.dispatchTouchEvent(event)
        event.recycle()
        if (action == MotionEvent.ACTION_UP) shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun spaceBracketsAndCenterFitAllSymbolPagesAndNarrowWidths() = withKeyboard({
        SettingsStore.prefs(it).edit().putInt("font", 22).putInt("height", 30).putInt("gap", 8).apply()
    }) { _, root, _ ->
        for ((page, next) in listOf("letters" to "123", "symbols1" to "1/2", "symbols2" to "ABC")) {
            for (width in listOf(320, 360, 720)) {
                layout(root, width)
                assertSpacePixels(key(root, "Space"), "space-$page-$width")
                screenshot(root, "keyboard-$page-$width")
            }
            key(root, next).performClick()
        }
    }

    @Test fun spaceEdgesInsertOneSpaceAndHoldSelectsLanguage() = withKeyboard { service, root, connection ->
        assertSpacePixels(key(root, "Space"), "space-default")
        screenshot(root, "keyboard-default")
        for (next in listOf("123", "1/2", "ABC")) {
            for (fraction in listOf(.08f, .5f, .92f)) {
                layout(root)
                val space = key(root, "Space")
                touch(space, MotionEvent.ACTION_DOWN, space.width * fraction)
                touch(space, MotionEvent.ACTION_UP, space.width * fraction)
            }
            key(root, next).performClick()
        }
        assertEquals(" ".repeat(9), connection.committed.toString())
        layout(root)
        assertTrue(key(root, "Space").performLongClick())
        val popup = ShadowPopupMenu.getLatestPopupMenu()
        shadowOf(popup).onMenuItemClickListener.onMenuItemClick(popup.menu.getItem(1))
        assertEquals("en-US", SettingsStore.language(service))
        assertEquals(" ".repeat(9), connection.committed.toString())
    }

    @Test fun functionKeysStayVisibleAtBottomOfTallLayoutAndDispatchModifiers() = withKeyboard({
        SettingsStore.prefs(it).edit().putInt("font", 22).putInt("height", 54).putInt("gap", 8).apply()
    }) { service, root, connection ->
        layout(root, 320, 260)
        val scroll = descendants(root).filterIsInstance<ScrollView>().single()
        scroll.scrollTo(0, 10000)
        key(root, "Fn").performClick()
        layout(root, 320, 260)
        assertTrue(SettingsStore.prefs(service).getBoolean("function_keys", false))
        assertTrue(key(root, "Fn").isSelected)
        scroll.scrollTo(0, 10000)
        screenshot(root, "fn-scrolled-320")
        for (n in 1..8) {
            layout(root, 320, 260)
            val button = key(root, "F$n")
            val xy = IntArray(2); val origin = IntArray(2)
            button.getLocationOnScreen(xy); root.getLocationOnScreen(origin)
            assertTrue("F$n must be fully visible vertically", xy[1] >= origin[1] && xy[1] + button.height <= origin[1] + root.height)
            assertTrue("F$n must be fully visible horizontally", xy[0] >= origin[0] && xy[0] + button.width <= origin[0] + root.width)
            assertEquals("F$n must fit on one line", 1, button.lineCount)
            key(root, "Ctrl").performClick()
            key(root, "Alt").performClick()
            key(root, "F$n").performClick()
            val pair = connection.events.takeLast(2)
            assertEquals(listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP), pair.map { it.action })
            assertTrue(pair.all { it.keyCode == KeyEvent.KEYCODE_F1 + n - 1 && it.isCtrlPressed && it.isAltPressed })
        }
        key(root, "F1").performClick()
        assertEquals(0, connection.events.last().metaState)
        service.onStartInputView(EditorInfo().apply { packageName = "com.termux" }, false)
        layout(root, 720, 260)
        for (n in 1..8) assertEquals(1, key(root, "F$n").lineCount)
        screenshot(root, "fn-wide-720")
        key(root, "Fn").performClick()
        assertFalse(hasKey(root, "F1"))
        assertFalse(SettingsStore.prefs(service).getBoolean("function_keys", true))
    }

    @Test @Config(qualifiers = "w320dp-h640dp-mdpi")
    fun labelsFitWithLargeSystemFont() = withKeyboard({
        org.robolectric.RuntimeEnvironment.setFontScale(1.5f)
        SettingsStore.prefs(it).edit().putInt("font", 22).putInt("height", 30).putInt("gap", 8).putBoolean("function_keys", true).apply()
    }) { _, root, _ ->
        layout(root, 320, 360)
        assertSpacePixels(key(root, "Space"), "space-font150")
        for (n in 1..8) {
            val button = key(root, "F$n")
            assertEquals(1, button.lineCount)
            assertTrue(button.paint.measureText(button.text.toString()) <= button.width)
            val bounds = android.graphics.Rect()
            button.paint.getTextBounds(button.text.toString(), 0, button.text.length, bounds)
            assertTrue(button.baseline + bounds.top >= 0 && button.baseline + bounds.bottom <= button.height)
        }
        screenshot(root, "fn-font150")
    }

    @Test fun enabledCombosAndFnAppearInImeAndSettingsPreviewIncludingEmojiToggle() = withKeyboard({
        SettingsStore.prefs(it).edit().putBoolean("function_keys", true).apply()
        SettingsStore.saveCombos(it, listOf(Combo("Build", "Ctrl+B"), Combo("Hidden", "Ctrl+H", false)))
    }) { service, root, connection ->
        assertTrue(hasKey(root, "Build")); assertFalse(hasKey(root, "Hidden"))
        key(root, "Build").performClick()
        assertEquals(KeyEvent.KEYCODE_B, connection.events.last().keyCode)
        assertTrue(connection.events.last().isCtrlPressed)
        val settings = Robolectric.buildActivity(MainActivity::class.java).setup()
        val preview = descendants(settings.get().window.decorView).filterIsInstance<TextView>()
        for (n in 1..8) assertTrue(preview.any { it.text.toString() == "F$n" })
        assertTrue(preview.any { it.text.toString() == "Build" })
        assertFalse(preview.any { it.text.toString() == "Hidden" })
        val previewSpace = preview.filterIsInstance<SpaceKeyButton>().single()
        layout(previewSpace, 128, 30)
        assertSpacePixels(previewSpace, "preview-space")
        settings.pause().stop().destroy()
        key(root, "😀").performClick()
        layout(root)
        for (n in 1..8) assertTrue(hasKey(root, "F$n"))
        val space = key(root, "Space")
        assertSpacePixels(space, "space-emoji")
        space.performClick()
        assertEquals(" ", connection.committed.toString())
        assertTrue(space.performLongClick())
        ShadowPopupMenu.getLatestPopupMenu().dismiss()
        assertEquals(" ", connection.committed.toString())
        key(root, "Fn").performClick()
        assertFalse(hasKey(root, "F1"))
        key(root, "ABC").performClick()
        assertTrue(hasKey(root, "Space"))
        assertFalse(SettingsStore.prefs(service).getBoolean("function_keys", true))
    }

    @Test fun shiftCapsAndLiteralSymbolPagesStillWork() = withKeyboard { _, root, connection ->
        // Avoid the double-tap window when explicitly testing one-shot Shift.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        key(root, "⇧").performClick(); key(root, "ą\n!").performClick()
        assertEquals("!", connection.committed.toString())
        assertTrue(hasKey(root, "ą\n1"))
        key(root, "⇧").performLongClick()
        key(root, "A").performClick(); key(root, "B").performClick(); key(root, "ą\n1").performClick()
        assertEquals("!AB1", connection.committed.toString())
        key(root, "⇪").performClick()
        key(root, "123").performClick()
        key(root, "!").performClick()
        assertEquals("!AB1!", connection.committed.toString())
    }

    @Test fun heldArrowAndBackspaceStopOnReleaseCancelSlideRebuildAndFinish() = withKeyboard { service, root, connection ->
        for (label in listOf("←", "⌫")) for (stop in listOf("release", "cancel", "slide", "rebuild", "finish")) {
            layout(root)
            // Backspace is present in two rows; either must obey the same cleanup.
            val button = descendants(root).filterIsInstance<Button>().first { it.text.toString() == label }
            val before = connection.events.size + connection.deletions
            touch(button, MotionEvent.ACTION_DOWN)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            assertTrue("$label failed to repeat", connection.events.size + connection.deletions > before + 1)
            when (stop) {
                "release" -> touch(button, MotionEvent.ACTION_UP)
                "cancel" -> touch(button, MotionEvent.ACTION_CANCEL)
                "slide" -> touch(button, MotionEvent.ACTION_MOVE, -20f)
                "rebuild" -> key(root, "Fn").performClick()
                "finish" -> service.onFinishInputView(false)
            }
            val count = connection.events.size + connection.deletions
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            assertEquals("$label kept repeating after $stop", count, connection.events.size + connection.deletions)
        }
    }

    @Test fun snippetsRemainExactAndPasswordClipboardIsRejected() = withKeyboard({
        SettingsStore.prefs(it).edit().putBoolean("clipboard_opt_in", true).apply()
        SettingsStore.saveSnippetEntries(it, listOf(SavedSnippet("Code", "  one\n\n two\n")))
    }) { service, root, connection ->
        key(root, "⭐").performClick()
        val popup = ShadowPopupMenu.getLatestPopupMenu()
        assertTrue(popup.menu.performIdentifierAction(popup.menu.getItem(0).itemId, 0))
        assertEquals("  one\n\n two\n", connection.committed.toString())
        assertTrue(SettingsStore.clipboardHistory(service).isEmpty())
        service.onStartInputView(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }, false)
        val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("secret", "password must not be saved"))
        key(root, "📋").performClick()
        assertEquals("Clipboard unavailable in password fields", org.robolectric.shadows.ShadowToast.getTextOfLatestToast())
        assertTrue(SettingsStore.clipboardHistory(service).isEmpty())
    }

    @Test fun animationSettingPersistsWithoutBreakingSpaceAndBackspaceHold() = withKeyboard { service, root, connection ->
        for (enabled in listOf(true, false)) {
            SettingsStore.prefs(service).edit().putBoolean("press_animation", enabled).commit()
            service.onStartInputView(EditorInfo().apply { packageName = "com.termux" }, false)
            layout(root)
            val space = key(root, "Space")
            touch(space, MotionEvent.ACTION_DOWN)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            assertEquals(if (enabled) .95f else 1f, space.scaleX, .01f)
            touch(space, MotionEvent.ACTION_UP)
            val deletionCount = connection.deletions
            layout(root)
            val backspace = descendants(root).filterIsInstance<Button>().first { it.text.toString() == "⌫" }
            touch(backspace, MotionEvent.ACTION_DOWN)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            touch(backspace, MotionEvent.ACTION_UP)
            assertTrue(connection.deletions > deletionCount + 1)
            assertEquals(enabled, SettingsStore.prefs(service).getBoolean("press_animation", !enabled))
        }
        assertEquals("  ", connection.committed.toString())
    }

    @Test fun micPermissionStatusTracksGrantAndRevokeAndCancelledResultsInsertNothing() = withKeyboard { service, root, connection ->
        val settings = Robolectric.buildActivity(MainActivity::class.java).setup()
        val voice = descendants(root).first { it.contentDescription?.startsWith("Voice input") == true }
        fun status() = descendants(settings.get().window.decorView).filterIsInstance<TextView>().map { it.text.toString() }
        assertTrue(status().contains("Permission required"))
        shadowOf(service.application).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        settings.pause().resume()
        assertTrue(status().contains("Permissions Granted"))
        assertFalse(android.speech.SpeechRecognizer.isRecognitionAvailable(service))
        voice.performClick()
        assertEquals("Telefone nėra kalbos atpažinimo paslaugos", org.robolectric.shadows.ShadowToast.getTextOfLatestToast())
        shadowOf(service.application).denyPermissions(android.Manifest.permission.RECORD_AUDIO)
        settings.pause().resume()
        assertTrue(status().contains("Permission required"))
        settings.pause().stop().destroy()
        voice.performClick()
        assertEquals(MicPermissionActivity::class.java.name, shadowOf(service).nextStartedActivity.component?.className)
        // Model an active recognizer session without accessing a real microphone.
        ReflectionHelpers.setField(service, "listening", true)
        service.onFinishInputView(false)
        service.onResults(android.os.Bundle().apply {
            putStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("late result"))
        })
        assertTrue(connection.committed.isEmpty())
    }
}
