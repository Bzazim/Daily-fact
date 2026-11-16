package com.dailyfact.lockscreen

import android.content.Context

/** [id] יציב (מבוסס על הטקסט), כדי שמועדפים יישמרו גם כשמוסיפים עובדות לקובץ. */
data class Fact(val id: String, val number: Int, val category: String, val text: String)

object Facts {
    @Volatile private var cache: List<Fact>? = null

    fun all(context: Context): List<Fact> = cache ?: load(context).also { cache = it }

    fun categories(context: Context): List<String> = all(context).map { it.category }.distinct()

    private fun load(context: Context): List<Fact> {
        val out = ArrayList<Fact>()
        var category = ""
        context.resources.openRawResource(R.raw.facts).bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty()) continue
                if (line.startsWith("#")) {
                    val name = line.removePrefix("#").trim()
                    if (!name.contains("עובדה אחת")) category = name
                    continue
                }
                out += Fact(Integer.toHexString(line.hashCode()), out.size + 1, category, line)
            }
        }
        return out
    }
}
