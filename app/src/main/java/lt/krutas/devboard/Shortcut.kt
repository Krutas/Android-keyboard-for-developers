package lt.krutas.devboard

internal data class Shortcut(val key: String, val ctrl: Boolean, val alt: Boolean, val shift: Boolean) {
    companion object {
        fun parse(raw: String): Shortcut? {
            val parts = raw.split('+').map { it.trim().uppercase() }
            if (parts.isEmpty() || parts.any { it.isEmpty() }) return null
            val modifiers = parts.dropLast(1)
            if (modifiers.distinct().size != modifiers.size || modifiers.any { it !in listOf("CTRL", "ALT", "SHIFT") }) return null
            val key = parts.last()
            if (!key.matches(Regex("[A-Z0-9]|F([1-9]|1[0-2])")) && key !in listOf("ESC", "ESCAPE", "TAB", "SPACE", "ENTER", "BACKSPACE", "DELETE", "DEL", "HOME", "END", "PAGEUP", "PAGEDOWN", "PGUP", "PGDN", "INSERT", "←", "→", "↑", "↓")) return null
            return Shortcut(key, "CTRL" in modifiers, "ALT" in modifiers, "SHIFT" in modifiers)
        }
    }
}
