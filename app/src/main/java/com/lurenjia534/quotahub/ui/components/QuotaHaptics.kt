package com.lurenjia534.quotahub.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

class QuotaHaptics(
    private val enabled: Boolean,
    private val performFeedback: (HapticFeedbackType) -> Unit
) {
    fun toggle(checked: Boolean, force: Boolean = false) {
        if (!enabled && !force) return
        performFeedback(
            if (checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
        )
    }

    fun refreshThreshold() {
        if (!enabled) return
        performFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    fun refreshResult(success: Boolean) {
        if (!enabled) return
        performFeedback(
            if (success) HapticFeedbackType.Confirm else HapticFeedbackType.Reject
        )
    }
}

@Composable
fun rememberQuotaHaptics(enabled: Boolean): QuotaHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(enabled, hapticFeedback) {
        QuotaHaptics(
            enabled = enabled,
            performFeedback = hapticFeedback::performHapticFeedback
        )
    }
}
