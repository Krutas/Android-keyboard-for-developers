package lt.krutas.devboard

/** Pure mapping shared by the IME and its preview. Caps Lock affects letters only. */
internal object KeyboardMapping {
    val ltRows = listOf(
        listOf("ą", "ž", "e", "r", "t", "y", "u", "i", "o", "p", "į", "w"),
        listOf("a", "s", "d", "š", "g", "h", "j", "k", "l", "ų", "ė", "q"),
        listOf("⇧", "z", "ū", "c", "v", "b", "n", "m", "č", "f", "x", "ę", "⌫")
    )
    private val shifted = "1234567890-=[]\\;',./`".zip("!@#\$%^&*()_+{}|:\"<>?~").associate { it.first.toString() to it.second.toString() }
    fun shiftAfterCommit(manualShift: Boolean, autoCapitalize: Boolean): Boolean =
        !manualShift && autoCapitalize

    fun output(key: String, variant: String, shift: Boolean, caps: Boolean): String = when {
        key.length == 1 && key[0].isLetter() -> if (shift || caps) key.uppercase() else key.lowercase()
        variant == "pc" && shift -> shifted[key] ?: key
        else -> key
    }
    fun initialRows(base: List<List<String>>, language: String, variant: String): MutableList<MutableList<String>> {
        val rows = base.map { it.toMutableList() }.toMutableList()
        if (language == "lt-LT" && variant == "qwerty") ltRows.forEachIndexed { i, row -> rows[i + 3] = row.toMutableList() }
        return rows
    }
}
