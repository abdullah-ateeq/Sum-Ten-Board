package com.example.sumtenboard.data

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val animations: Boolean = true,
    val confirmRestart: Boolean = true,
    val showValidMatches: Boolean = true,
    val showCoordinates: Boolean = false,
    val autoDetectNoMoves: Boolean = true
)

class SettingsStore(private val store: KeyValueStore) {

    fun load(): AppSettings {
        val defaults = AppSettings()
        val theme = store.getString(KEY_THEME, defaults.themeMode.name)
        return AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == theme } ?: defaults.themeMode,
            dynamicColor = store.getBoolean(KEY_DYNAMIC_COLOR, defaults.dynamicColor),
            sound = store.getBoolean(KEY_SOUND, defaults.sound),
            haptics = store.getBoolean(KEY_HAPTICS, defaults.haptics),
            animations = store.getBoolean(KEY_ANIMATIONS, defaults.animations),
            confirmRestart = store.getBoolean(KEY_CONFIRM_RESTART, defaults.confirmRestart),
            showValidMatches = store.getBoolean(KEY_SHOW_VALID_MATCHES, defaults.showValidMatches),
            showCoordinates = store.getBoolean(KEY_SHOW_COORDINATES, defaults.showCoordinates),
            autoDetectNoMoves = store.getBoolean(KEY_AUTO_DETECT, defaults.autoDetectNoMoves)
        )
    }

    fun save(settings: AppSettings) {
        store.edit {
            putString(KEY_THEME, settings.themeMode.name)
            putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
            putBoolean(KEY_SOUND, settings.sound)
            putBoolean(KEY_HAPTICS, settings.haptics)
            putBoolean(KEY_ANIMATIONS, settings.animations)
            putBoolean(KEY_CONFIRM_RESTART, settings.confirmRestart)
            putBoolean(KEY_SHOW_VALID_MATCHES, settings.showValidMatches)
            putBoolean(KEY_SHOW_COORDINATES, settings.showCoordinates)
            putBoolean(KEY_AUTO_DETECT, settings.autoDetectNoMoves)
        }
    }

    fun reset(): AppSettings {
        store.remove(ALL_KEYS)
        return AppSettings()
    }

    companion object {
        const val PREFS_NAME = "sum_ten_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_SOUND = "sound"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_ANIMATIONS = "animations"
        private const val KEY_CONFIRM_RESTART = "confirm_restart"
        private const val KEY_SHOW_VALID_MATCHES = "show_valid_matches"
        private const val KEY_SHOW_COORDINATES = "show_coordinates"
        private const val KEY_AUTO_DETECT = "auto_detect_no_moves"
        private val ALL_KEYS = listOf(
            KEY_THEME, KEY_DYNAMIC_COLOR, KEY_SOUND, KEY_HAPTICS, KEY_ANIMATIONS,
            KEY_CONFIRM_RESTART, KEY_SHOW_VALID_MATCHES, KEY_SHOW_COORDINATES, KEY_AUTO_DETECT
        )
    }
}
