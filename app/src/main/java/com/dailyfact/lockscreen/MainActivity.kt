package com.dailyfact.lockscreen

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var store: FactStore
    private lateinit var factText: TextView
    private lateinit var wallpaperSwitch: Switch
    private lateinit var notifySwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = FactStore(this)

        factText = findViewById(R.id.fact_text)
        wallpaperSwitch = findViewById(R.id.switch_wallpaper)
        notifySwitch = findViewById(R.id.switch_notify)

        factText.text = store.current()
        wallpaperSwitch.isChecked = store.wallpaperEnabled
        notifySwitch.isChecked = store.notificationEnabled && Notifier.canNotify(this)

        wallpaperSwitch.setOnCheckedChangeListener { _, on ->
            store.wallpaperEnabled = on
            if (on) {
                DailyUpdate.schedule(this)
                runInBackground(R.string.wallpaper_set) { WallpaperMaker.setLockScreen(this, store.current()) }
            } else {
                runInBackground(R.string.wallpaper_cleared) { WallpaperMaker.clearLockScreen(this) }
                cancelIfIdle()
            }
        }

        notifySwitch.setOnCheckedChangeListener { _, on ->
            store.notificationEnabled = on
            if (on) {
                DailyUpdate.schedule(this)
                if (Notifier.canNotify(this)) Notifier.show(this, store.current())
                else if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFY)
                }
            } else {
                Notifier.cancel(this)
                cancelIfIdle()
            }
        }

        findViewById<Button>(R.id.button_next).setOnClickListener {
            val fact = store.advance()
            factText.text = fact
            runInBackground(null) { DailyUpdate.apply(this, fact) }
        }

        findViewById<TextView>(R.id.fact_count).text = getString(R.string.fact_count, store.count)

        if (store.wallpaperEnabled || store.notificationEnabled) DailyUpdate.schedule(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_NOTIFY) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            Notifier.show(this, store.current())
        } else {
            notifySwitch.isChecked = false
            Toast.makeText(this, R.string.notify_denied, Toast.LENGTH_LONG).show()
        }
    }

    private fun cancelIfIdle() {
        if (!store.wallpaperEnabled && !store.notificationEnabled) DailyUpdate.cancel(this)
    }

    private fun runInBackground(doneMessage: Int?, work: () -> Unit) {
        Thread {
            val ok = try { work(); true } catch (e: Exception) { false }
            runOnUiThread {
                when {
                    !ok -> Toast.makeText(this, R.string.wallpaper_failed, Toast.LENGTH_LONG).show()
                    doneMessage != null -> Toast.makeText(this, doneMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private companion object {
        const val REQ_NOTIFY = 1
    }
}
