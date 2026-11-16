package com.dailyfact.lockscreen

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object DailyUpdate {
    private const val WORK_NAME = "daily_fact"
    private const val TAG = "DailyFact"

    /** מציג את העובדה הנוכחית בכל מקום שהמשתמש הפעיל. */
    fun apply(context: Context, fact: String) {
        val store = FactStore(context)
        if (store.wallpaperEnabled) {
            try {
                WallpaperMaker.setLockScreen(context, fact)
            } catch (e: Exception) {
                Log.w(TAG, "wallpaper failed", e)
            }
        }
        if (store.notificationEnabled) Notifier.show(context, fact)
    }

    /** מתזמן עדכון פעם ביום, בפעם הראשונה ב־7 בבוקר. */
    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(7, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes()

        val request = PeriodicWorkRequestBuilder<DailyWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

class DailyWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = FactStore(applicationContext)
        if (!store.wallpaperEnabled && !store.notificationEnabled) return Result.success()
        DailyUpdate.apply(applicationContext, store.advance())
        return Result.success()
    }
}
