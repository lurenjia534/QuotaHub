package com.lurenjia534.quotahub.ui.screens.home.overview

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.ui.components.MetricEmphasisLevel
import com.lurenjia534.quotahub.ui.components.QuotaLoadingIndicator
import com.lurenjia534.quotahub.ui.components.QuotaMetricText
import com.lurenjia534.quotahub.ui.screens.home.formatCount
import com.lurenjia534.quotahub.ui.screens.home.formatTimeUntil

@Composable
internal fun HomeRadarSurface(
    connectedCount: Int,
    providerCount: Int,
    trackedResources: Int,
    nextRefreshWindow: Long?,
    highEmphasisMetrics: Boolean,
    dominantRisk: QuotaRisk,
    dominantSyncState: SyncState,
    isBootstrapping: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor by animateColorAsState(
        targetValue = when {
            connectedCount == 0 -> colorScheme.onSurfaceVariant
            dominantSyncState == SyncState.AuthFailed -> colorScheme.error
            dominantSyncState == SyncState.SyncError -> colorScheme.error
            dominantSyncState == SyncState.Stale -> colorScheme.tertiary
            dominantSyncState == SyncState.Syncing -> colorScheme.primary
            dominantSyncState == SyncState.NeverSynced -> colorScheme.secondary
            dominantRisk == QuotaRisk.Critical -> colorScheme.error
            dominantRisk == QuotaRisk.Watch -> colorScheme.tertiary
            else -> colorScheme.primary
        },
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "radarAccent"
    )
    val scanScale by animateFloatAsState(
        targetValue = if (isBootstrapping || dominantSyncState == SyncState.Syncing) 1.04f else 1f,
        animationSpec = spring(stiffness = 320f, dampingRatio = 0.78f),
        label = "radarScanScale"
    )

    val stateLabel = when {
        connectedCount == 0 -> "No sources connected"
        dominantSyncState == SyncState.AuthFailed -> "Credentials need attention"
        dominantSyncState == SyncState.SyncError -> "Sync issues detected"
        dominantSyncState == SyncState.Stale -> "Snapshots getting stale"
        dominantSyncState == SyncState.Syncing -> "Sync in progress"
        dominantSyncState == SyncState.NeverSynced -> "Awaiting first sync"
        dominantRisk == QuotaRisk.Critical -> "Critical attention"
        dominantRisk == QuotaRisk.Watch -> "Watch list active"
        else -> "Coverage healthy"
    }
    val stateDescription = when {
        connectedCount == 0 -> "Connect a provider to begin tracking quotas and refresh windows."
        dominantSyncState == SyncState.AuthFailed -> "At least one subscription can no longer authenticate and needs updated credentials."
        dominantSyncState == SyncState.SyncError -> "At least one subscription failed to refresh and is serving cached data."
        dominantSyncState == SyncState.Stale -> "Some cached quota snapshots have not refreshed recently and may no longer be current."
        dominantSyncState == SyncState.Syncing -> "A provider refresh is currently in flight."
        dominantSyncState == SyncState.NeverSynced -> "One or more subscriptions are connected but still waiting for their first successful snapshot."
        dominantRisk == QuotaRisk.Critical -> "At least one source is close to exhausting its available quota."
        dominantRisk == QuotaRisk.Watch -> "Some sources are trending low and should be monitored."
        else -> "Tracked sources are connected and current quota data is readable at a glance."
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "QuotaHub",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
                )
                Text(
                    text = "Quota radar",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = colorScheme.onSurfaceVariant
                    )
                )
            }
            Surface(
                color = accentColor.copy(alpha = 0.16f),
                contentColor = accentColor,
                shape = CircleShape,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.32f))
            ) {
                Text(
                    text = if (connectedCount == 0) "Idle" else "Live",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadarDial(
                trackedResources = trackedResources,
                accentColor = accentColor,
                highEmphasisMetrics = highEmphasisMetrics,
                scanScale = scanScale
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RadarReadout(
                    label = "Sources",
                    value = formatCount(connectedCount),
                    accentColor = accentColor,
                    highEmphasisMetrics = highEmphasisMetrics
                )
                RadarReadout(
                    label = "Providers",
                    value = formatCount(providerCount),
                    accentColor = colorScheme.secondary,
                    highEmphasisMetrics = highEmphasisMetrics
                )
                RadarReadout(
                    label = "Next reset",
                    value = nextRefreshWindow?.let(::formatTimeUntil) ?: "Waiting",
                    accentColor = colorScheme.tertiary,
                    highEmphasisMetrics = highEmphasisMetrics
                )
            }
        }

        RadarStatusBand(
            label = stateLabel,
            description = stateDescription,
            accentColor = accentColor
        )

        if (isBootstrapping) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuotaLoadingIndicator(modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Loading cached snapshots and provider state",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
internal fun CompactHomeRadarSurface(
    connectedCount: Int,
    providerCount: Int,
    trackedResources: Int,
    nextRefreshWindow: Long?,
    highEmphasisMetrics: Boolean,
    dominantRisk: QuotaRisk,
    dominantSyncState: SyncState,
    isBootstrapping: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor by animateColorAsState(
        targetValue = when {
            connectedCount == 0 -> colorScheme.onSurfaceVariant
            dominantSyncState == SyncState.AuthFailed -> colorScheme.error
            dominantSyncState == SyncState.SyncError -> colorScheme.error
            dominantSyncState == SyncState.Stale -> colorScheme.tertiary
            dominantSyncState == SyncState.Syncing -> colorScheme.primary
            dominantSyncState == SyncState.NeverSynced -> colorScheme.secondary
            dominantRisk == QuotaRisk.Critical -> colorScheme.error
            dominantRisk == QuotaRisk.Watch -> colorScheme.tertiary
            else -> colorScheme.primary
        },
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "compactRadarAccent"
    )
    val scanScale by animateFloatAsState(
        targetValue = if (isBootstrapping || dominantSyncState == SyncState.Syncing) 1.04f else 1f,
        animationSpec = spring(stiffness = 320f, dampingRatio = 0.78f),
        label = "compactRadarScanScale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.surfaceContainerLow.copy(alpha = 0.78f),
        shape = RoundedCornerShape(
            topStart = 34.dp,
            topEnd = 22.dp,
            bottomStart = 22.dp,
            bottomEnd = 38.dp
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.20f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "QuotaHub",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                    )
                    Text(
                        text = "Live quota radar",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colorScheme.onSurfaceVariant
                        )
                    )
                }
                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    contentColor = accentColor,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.30f))
                ) {
                    Text(
                        text = if (connectedCount == 0) "Idle" else "Live",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactRadarDial(
                    trackedResources = trackedResources,
                    accentColor = accentColor,
                    highEmphasisMetrics = highEmphasisMetrics,
                    scanScale = scanScale
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadarReadout(
                        label = "Sources",
                        value = formatCount(connectedCount),
                        accentColor = accentColor,
                        highEmphasisMetrics = highEmphasisMetrics
                    )
                    RadarReadout(
                        label = "Providers",
                        value = formatCount(providerCount),
                        accentColor = colorScheme.secondary,
                        highEmphasisMetrics = highEmphasisMetrics
                    )
                    RadarReadout(
                        label = "Next reset",
                        value = nextRefreshWindow?.let(::formatTimeUntil) ?: "Waiting",
                        accentColor = colorScheme.tertiary,
                        highEmphasisMetrics = highEmphasisMetrics
                    )
                }
            }
        }
    }
}

@Composable
private fun RadarDial(
    trackedResources: Int,
    accentColor: Color,
    highEmphasisMetrics: Boolean,
    scanScale: Float
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.size(172.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(172.dp),
            color = accentColor.copy(alpha = 0.06f),
            shape = CircleShape,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f))
        ) {}
        Surface(
            modifier = Modifier.size(132.dp),
            color = accentColor.copy(alpha = 0.10f),
            shape = CircleShape,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.26f))
        ) {}
        Surface(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = scanScale
                    scaleY = scanScale
                },
            color = colorScheme.surfaceContainerHigh,
            shape = CircleShape,
            border = BorderStroke(2.dp, accentColor.copy(alpha = 0.72f)),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                QuotaMetricText(
                    text = formatCount(trackedResources),
                    emphasized = highEmphasisMetrics,
                    level = MetricEmphasisLevel.Hero,
                    color = accentColor
                )
                Text(
                    text = "resources",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun CompactRadarDial(
    trackedResources: Int,
    accentColor: Color,
    highEmphasisMetrics: Boolean,
    scanScale: Float
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.size(126.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(126.dp),
            color = accentColor.copy(alpha = 0.06f),
            shape = CircleShape,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f))
        ) {}
        Surface(
            modifier = Modifier.size(94.dp),
            color = accentColor.copy(alpha = 0.10f),
            shape = CircleShape,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.26f))
        ) {}
        Surface(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = scanScale
                    scaleY = scanScale
                },
            color = colorScheme.surfaceContainerHigh,
            shape = CircleShape,
            border = BorderStroke(2.dp, accentColor.copy(alpha = 0.72f)),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                QuotaMetricText(
                    text = formatCount(trackedResources),
                    emphasized = highEmphasisMetrics,
                    level = MetricEmphasisLevel.Standard,
                    color = accentColor
                )
                Text(
                    text = "items",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun RadarReadout(
    label: String,
    value: String,
    accentColor: Color,
    highEmphasisMetrics: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = accentColor,
            shape = CircleShape,
            modifier = Modifier.size(9.dp)
        ) {}
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = colorScheme.onSurfaceVariant
                )
            )
            QuotaMetricText(
                text = value,
                emphasized = highEmphasisMetrics,
                level = MetricEmphasisLevel.Standard,
                color = colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RadarStatusBand(
    label: String,
    description: String,
    accentColor: Color
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
        shape = CircleShape,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.18f),
                shape = CircleShape,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        color = accentColor,
                        shape = CircleShape,
                        modifier = Modifier.size(10.dp)
                    ) {}
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
