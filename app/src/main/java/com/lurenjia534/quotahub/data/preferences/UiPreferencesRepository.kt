package com.lurenjia534.quotahub.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiPreferences(
    val highEmphasisMetrics: Boolean = true,
    val hapticConfirmation: Boolean = true,
    val forceDarkMode: Boolean = false,
    val themeColorSource: ThemeColorSource = ThemeColorSource.System,
    val themePalette: ThemePalette = ThemePalette.QuotaHub,
    val landscapeMonitorMode: Boolean = false,
    val hideLandscapeMonitorHud: Boolean = true,
    val backgroundRefreshEnabled: Boolean = false,
    val refreshCadence: RefreshCadence = RefreshCadence.Balanced,
    val dismissedUpdateTag: String? = null
)

enum class ThemeColorSource {
    System,
    AppPalette;

    companion object {
        fun fromPersisted(value: String?): ThemeColorSource {
            return entries.firstOrNull { it.name == value } ?: System
        }
    }
}

enum class ThemePalette(
    val title: String,
    val description: String
) {
    QuotaHub(
        title = "QuotaHub",
        description = "Balanced purple tones with clear quota emphasis."
    ),
    Lagoon(
        title = "Lagoon",
        description = "Cool teal and blue tones for a calmer dashboard."
    ),
    Grove(
        title = "Grove",
        description = "Green tonal roles for usage and health scanning."
    ),
    Ember(
        title = "Ember",
        description = "Warm orange accents for stronger action hierarchy."
    ),
    Graphite(
        title = "Graphite",
        description = "Neutral surfaces with restrained mauve emphasis."
    );

    companion object {
        fun fromPersisted(value: String?): ThemePalette {
            return entries.firstOrNull { it.name == value } ?: QuotaHub
        }
    }
}

enum class RefreshCadence(
    val autoRefreshIntervalMillis: Long?
) {
    Live(60 * 1000L),
    Balanced(60 * 60 * 1000L),
    Manual(null);

    companion object {
        fun fromPersisted(value: String?): RefreshCadence {
            return entries.firstOrNull { it.name == value } ?: Balanced
        }
    }
}

class UiPreferencesRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<UiPreferences> = _preferences.asStateFlow()

    fun setHighEmphasisMetrics(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_HIGH_EMPHASIS_METRICS, enabled)
            .apply()

        _preferences.value = _preferences.value.copy(highEmphasisMetrics = enabled)
    }

    fun setHapticConfirmation(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_HAPTIC_CONFIRMATION, enabled)
            .apply()

        _preferences.value = _preferences.value.copy(hapticConfirmation = enabled)
    }

    fun setForceDarkMode(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_FORCE_DARK_MODE, enabled)
            .apply()

        _preferences.value = _preferences.value.copy(forceDarkMode = enabled)
    }

    fun setThemeColorSource(source: ThemeColorSource) {
        sharedPreferences.edit()
            .putString(KEY_THEME_COLOR_SOURCE, source.name)
            .apply()

        _preferences.value = _preferences.value.copy(themeColorSource = source)
    }

    fun setThemePalette(palette: ThemePalette) {
        sharedPreferences.edit()
            .putString(KEY_THEME_PALETTE, palette.name)
            .apply()

        _preferences.value = _preferences.value.copy(themePalette = palette)
    }

    fun setLandscapeMonitorMode(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_LANDSCAPE_MONITOR_MODE, enabled)
            .apply()

        _preferences.value = _preferences.value.copy(landscapeMonitorMode = enabled)
    }

    fun setHideLandscapeMonitorHud(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_HIDE_LANDSCAPE_MONITOR_HUD, enabled)
            .apply()

        _preferences.value = _preferences.value.copy(hideLandscapeMonitorHud = enabled)
    }

    fun setBackgroundRefreshEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_BACKGROUND_REFRESH_ENABLED, enabled)
            .apply()

        _preferences.value = _preferences.value.copy(backgroundRefreshEnabled = enabled)
    }

    fun setRefreshCadence(refreshCadence: RefreshCadence) {
        sharedPreferences.edit()
            .putString(KEY_REFRESH_CADENCE, refreshCadence.name)
            .apply()

        _preferences.value = _preferences.value.copy(refreshCadence = refreshCadence)
    }

    fun setDismissedUpdateTag(tagName: String) {
        sharedPreferences.edit()
            .putString(KEY_DISMISSED_UPDATE_TAG, tagName)
            .apply()

        _preferences.value = _preferences.value.copy(dismissedUpdateTag = tagName)
    }

    private fun loadPreferences(): UiPreferences {
        return UiPreferences(
            highEmphasisMetrics = sharedPreferences.getBoolean(KEY_HIGH_EMPHASIS_METRICS, true),
            hapticConfirmation = sharedPreferences.getBoolean(KEY_HAPTIC_CONFIRMATION, true),
            forceDarkMode = sharedPreferences.getBoolean(KEY_FORCE_DARK_MODE, false),
            themeColorSource = ThemeColorSource.fromPersisted(
                sharedPreferences.getString(KEY_THEME_COLOR_SOURCE, null)
            ),
            themePalette = ThemePalette.fromPersisted(
                sharedPreferences.getString(KEY_THEME_PALETTE, null)
            ),
            landscapeMonitorMode = sharedPreferences.getBoolean(KEY_LANDSCAPE_MONITOR_MODE, false),
            hideLandscapeMonitorHud = sharedPreferences.getBoolean(KEY_HIDE_LANDSCAPE_MONITOR_HUD, true),
            backgroundRefreshEnabled = sharedPreferences.getBoolean(KEY_BACKGROUND_REFRESH_ENABLED, false),
            refreshCadence = RefreshCadence.fromPersisted(
                sharedPreferences.getString(KEY_REFRESH_CADENCE, null)
            ),
            dismissedUpdateTag = sharedPreferences.getString(KEY_DISMISSED_UPDATE_TAG, null)
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "quotahub_ui_preferences"
        const val KEY_HIGH_EMPHASIS_METRICS = "high_emphasis_metrics"
        const val KEY_HAPTIC_CONFIRMATION = "haptic_confirmation"
        const val KEY_FORCE_DARK_MODE = "force_dark_mode"
        const val KEY_THEME_COLOR_SOURCE = "theme_color_source"
        const val KEY_THEME_PALETTE = "theme_palette"
        const val KEY_LANDSCAPE_MONITOR_MODE = "landscape_monitor_mode"
        const val KEY_HIDE_LANDSCAPE_MONITOR_HUD = "hide_landscape_monitor_hud"
        const val KEY_BACKGROUND_REFRESH_ENABLED = "background_refresh_enabled"
        const val KEY_REFRESH_CADENCE = "refresh_cadence"
        const val KEY_DISMISSED_UPDATE_TAG = "dismissed_update_tag"
    }
}
