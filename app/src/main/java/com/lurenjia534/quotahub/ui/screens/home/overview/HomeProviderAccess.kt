package com.lurenjia534.quotahub.ui.screens.home.overview

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.ui.screens.home.SubscriptionCardUiModel
import com.lurenjia534.quotahub.ui.provider.ProviderUiRegistry

@Composable
internal fun ProviderAccessSection(
    providers: List<ProviderDescriptor>,
    providerUiRegistry: ProviderUiRegistry,
    subscriptionCards: List<SubscriptionCardUiModel>,
    connectedProviderIds: Set<String>,
    onProviderClick: (ProviderDescriptor) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeLaneHeader(
            title = "Provider access",
            subtitle = "Manage provider connections and additional accounts."
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorScheme.surfaceContainerLow.copy(alpha = 0.74f),
            shape = RoundedCornerShape(
                topStart = 30.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 34.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.26f)
            ),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                providers.forEachIndexed { index, provider ->
                    ProviderAccessRow(
                        provider = provider,
                        providerUiRegistry = providerUiRegistry,
                        providerCards = subscriptionCards.filter { it.providerId == provider.id },
                        isConnected = connectedProviderIds.contains(provider.id),
                        onProviderClick = onProviderClick
                    )
                    if (index < providers.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp, end = 18.dp),
                            color = colorScheme.outlineVariant.copy(alpha = 0.20f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderAccessRow(
    provider: ProviderDescriptor,
    providerUiRegistry: ProviderUiRegistry,
    providerCards: List<SubscriptionCardUiModel>,
    isConnected: Boolean,
    onProviderClick: (ProviderDescriptor) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val providerUi = providerUiRegistry.require(provider)
    val targetStatusColor = providerCards.providerAccessColor()
    val statusColor by animateColorAsState(
        targetValue = targetStatusColor,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "providerStatusColor"
    )
    val statusLabel = providerCards.providerStatusLabel(isConnected)
    val summary = providerCards.providerAccessSummary()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProviderClick(provider) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = statusColor.copy(alpha = 0.13f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.22f)),
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(providerUi.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.Unspecified
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = provider.displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        ProviderAccessPill(
            label = statusLabel,
            color = statusColor
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
        )
    }
}

@Composable
private fun ProviderAccessPill(
    label: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        contentColor = color,
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun List<SubscriptionCardUiModel>.providerAccessColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when {
        isEmpty() -> colorScheme.onSurfaceVariant
        any { it.syncState == SyncState.AuthFailed || it.syncState == SyncState.SyncError } -> colorScheme.error
        any { it.risk == QuotaRisk.Critical } -> colorScheme.error
        any { it.risk == QuotaRisk.Watch || it.syncState == SyncState.Stale } -> colorScheme.tertiary
        else -> colorScheme.primary
    }
}
