package com.dailyfact.lockscreen

import android.app.WallpaperManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import kotlin.math.max
import kotlin.math.min

/** מצייר תמונת רקע עם העובדה ושם אותה רק במסך הנעילה (רקע מסך הבית לא משתנה). */
object WallpaperMaker {

    fun setLockScreen(context: Context, fact: Fact, sizeSp: Int = 15) {
        val bitmap = render(context, fact.text, sizeSp)
        WallpaperManager.getInstance(context)
            .setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
        bitmap.recycle()
    }

    /** מחזיר את מסך הנעילה לרקע של מסך הבית. */
    fun clearLockScreen(context: Context) {
        WallpaperManager.getInstance(context).clear(WallpaperManager.FLAG_LOCK)
    }

    fun render(context: Context, fact: String, sizeSp: Int = 15): Bitmap {
        val dm = Resources.getSystem().displayMetrics
        val w = min(dm.widthPixels, dm.heightPixels).coerceAtLeast(720)
        val h = max(dm.widthPixels, dm.heightPixels).coerceAtLeast(1280)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // רקע: מעבר צבעים כהה
        val bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w * 0.3f, h.toFloat(),
                intArrayOf(0xFF0B1A2B.toInt(), 0xFF12324A.toInt(), 0xFF1D4F5E.toInt()),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

        val margin = w * 0.08f
        val padding = w * 0.06f
        val textWidth = (w - 2 * margin - 2 * padding).toInt()

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8FD3E8.toInt()
            textSize = w * 0.042f
            typeface = Fonts.ui(context, 700)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Fonts.read(context)
        }

        // מקטינים את הגופן עד שהטקסט נכנס
        var size = w * 0.058f * sizeSp / 15f
        var body: StaticLayout
        do {
            bodyPaint.textSize = size
            body = layout(fact, bodyPaint, textWidth)
            size *= 0.92f
        } while (body.height > h * 0.30f && size > w * 0.03f)

        val title = layout(context.getString(R.string.did_you_know), titlePaint, textWidth)
        val iconSize = (titlePaint.textSize * 1.15f).toInt()
        val iconGap = titlePaint.textSize * 0.4f
        val gap = w * 0.03f
        val cardH = padding * 2 + title.height + gap + body.height

        // הכרטיס יושב קצת מתחת לאמצע, מתחת לשעון של מסך הנעילה
        val cardTop = h * 0.56f - cardH / 2
        val card = RectF(margin, cardTop, w - margin, cardTop + cardH)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66000000 }
        val radius = w * 0.05f
        canvas.drawRoundRect(card, radius, radius, cardPaint)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.003f
            color = 0x558FD3E8
        }
        canvas.drawRoundRect(card, radius, radius, border)

        canvas.save()
        canvas.translate(margin + padding, cardTop + padding)
        // נורה מימין לכותרת (בעברית הכותרת מתחילה מימין)
        context.getDrawable(R.drawable.ic_bulb)?.let { icon ->
            icon.setTint(titlePaint.color)
            val top = (title.getLineBottom(0) - iconSize) / 2
            icon.setBounds(textWidth - iconSize, top, textWidth, top + iconSize)
            icon.draw(canvas)
        }
        canvas.save()
        canvas.translate(-(iconSize + iconGap), 0f)
        title.draw(canvas)
        canvas.restore()
        canvas.translate(0f, title.height + gap)
        body.draw(canvas)
        canvas.restore()

        return bitmap
    }

    private fun layout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(0f, 1.25f)
            .build()
}
