package com.dailyfact.lockscreen

import android.content.Context
import kotlin.random.Random

/** רשימת העובדות והעובדה הנוכחית. הסדר מעורבב פעם אחת לכל מכשיר, כדי שלא יחזרו עובדות. */
class FactStore(context: Context) {
    private val prefs = context.getSharedPreferences("facts", Context.MODE_PRIVATE)

    private val facts: List<String> =
        context.resources.openRawResource(R.raw.facts).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
        }

    private val order: List<Int> by lazy {
        var seed = prefs.getLong(KEY_SEED, 0L)
        if (seed == 0L) {
            seed = Random.nextLong().takeIf { it != 0L } ?: 1L
            prefs.edit().putLong(KEY_SEED, seed).apply()
        }
        facts.indices.shuffled(Random(seed))
    }

    val count: Int get() = facts.size

    fun current(): String = facts[order[prefs.getInt(KEY_POS, 0) % facts.size]]

    fun advance(): String {
        prefs.edit().putInt(KEY_POS, prefs.getInt(KEY_POS, 0) + 1).commit()
        return current()
    }

    var wallpaperEnabled: Boolean
        get() = prefs.getBoolean(KEY_WALLPAPER, false)
        set(v) { prefs.edit().putBoolean(KEY_WALLPAPER, v).apply() }

    var notificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY, false)
        set(v) { prefs.edit().putBoolean(KEY_NOTIFY, v).apply() }

    private companion object {
        const val KEY_SEED = "seed"
        const val KEY_POS = "pos"
        const val KEY_WALLPAPER = "wallpaper"
        const val KEY_NOTIFY = "notify"
    }
}
