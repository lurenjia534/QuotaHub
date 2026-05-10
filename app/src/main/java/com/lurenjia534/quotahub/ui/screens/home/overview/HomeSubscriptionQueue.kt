package com.lurenjia534.quotahub.ui.screens.home.overview

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.ui.screens.home.SubscriptionCardUiModel
import com.lurenjia534.quotahub.ui.screens.home.formatCount
import com.lurenjia534.quotahub.ui.screens.home.formatTimeUntil

@Composable
internal fun SubscriptionSourceSummary(
    subscriptionCards: List<SubscriptionCardUiModel>
) {
    val cloudCount = subscriptionCards.count { it.isCloudSynced }
    val localCount = subscriptionCards.size - cloudCount

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SourceCountPill(
            icon = Icons.Outlined.CloudQueue,
            label = "Cloud",
            count = cloudCount,
            emphasized = cloudCount > 0,
            modifier = Modifier.weight(1f)
        )
        SourceCountPill(
            icon = Icons.Outlined.Storage,
            label = "Local",
            count = localCount,
            emphasized = localCount > 0,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SourceCountPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    emphasized: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val color = if (label == "Cloud") colorScheme.primary else colorScheme.secondary

    Surface(
        modifier = modifier,
        color = if (emphasized) color.copy(alpha = 0.14f) else colorScheme.surfaceContainerLow,
        contentColor = if (emphasized) color else colorScheme.onSurfaceVariant,
        shape = CircleShape,
        border = BorderStroke(
            1.dp,
            if (emphasized) color.copy(alpha = 0.28f) else colorScheme.outlineVariant.copy(alpha = 0.16f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatCount(count),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
            )
        }
    }
}

@Composable
internal fun SubscriptionSourceHeader(
    group: SubscriptionSourceGroup
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = if (group.isCloud) colorScheme.primary else colorScheme.secondary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = accentColor.copy(alpha = 0.16f),
            contentColor = accentColor,
            shape = CircleShape,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.24f)),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (group.isCloud) Icons.Outlined.CloudQueue else Icons.Outlined.Storage,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                SignalChip(
                    label = "${formatCount(group.cards.size)} ${group.label}",
                    color = accentColor
                )
            }
            Text(
                text = group.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
internal fun SubscriptionSignalLane(
    subscriptionCard: SubscriptionCardUiModel,
    highEmphasisMetrics: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val risk = subscriptionCard.risk
    val stateColor by animateColorAsState(
        targetValue = when (risk) {
            QuotaRisk.Critical -> colorScheme.error
            QuotaRisk.Watch -> colorScheme.tertiary
            QuotaRisk.Healthy -> colorScheme.primary
        },
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "subscriptionLaneColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = subscriptionCard.canOpenDetail,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SignalGlyph(color = stateColor) {
                Icon(
                    painter = painterResource(subscriptionCard.providerIconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(31.dp)
                        .padding(2.dp),
                    tint = Color.Unspecified
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(86.dp)
                    .background(
                        color = stateColor.copy(alpha = 0.30f),
                        shape = CircleShape
                    )
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = subscriptionCard.displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = subscriptionCard.subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colorScheme.onSurfaceVariant
                        )
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SignalChip(
                        label = subscriptionCard.sourceLabel,
                        color = if (subscriptionCard.isCloudSynced) {
                            colorScheme.primary
                        } else {
                            colorScheme.secondary
                        }
                    )
                    SignalChip(
                        label = when (risk) {
                            QuotaRisk.Critical -> "Critical"
                            QuotaRisk.Watch -> "Watch"
                            QuotaRisk.Healthy -> "Healthy"
                        },
                        color = stateColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QueueMetric(
                    modifier = Modifier.weight(1f),
                    label = subscriptionCard.primaryMetric.label,
                    value = subscriptionCard.primaryMetric.value,
                    highEmphasisMetrics = highEmphasisMetrics
                )
                QueueMetric(
                    modifier = Modifier.weight(1f),
                    label = subscriptionCard.secondaryMetric?.label ?: "Resources",
                    value = subscriptionCard.secondaryMetric?.value ?: formatCount(subscriptionCard.resourceCount),
                    highEmphasisMetrics = highEmphasisMetrics
                )
                QueueMetric(
                    modifier = Modifier.weight(1f),
                    label = "Next reset",
                    value = subscriptionCard.nextResetAt?.let(::formatTimeUntil) ?: "Manual",
                    highEmphasisMetrics = highEmphasisMetrics
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QueueMetric(
                    modifier = Modifier.weight(1f),
                    label = "Sync",
                    value = subscriptionCard.syncLabel,
                    highEmphasisMetrics = highEmphasisMetrics,
                    valueColor = syncStateColor(subscriptionCard.syncState)
                )
                Text(
                    text = subscriptionCard.syncDescription,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1.25f)
                )
                SignalChip(
                    label = if (subscriptionCard.isCloudSynced) "Cloud source" else "Local source",
                    color = if (subscriptionCard.isCloudSynced) {
                        colorScheme.primary
                    } else {
                        colorScheme.secondary
                    }
                )
                if (subscriptionCard.canOpenDetail) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Unavailable",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeEmptyState(
    onAddClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SignalGlyph(color = colorScheme.secondary) {
            Icon(
                imageVector = Icons.Filled.DataUsage,
                contentDescription = null,
                tint = colorScheme.secondary
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "No quota sources yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Connect a provider to start building the subscription queue and quota overview.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colorScheme.onSurfaceVariant
                )
            )
        }
        FilledTonalButton(
            onClick = onAddClick,
            shape = CircleShape
        ) {
            Text(
                text = "Connect",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
