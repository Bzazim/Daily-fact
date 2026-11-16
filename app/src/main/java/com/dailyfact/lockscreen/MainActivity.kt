package com.dailyfact.lockscreen

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var store: FactStore
    private lateinit var wallpaperSwitch: Switch
    private lateinit var notifySwitch: Switch
    private var tab = Tab.TODAY

    private enum class Tab { TODAY, FAVORITES, SETTINGS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = FactStore(this)
        applyInsets()

        findViewById<TextView>(R.id.date).text =
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'ב'MMMM", Locale.forLanguageTag("he")))

        // היום
        findViewById<View>(R.id.button_next).setOnClickListener {
            store.advance()
            DailyUpdate.refresh(this)
            renderToday()
            findViewById<ScrollView>(R.id.pane_today).smoothScrollTo(0, 0)
        }
        findViewById<View>(R.id.button_favorite).setOnClickListener {
            val fact = store.current() ?: return@setOnClickListener
            val saved = store.toggleFavorite(fact)
            toast(if (saved) R.string.saved else R.string.removed)
            FactWidget.updateAll(this)
            renderToday()
        }
        findViewById<View>(R.id.button_share).setOnClickListener { share() }

        // לשוניות
        findViewById<View>(R.id.tab_today).setOnClickListener { showTab(Tab.TODAY) }
        findViewById<View>(R.id.tab_favorites).setOnClickListener { showTab(Tab.FAVORITES) }
        findViewById<View>(R.id.tab_settings).setOnClickListener { showTab(Tab.SETTINGS) }

        setupSettings()
        Fonts.apply(findViewById(R.id.root))

        DailyUpdate.schedule(this)
        showTab(Tab.TODAY)
    }

    override fun onResume() {
        super.onResume()
        // העובדה יכלה להתחלף ברקע בזמן שהאפליקציה הייתה סגורה
        renderToday()
        if (tab == Tab.FAVORITES) renderFavorites()
    }

    // ---------- מסגרת ----------

    private fun applyInsets() {
        val header = findViewById<View>(R.id.header)
        val tabbar = findViewById<View>(R.id.tabbar)
        val headerTop = header.paddingTop
        findViewById<View>(R.id.root).setOnApplyWindowInsetsListener { _, insets ->
            val top: Int
            val bottom: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                top = bars.top; bottom = bars.bottom
            } else {
                @Suppress("DEPRECATION")
                top = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottom = insets.systemWindowInsetBottom
            }
            header.setPadding(header.paddingLeft, headerTop + top, header.paddingRight, header.paddingBottom)
            tabbar.setPadding(0, 0, 0, bottom)
            insets
        }
    }

    private fun showTab(t: Tab) {
        tab = t
        findViewById<View>(R.id.pane_today).visibility = if (t == Tab.TODAY) View.VISIBLE else View.GONE
        findViewById<View>(R.id.pane_favorites).visibility = if (t == Tab.FAVORITES) View.VISIBLE else View.GONE
        findViewById<View>(R.id.pane_settings).visibility = if (t == Tab.SETTINGS) View.VISIBLE else View.GONE
        findViewById<View>(R.id.tab_today).isSelected = t == Tab.TODAY
        findViewById<View>(R.id.tab_favorites).isSelected = t == Tab.FAVORITES
        findViewById<View>(R.id.tab_settings).isSelected = t == Tab.SETTINGS
        when (t) {
            Tab.TODAY -> renderToday()
            Tab.FAVORITES -> renderFavorites()
            Tab.SETTINGS -> renderSettings()
        }
    }

    // ---------- היום ----------

    private fun renderToday() {
        val fact = store.current()
        val category = findViewById<TextView>(R.id.fact_category)
        val text = findViewById<TextView>(R.id.fact_text)
        val number = findViewById<TextView>(R.id.fact_number)
        val favorite = findViewById<ImageView>(R.id.button_favorite)

        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, store.textSize + 4f)
        if (fact == null) {
            category.visibility = View.GONE
            number.text = ""
            text.text = getString(R.string.no_categories)
            favorite.isEnabled = false
        } else {
            category.visibility = View.VISIBLE
            category.text = fact.category
            number.text = getString(R.string.fact_number, fact.number, store.facts.size)
            text.text = fact.text
            favorite.isEnabled = true
            val saved = store.isFavorite(fact)
            favorite.setImageResource(if (saved) R.drawable.ic_heart_filled else R.drawable.ic_heart)
            favorite.imageTintList = getColorStateList(if (saved) R.color.favorite else R.color.text)
        }

        findViewById<TextView>(R.id.streak_count).text = store.touchStreak().toString()
        val nextAt = DailyUpdate.nextRun(java.time.LocalDateTime.now(), store.hour, store.frequencyHours)
        findViewById<TextView>(R.id.streak_text).text =
            getString(R.string.streak, nextAt.format(DateTimeFormatter.ofPattern("HH:mm")))

        // ימים קודמים
        val list = findViewById<LinearLayout>(R.id.past_list)
        list.removeAllViews()
        val labels = listOf(R.string.yesterday, R.string.two_days_ago, R.string.three_days_ago)
        if (fact != null) {
            labels.forEachIndexed { i, label ->
                val past = store.factAt(-(i + 1)) ?: return@forEachIndexed
                if (past.id == fact.id) return@forEachIndexed
                val whenLabel = if (store.frequencyHours == 24) getString(label) else getString(R.string.earlier)
                list.addView(pastItem("$whenLabel · ${past.category}", past.text) {
                    store.pos = store.pos - (i + 1)
                    DailyUpdate.refresh(this)
                    renderToday()
                    findViewById<ScrollView>(R.id.pane_today).smoothScrollTo(0, 0)
                })
            }
        }
        Fonts.apply(list)
    }

    private fun pastItem(label: String, body: String, onClick: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card2)
            foreground = ripple()
            setPadding(dp(14), dp(11), dp(14), dp(11))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(8) }
            addView(textView(label, 12f, R.color.muted2))
            addView(textView(body, 14f, R.color.text_soft).apply { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(0, dp(3), 0, 0) })
        }

    private fun share() {
        val fact = store.current() ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.did_you_know) + " " + fact.text + getString(R.string.share_footer))
        }
        startActivity(Intent.createChooser(send, getString(R.string.share)))
    }

    // ---------- מועדפים ----------

    private fun renderFavorites() {
        val list = findViewById<LinearLayout>(R.id.favorites_list)
        list.removeAllViews()
        val favorites = store.favorites()
        if (favorites.isEmpty()) {
            list.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(16), dp(56), dp(16), dp(16))
                addView(ImageView(context).apply {
                    setImageResource(R.drawable.ic_heart)
                    imageTintList = getColorStateList(R.color.option_border)
                    layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { bottomMargin = dp(12) }
                })
                addView(textView(getString(R.string.favorites_empty), 14f, R.color.muted).apply {
                    gravity = Gravity.CENTER
                    setLineSpacing(0f, 1.3f)
                })
            })
        }
        favorites.forEach { fact ->
            list.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.card)
                setPadding(dp(16), dp(14), dp(16), dp(12))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(10) }
                addView(textView(fact.text, store.textSize + 1f, R.color.text).apply {
                    tag = "read"
                    setLineSpacing(0f, 1.3f)
                    setTextIsSelectable(true)
                })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(10), 0, 0)
                    addView(TextView(context, null, 0, R.style.Chip).apply { text = fact.category })
                    addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
                    addView(textView(getString(R.string.remove), 13f, R.color.favorite).apply {
                        setPadding(dp(8), dp(6), dp(8), dp(6))
                        setOnClickListener {
                            store.toggleFavorite(fact)
                            FactWidget.updateAll(this@MainActivity)
                            renderFavorites()
                        }
                    })
                })
            })
        }
        Fonts.apply(list)
    }

    // ---------- הגדרות ----------

    private fun setupSettings() {
        wallpaperSwitch = findViewById(R.id.switch_wallpaper)
        notifySwitch = findViewById(R.id.switch_notify)

        wallpaperSwitch.setOnCheckedChangeListener { _, on ->
            if (on == store.wallpaperEnabled) return@setOnCheckedChangeListener
            store.wallpaperEnabled = on
            val fact = store.current()
            inBackground(if (on) R.string.wallpaper_set else R.string.wallpaper_cleared) {
                if (on) { if (fact != null) WallpaperMaker.setLockScreen(this, fact, store.textSize) }
                else WallpaperMaker.clearLockScreen(this)
            }
        }

        notifySwitch.setOnCheckedChangeListener { _, on ->
            if (on == store.notificationEnabled) return@setOnCheckedChangeListener
            store.notificationEnabled = on
            if (on) {
                if (Notifier.canNotify(this)) store.current()?.let { Notifier.show(this, it) }
                else if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFY)
            } else {
                Notifier.cancel(this)
            }
        }

        findViewById<View>(R.id.button_add_widget).setOnClickListener {
            val manager = AppWidgetManager.getInstance(this)
            if (manager.isRequestPinAppWidgetSupported) {
                manager.requestPinAppWidget(ComponentName(this, FactWidget::class.java), null, null)
            } else {
                toast(R.string.widget_add_unsupported)
            }
        }
    }

    private fun renderSettings() {
        wallpaperSwitch.isChecked = store.wallpaperEnabled
        notifySwitch.isChecked = store.notificationEnabled && Notifier.canNotify(this)

        options(R.id.frequency_options,
            listOf(24 to getString(R.string.freq_day), 12 to getString(R.string.freq_12), 3 to getString(R.string.freq_3)),
            selected = { it == store.frequencyHours }
        ) { store.frequencyHours = it; DailyUpdate.schedule(this, reset = true) }

        options(R.id.hour_options,
            listOf(6, 7, 8, 9, 12, 20).map { it to String.format(Locale.US, "%02d:00", it) },
            selected = { it == store.hour }
        ) { store.hour = it; DailyUpdate.schedule(this, reset = true) }

        options(R.id.category_options,
            store.categories.map { c -> c to "$c  ${store.facts.count { it.category == c }}" },
            selected = { it in store.enabledCategories }
        ) { c ->
            val cats = store.enabledCategories
            store.enabledCategories = if (c in cats) cats - c else cats + c
            DailyUpdate.refresh(this)
        }

        options(R.id.size_options,
            listOf(13 to getString(R.string.size_small), 15 to getString(R.string.size_normal), 17 to getString(R.string.size_large)),
            selected = { it == store.textSize }
        ) { store.textSize = it; DailyUpdate.refresh(this) }

        renderStyles()
    }

    /** שורת צ׳יפים לבחירה. כל לחיצה שומרת ומציירת מחדש. */
    private fun <T> options(containerId: Int, items: List<Pair<T, String>>, selected: (T) -> Boolean, onPick: (T) -> Unit) {
        val box = findViewById<FlowLayout>(containerId)
        box.removeAllViews()
        items.forEach { (value, label) ->
            box.addView(TextView(this).apply {
                text = label
                textSize = 13.5f
                setTextColor(getColorStateList(R.color.option_text))
                setBackgroundResource(R.drawable.bg_option)
                setPadding(dp(13), dp(7), dp(13), dp(7))
                isSelected = selected(value)
                setOnClickListener { onPick(value); renderSettings() }
            })
        }
        Fonts.apply(box)
    }

    private fun renderStyles() {
        val box = findViewById<LinearLayout>(R.id.style_options)
        box.removeAllViews()
        val labels = mapOf(
            WidgetStyle.DARK to R.string.style_dark, WidgetStyle.LIGHT to R.string.style_light,
            WidgetStyle.GLASS to R.string.style_glass, WidgetStyle.BULB to R.string.style_bulb,
        )
        val swatch = mapOf(
            WidgetStyle.DARK to (0xFF0B1A2B.toInt() to 0xFF2A4357.toInt()),
            WidgetStyle.LIGHT to (0xFFFAFBFC.toInt() to 0),
            WidgetStyle.GLASS to (0x40FFFFFF to 0x66FFFFFF),
            WidgetStyle.BULB to (0xFFFFD86B.toInt() to 0),
        )
        WidgetStyle.entries.forEachIndexed { i, style ->
            box.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setBackgroundResource(R.drawable.bg_swatch_frame)
                setPadding(dp(4), dp(4), dp(4), dp(6))
                isSelected = style == store.widgetStyle
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { if (i > 0) marginStart = dp(8) }
                addView(View(context).apply {
                    val (fill, stroke) = swatch.getValue(style)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(fill)
                        if (stroke != 0) setStroke(dp(1), stroke)
                    }
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38))
                })
                addView(textView(getString(labels.getValue(style)), 12.5f, R.color.text_soft).apply { setPadding(0, dp(5), 0, 0) })
                setOnClickListener {
                    store.widgetStyle = style
                    FactWidget.updateAll(this@MainActivity)
                    toast(R.string.widget_updated)
                    renderStyles()
                }
            })
        }
        Fonts.apply(box)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_NOTIFY) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            store.current()?.let { Notifier.show(this, it) }
        } else {
            store.notificationEnabled = false
            notifySwitch.isChecked = false
            toast(R.string.notify_denied)
        }
    }

    // ---------- עזרים ----------

    private fun inBackground(doneMessage: Int, work: () -> Unit) {
        Thread {
            val ok = try { work(); true } catch (e: Exception) { false }
            runOnUiThread { toast(if (ok) doneMessage else R.string.wallpaper_failed) }
        }.start()
    }

    private fun textView(value: String, sizeSp: Float, colorRes: Int) = TextView(this).apply {
        text = value
        textSize = sizeSp
        setTextColor(getColor(colorRes))
    }

    private fun ripple() = TypedValue().let {
        theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
        getDrawable(it.resourceId)
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_SHORT).show()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val REQ_NOTIFY = 1
    }
}
