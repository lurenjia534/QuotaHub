package com.lurenjia534.quotahub.ui.screens.home

import com.lurenjia534.quotahub.format.formatTimeRemaining
import java.text.NumberFormat
import kotlin.math.abs

internal fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)

internal fun formatTimeUntil(timestamp: Long): String {
    val remaining = (timestamp - System.currentTimeMillis()).coerceAtLeast(0L)
    return formatTimeRemaining(remaining)
}

internal fun formatTimeAgo(timestamp: Long): String {
    val elapsed = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    return "${formatTimeRemaining(elapsed)} ago"
}
