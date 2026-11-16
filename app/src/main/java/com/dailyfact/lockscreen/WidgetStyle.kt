package com.dailyfact.lockscreen

/** ארבעת הסגנונות של הווידג׳ט. */
enum class WidgetStyle(
    val key: String,
    val background: Int,
    val chipBackground: Int,
    val buttonBackground: Int,
    val text: Int,
    val accent: Int,
    val chipText: Int,
) {
    DARK("dark", R.drawable.widget_bg_dark, R.drawable.widget_chip_dark, R.drawable.widget_btn_dark,
        0xFFFFFFFF.toInt(), 0xFF8FD3E8.toInt(), 0xFFCDEBF4.toInt()),
    LIGHT("light", R.drawable.widget_bg_light, R.drawable.widget_chip_light, R.drawable.widget_btn_light,
        0xFF13212E.toInt(), 0xFF1D5A70.toInt(), 0xFF1D5A70.toInt()),
    GLASS("glass", R.drawable.widget_bg_glass, R.drawable.widget_chip_glass, R.drawable.widget_btn_glass,
        0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()),
    BULB("bulb", R.drawable.widget_bg_bulb, R.drawable.widget_chip_bulb, R.drawable.widget_btn_bulb,
        0xFF2A2208.toInt(), 0xFF5A4500.toInt(), 0xFF5A4500.toInt());

    companion object {
        fun of(key: String?): WidgetStyle = entries.firstOrNull { it.key == key } ?: DARK
    }
}
