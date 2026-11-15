package com.ph.screencast.autodim

import android.content.Context
import android.provider.Settings

/** Saves before dimming so restoration survives a process restart. */
class BrightnessController(context: Context) {
    private val context = context.applicationContext
    private val preferences = this.context.getSharedPreferences("state", Context.MODE_PRIVATE)
    private val resolver = this.context.contentResolver

    fun dim() {
        check(Settings.System.canWrite(context)) { "System settings permission is required." }
        if (!preferences.getBoolean("saved", false)) {
            check(preferences.edit()
                .putInt("brightness", Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128))
                .putInt("mode", Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0))
                .putBoolean("saved", true).commit()) { "Unable to save the previous brightness." }
        }
        check(Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)) {
            "Unable to change brightness mode."
        }
        check(Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 1)) {
            "Unable to change brightness."
        }
    }

    fun restore() {
        if (!preferences.getBoolean("saved", false)) return
        check(Settings.System.canWrite(context)) { "Grant system settings permission again to restore brightness." }
        val brightnessRestored = Settings.System.putInt(
            resolver, Settings.System.SCREEN_BRIGHTNESS, preferences.getInt("brightness", 128)
        )
        val modeRestored = Settings.System.putInt(
            resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, preferences.getInt("mode", 0)
        )
        check(brightnessRestored && modeRestored) { "Unable to restore brightness." }
        check(preferences.edit().putBoolean("saved", false).commit()) { "Unable to save the restoration state." }
    }
}
