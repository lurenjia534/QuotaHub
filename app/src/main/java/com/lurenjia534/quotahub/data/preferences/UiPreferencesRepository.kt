package com.lurenjia534.quotahub.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiPreferences(
    val highEmphasisMetrics: Boolean = true,
    val hapticConfirmation: Boolean = true
)

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

    private fun loadPreferences(): UiPreferences {
        return UiPreferences(
            highEmphasisMetrics = sharedPreferences.getBoolean(KEY_HIGH_EMPHASIS_METRICS, true),
            hapticConfirmation = sharedPreferences.getBoolean(KEY_HAPTIC_CONFIRMATION, true)
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "quotahub_ui_preferences"
        const val KEY_HIGH_EMPHASIS_METRICS = "high_emphasis_metrics"
        const val KEY_HAPTIC_CONFIRMATION = "haptic_confirmation"
    }
}
