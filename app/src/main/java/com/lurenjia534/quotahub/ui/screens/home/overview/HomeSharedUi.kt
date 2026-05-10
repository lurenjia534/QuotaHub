package com.lurenjia534.quotahub.ui.screens.home.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.ui.components.MetricEmphasisLevel
import com.lurenjia534.quotahub.ui.components.QuotaLoadingIndicator
import com.lurenjia534.quotahub.ui.components.QuotaMetricText

@Composable
internal fun HomeAnimatedSection(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = spring(stiffness = 420f, dampingRatio = 0.92f)
        ) + slideInVertically(
            animationSpec = spring(stiffness = 420f, dampingRatio = 0.92f),
            initialOffsetY = { it / 7 }
        )
    ) {
        content()
    }
}

@Composable
internal fun HomeLaneHeader(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier
                .padding(top = 5.dp)
                .size(10.dp)
        ) {}
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
internal fun SignalGlyph(
    color: Color,
    content: @Composable () -> Unit
) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.24f)),
        modifier = Modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
internal fun SignalChip(
    label: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
internal fun HomeLoadingRow() {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = colorScheme.primary.copy(alpha = 0.12f),
            shape = CircleShape,
            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.20f)),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                QuotaLoadingIndicator(modifier = Modifier.size(30.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Preparing subscription queue",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Reading local data and provider refresh windows before rendering the queue.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
internal fun HomeErrorStrip(
    message: String,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.errorContainer,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Attention needed",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onErrorContainer
                    )
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colorScheme.onErrorContainer
                    )
                )
            }
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    color = colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
internal fun QueueMetric(
    label: String,
    value: String,
    highEmphasisMetrics: Boolean,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    val resolvedValueColor = if (valueColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurface
    } else {
        valueColor
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        QuotaMetricText(
            text = value,
            emphasized = highEmphasisMetrics,
            level = MetricEmphasisLevel.Standard,
            color = resolvedValueColor
        )
    }
}

@Composable
internal fun syncStateColor(state: SyncState): Color {
    return when (state) {
        SyncState.AuthFailed -> MaterialTheme.colorScheme.error
        SyncState.SyncError -> MaterialTheme.colorScheme.error
        SyncState.Stale -> MaterialTheme.colorScheme.tertiary
        SyncState.Syncing -> MaterialTheme.colorScheme.primary
        SyncState.NeverSynced -> MaterialTheme.colorScheme.onSurfaceVariant
        SyncState.Active -> MaterialTheme.colorScheme.onSurface
    }
}

internal fun Modifier.verticalTimelineLine(color: Color): Modifier {
    return width(2.dp)
        .height(86.dp)
        .background(
            color = color.copy(alpha = 0.30f),
            shape = CircleShape
        )
}
