package com.lurenjia534.quotahub.ui.screens.home

import java.text.NumberFormat
import kotlin.math.abs

internal fun formatTimeRemaining(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

internal fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)

internal fun formatTimeAgo(timestamp: Long): String {
    val elapsed = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    return "${formatTimeRemaining(elapsed)} ago"
}
