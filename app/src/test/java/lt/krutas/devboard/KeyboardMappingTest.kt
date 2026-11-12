package lt.krutas.devboard

import org.junit.Assert.*
import org.junit.Test

class KeyboardMappingTest {
    @Test fun shiftAndCapsAreIndependent() {
        val pairs = "1234567890-=[]\\;',./`".zip("!@#\$%^&*()_+{}|:\"<>?~")
        for ((key, symbol) in pairs) {
            assertEquals(symbol.toString(), KeyboardMapping.output(key.toString(), "pc", true, false))
            assertEquals(key.toString(), KeyboardMapping.output(key.toString(), "pc", false, true))
            assertEquals(key.toString(), KeyboardMapping.output(key.toString(), "qwerty", true, false))
        }
        for (key in "ąčęėįšųūžqwerty") {
            assertEquals(key.uppercase(), KeyboardMapping.output(key.toString(), "pc", false, true))
            assertEquals(key.uppercase(), KeyboardMapping.output(key.toString(), "qwerty", true, false))
        }
    }
    @Test fun variantsPreserveBaseAndContainAllLetters() {
        val base = List(7) { listOf("one", "two", "three") }
        for (language in listOf("lt-LT", "en-US")) for (variant in listOf("pc", "qwerty")) {
            val rows = KeyboardMapping.initialRows(base, language, variant)
            assertEquals(7, rows.size)
            if (language == "lt-LT" && variant == "qwerty") {
                val letters = rows.subList(3, 6).flatten().joinToString("")
                for (letter in "abcdefghijklmnopqrstuvwxyząčęėįšųūž") assertTrue("Missing $letter", letters.contains(letter))
            } else assertEquals(base, rows)
            rows[0][0] = "changed"
            assertEquals("one", base[0][0])
        }
    }
    @Test fun comboParserValidatesRecordedAndLegacyActions() {
        assertEquals(Shortcut("C", true, false, true), Shortcut.parse(" Ctrl + Shift + c "))
        assertEquals(Shortcut("F8", false, true, false), Shortcut.parse("Alt+F8"))
        assertEquals("END", Shortcut.parse("End")?.key)
        for (invalid in listOf("", "Ctrl+", "Ctrl+Ctrl+A", "Super+A", "F13", "nonsense")) assertNull(Shortcut.parse(invalid))
    }
}
