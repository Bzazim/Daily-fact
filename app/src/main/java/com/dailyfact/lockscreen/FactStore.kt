package com.dailyfact.lockscreen

import android.content.Context
import java.time.LocalDate
import kotlin.random.Random

/** כל המצב של האפליקציה: איזו עובדה מוצגת, מועדפים והגדרות. */
class FactStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("facts", Context.MODE_PRIVATE)

    val facts: List<Fact> = Facts.all(context)
    val categories: List<String> = Facts.categories(context)

    private val order: List<Int> by lazy {
        var seed = prefs.getLong(KEY_SEED, 0L)
        if (seed == 0L) {
            seed = Random.nextLong().takeIf { it != 0L } ?: 1L
            prefs.edit().putLong(KEY_SEED, seed).apply()
        }
        facts.indices.shuffled(Random(seed))
    }

    private fun pool(): List<Int> {
        val cats = enabledCategories
        return order.filter { facts[it].category in cats }
    }

    var pos: Int
        get() = prefs.getInt(KEY_POS, 0)
        set(v) { prefs.edit().putInt(KEY_POS, v).commit() }

    /** העובדה במרחק [offset] מהנוכחית (‎-1 = הקודמת). null כשלא נבחר אף נושא. */
    fun factAt(offset: Int = 0): Fact? {
        val p = pool()
        if (p.isEmpty()) return null
        return facts[p[Math.floorMod(pos + offset, p.size)]]
    }

    fun current(): Fact? = factAt(0)

    fun advance(): Fact? {
        pos += 1
        return current()
    }

    // ---- מועדפים (החדש ראשון) ----
    private var favoriteIds: List<String>
        get() = prefs.getString(KEY_FAVS, "")!!.split(',').filter { it.isNotEmpty() }
        set(v) { prefs.edit().putString(KEY_FAVS, v.joinToString(",")).commit() }

    fun isFavorite(fact: Fact): Boolean = fact.id in favoriteIds

    /** מחזיר true אם העובדה נשמרה, false אם הוסרה. */
    fun toggleFavorite(fact: Fact): Boolean {
        val ids = favoriteIds
        return if (fact.id in ids) {
            favoriteIds = ids - fact.id; false
        } else {
            favoriteIds = listOf(fact.id) + ids; true
        }
    }

    fun favorites(): List<Fact> {
        val byId = facts.associateBy { it.id }
        return favoriteIds.mapNotNull { byId[it] }
    }

    // ---- הגדרות ----
    var enabledCategories: Set<String>
        get() = prefs.getStringSet(KEY_CATS, null)?.toSet() ?: categories.toSet()
        set(v) { prefs.edit().putStringSet(KEY_CATS, v).commit() }

    var wallpaperEnabled: Boolean
        get() = prefs.getBoolean(KEY_WALLPAPER, false)
        set(v) { prefs.edit().putBoolean(KEY_WALLPAPER, v).commit() }

    var notificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY, false)
        set(v) { prefs.edit().putBoolean(KEY_NOTIFY, v).commit() }

    /** כל כמה שעות מתחלפת העובדה: 24, 12 או 3. */
    var frequencyHours: Int
        get() = prefs.getInt(KEY_FREQ, 24)
        set(v) { prefs.edit().putInt(KEY_FREQ, v).commit() }

    var hour: Int
        get() = prefs.getInt(KEY_HOUR, 7)
        set(v) { prefs.edit().putInt(KEY_HOUR, v).commit() }

    var widgetStyle: WidgetStyle
        get() = WidgetStyle.of(prefs.getString(KEY_STYLE, null))
        set(v) { prefs.edit().putString(KEY_STYLE, v.key).commit() }

    /** גודל טקסט בסיסי ב־sp: ‏13, 15 או 17. */
    var textSize: Int
        get() = prefs.getInt(KEY_SIZE, 15)
        set(v) { prefs.edit().putInt(KEY_SIZE, v).commit() }

    /** מעדכן ומחזיר כמה ימים ברצף נפתחה האפליקציה. */
    fun touchStreak(): Int {
        val today = LocalDate.now().toEpochDay()
        val last = prefs.getLong(KEY_LAST_DAY, Long.MIN_VALUE)
        var streak = prefs.getInt(KEY_STREAK, 0)
        if (last == today) return streak.coerceAtLeast(1)
        streak = if (last == today - 1) streak + 1 else 1
        prefs.edit().putLong(KEY_LAST_DAY, today).putInt(KEY_STREAK, streak).apply()
        return streak
    }

    private companion object {
        const val KEY_SEED = "seed"
        const val KEY_POS = "pos"
        const val KEY_FAVS = "favs"
        const val KEY_CATS = "cats"
        const val KEY_WALLPAPER = "wallpaper"
        const val KEY_NOTIFY = "notify"
        const val KEY_FREQ = "freq"
        const val KEY_HOUR = "hour"
        const val KEY_STYLE = "style"
        const val KEY_SIZE = "size"
        const val KEY_LAST_DAY = "lastDay"
        const val KEY_STREAK = "streak"
    }
}
