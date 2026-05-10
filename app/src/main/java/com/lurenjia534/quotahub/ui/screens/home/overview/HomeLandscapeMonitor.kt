package com.lurenjia534.quotahub.ui.screens.home.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.provider.QuotaProgressMetric
import com.lurenjia534.quotahub.ui.screens.home.SubscriptionCardUiModel
import com.lurenjia534.quotahub.ui.screens.home.formatCount
import com.lurenjia534.quotahub.ui.screens.home.formatTimeUntil
import kotlin.math.roundToInt

@Composable
internal fun LandscapeHubStatusBar(
    connectedCount: Int,
    providerCount: Int,
    trackedResources: Int,
    nextRefreshWindow: Long?,
    highEmphasisMetrics: Boolean,
    dominantRisk: QuotaRisk,
    dominantSyncState: SyncState,
    isBootstrapping: Boolean,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit
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
        label = "landscapeHubAccent"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.widthIn(min = 142.dp, max = 174.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(10.dp),
                color = accentColor,
                shape = CircleShape
            ) {}
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    text = "QuotaHub",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isBootstrapping) "Loading" else if (connectedCount == 0) "Idle" else "Live monitor",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LandscapeHudMetricChip(
                label = "${formatCount(connectedCount)} sources",
                highEmphasisMetrics = highEmphasisMetrics
            )
            LandscapeHudMetricChip(
                label = "${formatCount(providerCount)} providers",
                highEmphasisMetrics = highEmphasisMetrics
            )
            LandscapeHudMetricChip(
                label = "${formatCount(trackedResources)} items",
                highEmphasisMetrics = highEmphasisMetrics
            )
            LandscapeHudMetricChip(
                label = "next ${nextRefreshWindow?.let(::formatTimeUntil) ?: "--"}",
                highEmphasisMetrics = highEmphasisMetrics
            )
        }

        LandscapeHudAction(
            icon = Icons.Filled.Settings,
            contentDescription = "Open settings",
            color = colorScheme.onSurfaceVariant,
            onClick = onSettingsClick
        )
        LandscapeHudAction(
            icon = Icons.Filled.Add,
            contentDescription = "Add provider",
            color = colorScheme.onSurface,
            onClick = onAddClick
        )
    }
}

@Composable
private fun LandscapeHudMetricChip(
    label: String,
    highEmphasisMetrics: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
        contentColor = colorScheme.onSurfaceVariant,
        shape = CircleShape,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.16f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (highEmphasisMetrics) FontWeight.SemiBold else FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
internal fun LandscapeHudAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.84f),
        contentColor = color,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LandscapeProviderHeader() {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Provider",
            style = MaterialTheme.typography.labelSmall.copy(
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.widthIn(min = 196.dp, max = 234.dp)
        )
        Text(
            text = "Quota windows",
            style = MaterialTheme.typography.labelSmall.copy(
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun LandscapeProviderMonitorTable(
    subscriptionCards: List<SubscriptionCardUiModel>,
    highEmphasisMetrics: Boolean,
    isBootstrapping: Boolean,
    hasSubscriptions: Boolean,
    showHeader: Boolean,
    onSubscriptionClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        when {
            isBootstrapping -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    HomeLoadingRow()
                }
            }
            hasSubscriptions -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = showHeader,
                        enter = fadeIn(
                            animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f)
                        ) + slideInVertically(
                            animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
                            initialOffsetY = { -it / 2 }
                        ),
                        exit = fadeOut(
                            animationSpec = spring(stiffness = 500f, dampingRatio = 0.95f)
                        ) + slideOutVertically(
                            animationSpec = spring(stiffness = 500f, dampingRatio = 0.95f),
                            targetOffsetY = { -it / 2 }
                        )
                    ) {
                        LandscapeProviderHeader()
                    }
                    val groups = subscriptionCards.sourceGroups()
                    groups.forEachIndexed { groupIndex, group ->
                        LandscapeSourceHeader(group = group)
                        group.cards.forEachIndexed { cardIndex, subscriptionCard ->
                            HubProviderSignalRow(
                                subscriptionCard = subscriptionCard,
                                highEmphasisMetrics = highEmphasisMetrics,
                                onClick = { onSubscriptionClick(subscriptionCard.subscriptionId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                            if (cardIndex < group.cards.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
                                )
                            }
                        }
                        if (groupIndex < groups.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                thickness = 2.dp
                            )
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    HomeEmptyState(onAddClick = onAddClick)
                }
            }
        }
    }
}

@Composable
private fun HubProviderSignalRow(
    subscriptionCard: SubscriptionCardUiModel,
    highEmphasisMetrics: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val stateColor by animateColorAsState(
        targetValue = when (subscriptionCard.risk) {
            QuotaRisk.Critical -> colorScheme.error
            QuotaRisk.Watch -> colorScheme.tertiary
            QuotaRisk.Healthy -> colorScheme.primary
        },
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "landscapeProviderColor"
    )
    val progressPercent = subscriptionCard.primaryMetric.value.percentValueOrNull()

    Row(
        modifier = modifier
            .clickable(
                enabled = subscriptionCard.canOpenDetail,
                onClick = onClick
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 18.dp
            ),
            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.42f)),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(subscriptionCard.providerIconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(2.dp),
                    tint = Color.Unspecified
                )
            }
        }

        Column(
            modifier = Modifier.widthIn(min = 144.dp, max = 182.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = subscriptionCard.displayTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when (subscriptionCard.risk) {
                    QuotaRisk.Critical -> "Critical"
                    QuotaRisk.Watch -> "Watch"
                    QuotaRisk.Healthy -> subscriptionCard.syncLabel
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (subscriptionCard.risk == QuotaRisk.Healthy) {
                        colorScheme.onSurfaceVariant
                    } else {
                        stateColor
                    },
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val metrics = subscriptionCard.hubProgressMetrics.landscapeMetricOrder()
        if (metrics.isNotEmpty()) {
            HubStackedProgressStrip(
                metrics = metrics,
                color = stateColor,
                modifier = Modifier.weight(1f)
            )
        } else {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QueueMetric(
                    modifier = Modifier.weight(0.8f),
                    label = subscriptionCard.primaryMetric.label,
                    value = subscriptionCard.primaryMetric.value,
                    highEmphasisMetrics = highEmphasisMetrics
                )
                if (progressPercent != null) {
                    LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(14.dp)
                            .clip(CircleShape),
                        color = stateColor,
                        trackColor = stateColor.copy(alpha = 0.16f)
                    )
                } else {
                    QueueMetric(
                        modifier = Modifier.weight(1f),
                        label = subscriptionCard.secondaryMetric?.label ?: "Resources",
                        value = subscriptionCard.secondaryMetric?.value ?: formatCount(subscriptionCard.resourceCount),
                        highEmphasisMetrics = highEmphasisMetrics
                    )
                }
            }
        }
    }
}

@Composable
private fun LandscapeSourceHeader(
    group: SubscriptionSourceGroup
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = if (group.isCloud) colorScheme.primary else colorScheme.secondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (group.isCloud) Icons.Outlined.CloudQueue else Icons.Outlined.Storage,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = group.title,
            style = MaterialTheme.typography.labelLarge.copy(
                color = accentColor,
                fontWeight = FontWeight.Black
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            color = accentColor.copy(alpha = 0.14f),
            contentColor = accentColor,
            shape = CircleShape
        ) {
            Text(
                text = formatCount(group.cards.size),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = accentColor.copy(alpha = 0.22f)
        )
    }
}

@Composable
private fun HubStackedProgressStrip(
    metrics: List<QuotaProgressMetric>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        metrics.take(2).forEach { metric ->
            HubStackedProgressMetric(
                metric = metric,
                color = color
            )
        }
    }
}

@Composable
private fun HubStackedProgressMetric(
    metric: QuotaProgressMetric,
    color: Color
) {
    val colorScheme = MaterialTheme.colorScheme
    val targetProgress = if (metric.total > 0L) {
        metric.used.toFloat() / metric.total.toFloat()
    } else {
        0f
    }.coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(stiffness = 360f, dampingRatio = 0.86f),
        label = "landscapeQuotaProgress"
    )
    val remainingText = metric.remaining?.let {
        "${formatLongCount(it)} left"
    } ?: "${(progress * 100).roundToInt()}% used"
    val resetText = metric.resetAtEpochMillis?.let(::formatTimeUntil) ?: "rolling"
    val trackShape = RoundedCornerShape(percent = 50)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = metric.label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(min = 78.dp, max = 96.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(trackShape),
            contentAlignment = Alignment.Center
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .matchParentSize()
                    .clip(trackShape),
                color = color,
                trackColor = colorScheme.surfaceContainerHigh
            )
            Text(
                text = "${formatLongCount(metric.used)} / ${formatLongCount(metric.total)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "$remainingText / $resetText",
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(min = 112.dp, max = 146.dp)
        )
    }
}
