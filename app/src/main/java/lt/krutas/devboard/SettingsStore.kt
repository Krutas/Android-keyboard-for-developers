package lt.krutas.devboard

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

internal data class Palette(
    val name: String, val background: String, val key: String,
    val special: String, val text: String, val border: String,
    val colorful: Boolean = false
) {
    fun bg() = Color.parseColor(background)
    fun keyColor() = Color.parseColor(key)
    fun specialColor() = Color.parseColor(special)
    fun textColor() = Color.parseColor(text)
    fun borderColor() = Color.parseColor(border)
}

internal data class Combo(val label: String, val action: String)

internal object SettingsStore {
    // Includes the original nine terminal themes plus the original rounded-key styles.
    val themes = listOf(
        Palette("Midnight", "#151D2B", "#303E54", "#425474", "#F5F7FC", "#40516A"),
        Palette("Graphite", "#18191E", "#383A42", "#53565F", "#F8F9FC", "#757784"),
        Palette("Terminal Green", "#020703", "#091509", "#0B2714", "#35FA43", "#27CC35"),
        Palette("Amber Terminal", "#100C05", "#22170B", "#4C300F", "#FFCA3A", "#DC9600"),
        Palette("Cyber Violet", "#21172E", "#48345C", "#655080", "#FBEFFF", "#AC7ADC"),
        Palette("Arctic Blue", "#192333", "#26384C", "#405D79", "#ECF7FF", "#8AA9D0"),
        Palette("Ivory", "#F7F0E4", "#FFF9EE", "#EDE0C8", "#16475A", "#427C90"),
        Palette("Cobalt", "#09144D", "#103080", "#1249AA", "#FFFFFF", "#3D86FF"),
        Palette("Cyber Cyan", "#07191C", "#07272F", "#134453", "#32F0F2", "#F09B37"),
        Palette("Monochrome", "#E6E6E6", "#FAFAFA", "#D8D8D8", "#151515", "#111111"),
        Palette("Neon Blue", "#070C19", "#0C1738", "#113B91", "#D6E7FF", "#315AFB"),
        Palette("Mint", "#0F2A23", "#1B3930", "#25594A", "#E0FFE8", "#8EE0AF"),
        Palette("Sakura", "#2C1C2A", "#4C3044", "#754665", "#FFE5F0", "#E98BB7"),
        Palette("Solarized", "#002B36", "#073642", "#124D5D", "#93A1A1", "#268BD2"),
        Palette("Paper", "#E6E7EF", "#FFFFFF", "#D4DDEE", "#1E2C44", "#BBC6DC"),
        Palette("Ocean", "#071827", "#15304A", "#25577A", "#E2F5FF", "#3E90C2"),
        Palette("Wood", "#271F18", "#493A2E", "#775A39", "#FAEBD1", "#937553"),
        Palette("Neon Circuit", "#080F1B", "#123042", "#064959", "#7FFFE7", "#1A8991"),
        Palette("Colorful ASCII", "#080D1C", "#152343", "#1F3561", "#FFFFFF", "#4F75D2", true)
    )

    val defaultRows = listOf(
        listOf("Esc", "Ctrl", "Alt", "Tab", "/", "Home", "PgUp"),
        listOf("←", "↑", "↓", "→", "Enter", "End", "PgDn"),
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("⇧", "Z", "X", "C", "V", "B", "N", "M", "⌫"),
        listOf("123", "🎙", ",", "Space", ".", "↵")
    )
    val symbols1 = listOf(
        listOf("Esc", "Ctrl", "Alt", "Tab", "/", "Home", "PgUp"),
        listOf("←", "↑", "↓", "→", "Enter", "End", "PgDn"),
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")"),
        listOf("-", "_", "=", "+", "[", "]", "{", "}", "\\"),
        listOf("1/2", ";", ":", "'", "\"", "<", ">", "?", "⌫"),
        listOf("ABC", "🎙", ",", "Space", ".", "↵")
    )
    val symbols2 = listOf(
        symbols1[0], symbols1[1], symbols1[2],
        listOf("~", "`", "|", "\\", "€", "£", "¥", "§", "±", "°"),
        listOf("✓", "•", "→", "←", "↑", "↓", "≤", "≥", "≠"),
        listOf("2/2", "\"", "'", "«", "»", "×", "÷", "?", "⌫"),
        symbols1[6]
    )
    val ltNumbers = mapOf("1" to "ą", "2" to "č", "3" to "ę", "4" to "ė", "5" to "į", "6" to "š", "7" to "ų", "8" to "ū", "9" to "ž")

    fun prefs(c: Context) = c.getSharedPreferences("devboard", Context.MODE_PRIVATE)
    fun theme(c: Context): Palette = themes.firstOrNull { it.name == prefs(c).getString("theme", "Midnight") } ?: themes.first()
    fun language(c: Context) = prefs(c).getString("language", "lt-LT") ?: "lt-LT"
    fun font(c: Context) = prefs(c).getInt("font", 14)
    fun keyHeight(c: Context) = prefs(c).getInt("height", 39)
    fun gap(c: Context) = prefs(c).getInt("gap", 2)
    fun radius(c: Context) = prefs(c).getInt("radius", 6)
    fun showExtras(c: Context) = prefs(c).getBoolean("extras", true)
    fun captureClipboard(c: Context) = prefs(c).getBoolean("clipboard_opt_in", false)

    fun rows(c: Context): MutableList<MutableList<String>> {
        val stored = prefs(c).getString("keyboard_rows", null) ?: return copyDefault()
        return try {
            val outer = JSONArray(stored)
            if (outer.length() != defaultRows.size) return copyDefault()
            MutableList(outer.length()) { row ->
                val inner = outer.getJSONArray(row)
                MutableList(inner.length()) { col -> inner.getString(col) }
            }
        } catch (_: Exception) { copyDefault() }
    }
    fun copyDefault() = defaultRows.map { it.toMutableList() }.toMutableList()
    fun saveRows(c: Context, rows: List<List<String>>) {
        val arr = JSONArray()
        rows.forEach { row -> arr.put(JSONArray(row)) }
        prefs(c).edit().putString("keyboard_rows", arr.toString()).apply()
    }
    fun combos(c: Context): MutableList<Combo> {
        return try {
            val a = JSONArray(prefs(c).getString("combos", "[]"))
            MutableList(a.length()) { i ->
                val o = a.getJSONObject(i)
                Combo(o.getString("label"), o.getString("action"))
            }
        } catch (_: Exception) { mutableListOf() }
    }
    fun saveCombos(c: Context, combos: List<Combo>) {
        val a = JSONArray()
        combos.forEach { a.put(JSONObject().put("label", it.label).put("action", it.action)) }
        prefs(c).edit().putString("combos", a.toString()).apply()
    }
    fun clipboardHistory(c: Context): MutableList<String> = try {
        val a = JSONArray(prefs(c).getString("clipboard_history", "[]"))
        MutableList(a.length()) { i -> a.getString(i) }
    } catch (_: Exception) { mutableListOf() }
    fun saveClipboardHistory(c: Context, items: List<String>) {
        val a = JSONArray(); items.forEach(a::put)
        prefs(c).edit().putString("clipboard_history", a.toString()).apply()
    }
    fun snippets(c: Context): MutableList<String> = try {
        val a = JSONArray(prefs(c).getString("snippets", "[]"))
        MutableList(a.length()) { i -> a.getString(i) }
    } catch (_: Exception) { mutableListOf() }
    fun saveSnippets(c: Context, items: List<String>) {
        val a = JSONArray(); items.forEach(a::put)
        prefs(c).edit().putString("snippets", a.toString()).apply()
    }
}
