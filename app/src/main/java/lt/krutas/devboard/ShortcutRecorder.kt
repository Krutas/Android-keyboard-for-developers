package lt.krutas.devboard

/** Records on-screen key taps without executing the shortcut. */
internal class ShortcutRecorder {
    private val selected = linkedSetOf<String>()
    fun reset() = selected.clear()
    fun press(key: String): String {
        if (key in listOf("Ctrl", "Alt", "Shift")) {
            if (!selected.add(key)) selected.remove(key)
            return selected.joinToString("+")
        }
        val result = (selected + key).joinToString("+")
        selected.clear()
        return result
    }
}
