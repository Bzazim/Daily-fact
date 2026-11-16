package com.dailyfact.lockscreen

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object DailyUpdate {
    private const val PERIODIC = "next_fact"
    private const val REFRESH = "refresh"
    private const val TAG = "DailyFact"

    /** מציג את העובדה הנוכחית בכל מקום: ווידג׳ט, רקע מסך הנעילה והתראה (לפי ההגדרות). רץ ברקע. */
    fun refreshNow(context: Context) {
        FactWidget.updateAll(context)
        val store = FactStore(context)
        val fact = store.current() ?: return
        if (store.wallpaperEnabled) {
            try {
                WallpaperMaker.setLockScreen(context, fact, store.textSize)
            } catch (e: Exception) {
                Log.w(TAG, "wallpaper failed", e)
            }
        }
        if (store.notificationEnabled) Notifier.show(context, fact)
    }

    /** הווידג׳ט מתעדכן מיד; הרקע וההתראה מתעדכנים ברקע. */
    fun refresh(context: Context) {
        FactWidget.updateAll(context)
        WorkManager.getInstance(context).enqueueUniqueWork(
            REFRESH, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<RefreshWorker>().build()
        )
    }

    /** מתזמן החלפת עובדה. [reset] = אחרי שינוי שעה או תדירות. */
    fun schedule(context: Context, reset: Boolean = false) {
        val store = FactStore(context)
        val freq = store.frequencyHours
        val now = LocalDateTime.now()
        val delay = Duration.between(now, nextRun(now, store.hour, freq)).toMinutes()

        val request = PeriodicWorkRequestBuilder<NextFactWorker>(freq.toLong(), TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,
            if (reset) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** הפעם הבאה אחרי [now] שמתאימה לשעה ולתדירות (למשל 7:00 כל 12 שעות → 7:00 / 19:00). */
    fun nextRun(now: LocalDateTime, hour: Int, frequencyHours: Int): LocalDateTime {
        var t = now.toLocalDate().atTime(hour, 0).minusDays(1)
        while (!t.isAfter(now)) t = t.plusHours(frequencyHours.toLong())
        return t
    }
}

class NextFactWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        FactStore(applicationContext).advance()
        DailyUpdate.refreshNow(applicationContext)
        return Result.success()
    }
}

class RefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        DailyUpdate.refreshNow(applicationContext)
        return Result.success()
    }
}
