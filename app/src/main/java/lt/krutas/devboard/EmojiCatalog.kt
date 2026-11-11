package lt.krutas.devboard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Bundled Unicode RGI emoji, usable offline without uploading typed text. */
internal object EmojiCatalog {
    data class Emoji(val value: String, val name: String)
    data class Group(val name: String, val icon: String, val items: List<Emoji>)

    private val icons = listOf("😀", "🧑", "🐻", "🍔", "🚗", "⚽", "💡", "❤️", "🏳️")
    private var cached: List<Group>? = null

    fun groups(context: Context): List<Group> {
        cached?.let { return it }
        val json = JSONObject(context.assets.open("emoji.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
        val values = json.getJSONArray("groups")
        return (0 until values.length()).map { groupIndex ->
            val record = values.getJSONObject(groupIndex)
            val items = record.getJSONArray("items")
            Group(record.getString("name"), icons.getOrElse(groupIndex) { "😀" },
                (0 until items.length()).map { i ->
                    val entry = items.getJSONArray(i)
                    Emoji(entry.getString(0), entry.getString(1))
                })
        }.also { cached = it }
    }

    fun recent(context: Context): List<Emoji> {
        val stored = context.getSharedPreferences("emoji_picker", Context.MODE_PRIVATE)
            .getString("recent", "[]") ?: "[]"
        val lookup = groups(context).flatMap { it.items }.associateBy { it.value }
        return try {
            val values = JSONArray(stored)
            (0 until values.length()).mapNotNull { lookup[values.getString(it)] }
        } catch (_: Exception) { emptyList() }
    }

    fun remember(context: Context, emoji: String) {
        val latest = (listOf(emoji) + recent(context).map { it.value })
            .distinct().take(48)
        val updated = JSONArray()
        latest.forEach { updated.put(it) }
        context.getSharedPreferences("emoji_picker", Context.MODE_PRIVATE)
            .edit().putString("recent", updated.toString()).apply()
    }
}
