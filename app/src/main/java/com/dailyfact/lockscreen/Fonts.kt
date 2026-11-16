package com.dailyfact.lockscreen

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/** הגופנים של האפליקציה: רוביק לממשק, פרנק רוהל לטקסט של העובדות. */
object Fonts {
    private val cache = HashMap<String, Typeface>()

    private fun load(context: Context, file: String, weight: Int): Typeface = synchronized(cache) {
        cache.getOrPut("$file:$weight") {
            Typeface.Builder(context.assets, "fonts/$file")
                .setFontVariationSettings("'wght' $weight")
                .build() ?: Typeface.DEFAULT
        }
    }

    fun ui(context: Context, weight: Int = 400) = load(context, "Rubik.ttf", weight)
    fun read(context: Context) = load(context, "FrankRuhlLibre.ttf", 500)

    /** עובר על כל הטקסטים במסך. tag="read" → גופן הקריאה, tag="medium" → משקל בינוני, bold → מודגש. */
    fun apply(view: View) {
        if (view is TextView) {
            val ctx = view.context
            view.typeface = when {
                view.tag == "read" -> read(ctx)
                view.tag == "medium" -> ui(ctx, 500)
                view.typeface?.isBold == true -> ui(ctx, 700)
                else -> ui(ctx, 400)
            }
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) apply(view.getChildAt(i))
    }
}
