package lt.krutas.devboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutRecorderTest {
    @Test fun modifiersAppearImmediatelyAndAreCapturedWithKey() {
        val recorder = ShortcutRecorder()
        assertEquals("Ctrl", recorder.press("Ctrl"))
        assertEquals("Ctrl+Shift", recorder.press("Shift"))
        assertEquals("Ctrl+Shift+C", recorder.press("C"))
        assertEquals("A", recorder.press("A"))
    }
    @Test fun tappingModifierAgainClearsIt() {
        val recorder = ShortcutRecorder()
        assertEquals("Alt", recorder.press("Alt"))
        assertEquals("", recorder.press("Alt"))
        assertEquals("F8", recorder.press("F8"))
    }
}
