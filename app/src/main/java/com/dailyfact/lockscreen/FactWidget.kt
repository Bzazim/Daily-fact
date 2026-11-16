package com.dailyfact.lockscreen

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews

/** הווידג׳ט של מסך הבית: עובדה, נושא, כפתור החלפה ולב. לחיצה על הווידג׳ט פותחת את האפליקציה. */
class FactWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = build(context)
        ids.forEach { manager.updateAppWidget(it, views) }
        DailyUpdate.schedule(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_NEXT -> {
                FactStore(context).advance()
                DailyUpdate.refresh(context)
            }
            ACTION_FAVORITE -> {
                val store = FactStore(context)
                store.current()?.let { store.toggleFavorite(it) }
                updateAll(context)
            }
        }
    }

    companion object {
        private const val ACTION_NEXT = "com.dailyfact.lockscreen.NEXT"
        private const val ACTION_FAVORITE = "com.dailyfact.lockscreen.FAVORITE"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FactWidget::class.java))
            if (ids.isEmpty()) return
            val views = build(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun build(context: Context): RemoteViews {
            val store = FactStore(context)
            val fact = store.current()
            val style = store.widgetStyle
            val favorite = fact != null && store.isFavorite(fact)

            return RemoteViews(context.packageName, R.layout.widget_fact).apply {
                setInt(R.id.w_root, "setBackgroundResource", style.background)
                setInt(R.id.w_icon, "setColorFilter", style.accent)
                setTextColor(R.id.w_title, style.accent)

                setTextViewText(R.id.w_category, fact?.category ?: "")
                setTextColor(R.id.w_category, style.chipText)
                setInt(R.id.w_category, "setBackgroundResource", style.chipBackground)

                setTextViewText(R.id.w_text, fact?.text ?: context.getString(R.string.no_categories))
                setTextColor(R.id.w_text, style.text)
                setTextViewTextSize(R.id.w_text, TypedValue.COMPLEX_UNIT_SP, store.textSize.toFloat())

                setInt(R.id.w_next, "setBackgroundResource", style.buttonBackground)
                setInt(R.id.w_next, "setColorFilter", style.text)
                setInt(R.id.w_favorite, "setBackgroundResource", style.buttonBackground)
                setImageViewResource(R.id.w_favorite, if (favorite) R.drawable.ic_heart_filled else R.drawable.ic_heart)
                setInt(R.id.w_favorite, "setColorFilter", if (favorite) 0xFFFF6B81.toInt() else style.text)

                val open = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                setOnClickPendingIntent(R.id.w_root, open)
                setOnClickPendingIntent(R.id.w_next, broadcast(context, ACTION_NEXT, 1))
                setOnClickPendingIntent(R.id.w_favorite, broadcast(context, ACTION_FAVORITE, 2))
            }
        }

        private fun broadcast(context: Context, action: String, code: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context, code,
                Intent(context, FactWidget::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
    }
}
