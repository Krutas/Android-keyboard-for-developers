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


    private fun legacyRows(c: Context): MutableList<MutableList<String>> {
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

    private fun parseRows(array: JSONArray): MutableList<MutableList<String>>? {
        if (array.length() != defaultRows.size) return null
        return try {
            MutableList(array.length()) { i ->
                val row = array.getJSONArray(i)
                require(row.length() in 3..16)
                MutableList(row.length()) { j ->
                    val key = row.getString(j)
                    require(key.isNotBlank() && key.length <= 24)
                    key
                }
            }
        } catch (_: Exception) { null }
    }

    fun profiles(c: Context): LinkedHashMap<String, MutableList<MutableList<String>>> {
        val result = linkedMapOf<String, MutableList<MutableList<String>>>()
        try {
            val obj = JSONObject(prefs(c).getString("layout_profiles_v2", "{}") ?: "{}")
            val keys = obj.keys()
            while (keys.hasNext() && result.size < 30) {
                val name = keys.next()
                if (name.isBlank() || name.length > 40) continue
                val item = obj.opt(name)
                val array = when (item) {
                    is JSONArray -> item
                    is JSONObject -> item.optJSONArray("Terminal")
                    else -> null
                } ?: continue
                val rows = parseRows(array) ?: continue
                result[name] = rows
            }
        } catch (_: Exception) {}
        if (result.isEmpty()) result["Default"] = legacyRows(c)
        return LinkedHashMap(result)
    }

    fun activeProfile(c: Context): String {
        val current = prefs(c).getString("active_profile", "Default") ?: "Default"
        val all = profiles(c)
        return if (all.containsKey(current)) current else all.keys.first()
    }

    fun autoProfile(c: Context) = prefs(c).getBoolean("auto_profile", false)

    fun appBindings(c: Context): LinkedHashMap<String, String> {
        val map = linkedMapOf<String, String>()
        try {
            val obj = JSONObject(prefs(c).getString("app_bindings", "{}") ?: "{}")
            val keys = obj.keys()
            while (keys.hasNext() && map.size < 80) {
                val pkg = keys.next()
                val profile = obj.optString(pkg)
                if (pkg.matches(Regex("[A-Za-z0-9_.]{3,160}")) && profile.isNotBlank()) map[pkg] = profile
            }
        } catch (_: Exception) {}
        return LinkedHashMap(map)
    }

    fun effectiveProfile(c: Context, packageName: String? = null): String {
        if (autoProfile(c) && packageName != null) {
            val selected = appBindings(c)[packageName]
            if (selected != null && profiles(c).containsKey(selected)) return selected
        }
        return activeProfile(c)
    }

    fun rows(c: Context, packageName: String? = null): MutableList<MutableList<String>> {
        val all = profiles(c)
        val name = effectiveProfile(c, packageName)
        return all[name]?.map { it.toMutableList() }?.toMutableList() ?: copyDefault()
    }

    private fun rowsArray(rows: List<List<String>>): JSONArray {
        val arr = JSONArray()
        rows.forEach { arr.put(JSONArray(it)) }
        return arr
    }

    fun saveProfiles(c: Context, all: Map<String, List<List<String>>>) {
        val obj = JSONObject()
        all.forEach { (name, rows) ->
            if (name.isNotBlank() && name.length <= 40 && parseRows(rowsArray(rows)) != null) {
                obj.put(name, JSONObject().put("Terminal", rowsArray(rows)))
            }
        }
        require(obj.length() > 0)
        prefs(c).edit().putString("layout_profiles_v2", obj.toString()).apply()
    }

    fun saveRows(c: Context, rows: List<List<String>>) {
        require(parseRows(rowsArray(rows)) != null)
        val all = profiles(c)
        all[activeProfile(c)] = rows.map { it.toMutableList() }.toMutableList()
        saveProfiles(c, all)
    }

    fun resetSelectedRows(c: Context) {
        saveRows(c, copyDefault())
    }

    fun setActiveProfile(c: Context, name: String) {
        require(profiles(c).containsKey(name))
        prefs(c).edit().putString("active_profile", name).apply()
    }

    fun addProfile(c: Context, name: String) {
        require(name.isNotBlank() && name.length <= 40)
        val all = profiles(c)
        require(!all.containsKey(name) && all.size < 30)
        all[name] = rows(c)
        saveProfiles(c, all)
        setActiveProfile(c, name)
    }

    fun deleteProfile(c: Context, name: String) {
        val all = profiles(c)
        require(all.size > 1)
        all.remove(name)
        saveProfiles(c, all)
        if (activeProfile(c) == name || !all.containsKey(prefs(c).getString("active_profile", ""))) {
            prefs(c).edit().putString("active_profile", all.keys.first()).apply()
        }
        val map = appBindings(c).filterValues { it != name }
        setAppBindings(c, map)
    }

    fun setAppBindings(c: Context, bindings: Map<String, String>) {
        val obj = JSONObject()
        bindings.forEach { (pkg, name) ->
            if (pkg.matches(Regex("[A-Za-z0-9_.]{3,160}")) && profiles(c).containsKey(name)) {
                obj.put(pkg, name)
            }
        }
        prefs(c).edit().putString("app_bindings", obj.toString()).apply()
    }

    /** Import the simulator's {version, profiles: {name: {Terminal: [...]}}} format. */
    fun importProfiles(c: Context, json: String): Int {
        require(json.length <= 256_000) { "JSON file exceeds 256 KB" }
        val root = JSONObject(json)
        val source = root.getJSONObject("profiles")
        val imported = linkedMapOf<String, MutableList<MutableList<String>>>()
        val names = source.keys()
        while (names.hasNext() && imported.size < 30) {
            val name = names.next()
            if (name.isBlank() || name.length > 40) continue
            val item = source.opt(name)
            val arr = if (item is JSONObject) item.optJSONArray("Terminal") else item as? JSONArray
            val parsed = arr?.let { parseRows(it) } ?: continue
            imported[name] = parsed
        }
        require(imported.isNotEmpty()) { "No valid Terminal layout in JSON" }
        // Preserve existing profiles, but replace matching names with imported layouts.
        val current = profiles(c)
        current.putAll(imported)
        saveProfiles(c, current)
        return imported.size
    }

    fun exportProfiles(c: Context): String {
        val json = JSONObject()
        val savedProfiles = JSONObject()
        profiles(c).forEach { (name, rows) ->
            savedProfiles.put(name, JSONObject().put("Terminal", rowsArray(rows)))
        }
        json.put("version", 2)
        json.put("profiles", savedProfiles)
        json.put("activeProfile", activeProfile(c))
        json.put("appBindings", JSONObject(appBindings(c) as Map<*, *>))
        return json.toString(2)
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
