package com.lurenjia534.quotahub.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.provider.SubscriptionRegistry
import com.lurenjia534.quotahub.ui.components.MetricEmphasisLevel
import com.lurenjia534.quotahub.ui.components.QuotaMetricText
import com.lurenjia534.quotahub.ui.components.QuotaLoadingIndicator
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    subscriptionRegistry: SubscriptionRegistry,
    highEmphasisMetrics: Boolean,
    bottomContentPadding: Dp = 0.dp,
    onSubscriptionClick: (Long) -> Unit
) {
    val viewModel: HomeHubViewModel = viewModel(
        factory = HomeHubViewModel.Factory(subscriptionRegistry)
    )
    val uiState by viewModel.uiState.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val connectedSubscriptions = uiState.subscriptionCards.filter { it.isConnected }
    val connectedCount = connectedSubscriptions.size
    val providerCount = subscriptionRegistry.providers.size
    val trackedCalls = connectedSubscriptions.sumOf { it.remainingCalls }
    val trackedModels = connectedSubscriptions.sumOf { it.modelCount }
    val nextRefreshWindow = connectedSubscriptions.mapNotNull { it.remainingTime }.minOrNull()
    val dominantRisk = connectedSubscriptions.dominantRisk()
    val hasSubscriptions = connectedSubscriptions.isNotEmpty()
    val connectedProviderKeys = connectedSubscriptions
        .map { it.subtitle.substringAfterLast("•").trim() }
        .toSet()

    var boardVisible by remember { mutableStateOf(false) }
    var statusVisible by remember { mutableStateOf(false) }
    var queueVisible by remember { mutableStateOf(false) }
    var providerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        boardVisible = true
        delay(70)
        statusVisible = true
        delay(70)
        queueVisible = true
        delay(70)
        providerVisible = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 32.dp + bottomContentPadding
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                AnimatedSection(visible = boardVisible) {
                    HomeOperationsBoard(
                        connectedCount = connectedCount,
                        providerCount = providerCount,
                        trackedCalls = trackedCalls,
                        trackedModels = trackedModels,
                        nextRefreshWindow = nextRefreshWindow,
                        highEmphasisMetrics = highEmphasisMetrics,
                        dominantRisk = dominantRisk,
                        isBootstrapping = uiState.isBootstrapping,
                        onAddClick = { showBottomSheet = true }
                    )
                }
            }

            if (uiState.error != null && !uiState.showCredentialDialog) {
                item {
                    AnimatedSection(visible = statusVisible) {
                        HomeErrorStrip(
                            message = uiState.error!!,
                            onDismiss = viewModel::clearError
                        )
                    }
                }
            }

            item {
                AnimatedSection(visible = statusVisible) {
                    HomeSectionHeader(
                        title = if (hasSubscriptions) "Subscription queue" else "Subscriptions",
                        subtitle = if (hasSubscriptions) {
                            "Active sources ordered for fast scanning. Open any row for model-level quota detail."
                        } else {
                            "No sources connected yet. Add a provider to start caching quota snapshots."
                        }
                    )
                }
            }

            if (uiState.isBootstrapping) {
                item {
                    AnimatedSection(visible = queueVisible) {
                        HomeLoadingRow()
                    }
                }
            } else if (hasSubscriptions) {
                items(
                    items = connectedSubscriptions,
                    key = { it.subscriptionId }
                ) { subscriptionCard ->
                    SubscriptionQueueRow(
                        subscriptionCard = subscriptionCard,
                        highEmphasisMetrics = highEmphasisMetrics,
                        onClick = { onSubscriptionClick(subscriptionCard.subscriptionId) }
                    )
                }
            } else {
                item {
                    AnimatedSection(visible = queueVisible) {
                        HomeEmptyState(onAddClick = { showBottomSheet = true })
                    }
                }
            }

            item {
                AnimatedSection(visible = providerVisible) {
                    ProviderAccessSection(
                        providers = subscriptionRegistry.providers,
                        connectedProviderKeys = connectedProviderKeys,
                        onAddClick = { showBottomSheet = true }
                    )
                }
            }
        }

        if (showBottomSheet) {
            ProviderBottomSheet(
                sheetState = sheetState,
                onDismiss = { showBottomSheet = false },
                providers = subscriptionRegistry.providers,
                onProviderClick = { provider ->
                    viewModel.clearError()
                    viewModel.showCredentialDialog(provider)
                    showBottomSheet = false
                }
            )
        }

        if (uiState.showCredentialDialog) {
            ProviderApiKeyDialog(
                provider = uiState.selectedProvider ?: QuotaProvider.MiniMax,
                customTitleInput = uiState.customTitleInput,
                credentialInput = uiState.credentialInput,
                isSaving = uiState.isSaving,
                errorMessage = uiState.error,
                onCustomTitleChange = viewModel::updateCustomTitleInput,
                onCredentialChange = viewModel::updateCredentialInput,
                onDismiss = {
                    viewModel.clearError()
                    viewModel.hideCredentialDialog()
                },
                onConfirm = viewModel::saveSelectedProviderCredential
            )
        }
    }
}

@Composable
private fun AnimatedSection(
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
private fun HomeOperationsBoard(
    connectedCount: Int,
    providerCount: Int,
    trackedCalls: Int,
    trackedModels: Int,
    nextRefreshWindow: Long?,
    highEmphasisMetrics: Boolean,
    dominantRisk: QuotaRisk,
    isBootstrapping: Boolean,
    onAddClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor by animateColorAsState(
        targetValue = when {
            connectedCount == 0 -> colorScheme.secondaryContainer
            dominantRisk == QuotaRisk.Critical -> colorScheme.errorContainer
            dominantRisk == QuotaRisk.Watch -> colorScheme.tertiaryContainer
            else -> colorScheme.primaryContainer
        },
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "boardAccent"
    )

    val stateLabel = when {
        connectedCount == 0 -> "No sources connected"
        dominantRisk == QuotaRisk.Critical -> "Critical attention"
        dominantRisk == QuotaRisk.Watch -> "Watch list active"
        else -> "Coverage healthy"
    }
    val stateDescription = when {
        connectedCount == 0 -> "Connect a provider to begin tracking quotas and refresh windows."
        dominantRisk == QuotaRisk.Critical -> "At least one source is close to exhausting its available quota."
        dominantRisk == QuotaRisk.Watch -> "Some sources are trending low and should be monitored."
        else -> "Tracked sources are connected and current quota data is readable at a glance."
    }

    Surface(
        color = colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(34.dp),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.34f),
                            colorScheme.surfaceContainerHigh
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "QuotaHub",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Operational overview",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    FilledTonalButton(
                        onClick = onAddClick,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1.3f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Tracked calls",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = colorScheme.onSurfaceVariant
                            )
                        )
                        QuotaMetricText(
                            text = formatCount(trackedCalls),
                            emphasized = highEmphasisMetrics,
                            level = MetricEmphasisLevel.Hero
                        )
                        Text(
                            text = stateDescription,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = colorScheme.surface.copy(alpha = 0.84f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MiniBoardMetric(
                                label = "Connected",
                                value = "$connectedCount / $providerCount",
                                highEmphasisMetrics = highEmphasisMetrics
                            )
                            HorizontalDivider(
                                color = colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                            MiniBoardMetric(
                                label = "Models visible",
                                value = formatCount(trackedModels),
                                highEmphasisMetrics = highEmphasisMetrics
                            )
                        }
                    }
                }

                Surface(
                    color = colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OverviewStripMetric(
                            modifier = Modifier.weight(1f),
                            label = "Status",
                            value = stateLabel,
                            highEmphasisMetrics = highEmphasisMetrics,
                            valueColor = colorScheme.onSurface
                        )
                        StripDivider()
                        OverviewStripMetric(
                            modifier = Modifier.weight(1f),
                            label = "Next reset",
                            value = nextRefreshWindow?.let { formatTimeRemaining(it) } ?: "Waiting",
                            highEmphasisMetrics = highEmphasisMetrics,
                            valueColor = colorScheme.onSurface
                        )
                        StripDivider()
                        OverviewStripMetric(
                            modifier = Modifier.weight(1f),
                            label = "Sources",
                            value = if (connectedCount == 0) "Pending" else "Live",
                            highEmphasisMetrics = highEmphasisMetrics,
                            valueColor = colorScheme.onSurface
                        )
                    }
                }

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
    }
}

@Composable
private fun MiniBoardMetric(
    label: String,
    value: String,
    highEmphasisMetrics: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        QuotaMetricText(
            text = value,
            emphasized = highEmphasisMetrics,
            level = MetricEmphasisLevel.Standard
        )
    }
}

@Composable
private fun OverviewStripMetric(
    label: String,
    value: String,
    highEmphasisMetrics: Boolean,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
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
            color = valueColor
        )
    }
}

@Composable
private fun StripDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}

@Composable
private fun HomeSectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun HomeLoadingRow() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuotaLoadingIndicator(modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Preparing subscription queue",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Reading local data and provider refresh windows before rendering the queue.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeErrorStrip(
    message: String,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.errorContainer,
        shape = RoundedCornerShape(26.dp)
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
private fun SubscriptionQueueRow(
    subscriptionCard: SubscriptionCardUiModel,
    highEmphasisMetrics: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val risk = subscriptionCard.risk
    val stateColor by animateColorAsState(
        targetValue = when (risk) {
            QuotaRisk.Critical -> colorScheme.errorContainer
            QuotaRisk.Watch -> colorScheme.tertiaryContainer
            QuotaRisk.Healthy -> colorScheme.secondaryContainer
        },
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "subscriptionStateColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = stateColor,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        painter = painterResource(subscriptionCard.providerIconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(10.dp),
                        tint = Color.Unspecified
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
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
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Next reset",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = colorScheme.onSurfaceVariant
                        )
                    )
                    QuotaMetricText(
                        text = subscriptionCard.remainingTime?.let { formatTimeRemaining(it) } ?: "Manual",
                        emphasized = highEmphasisMetrics,
                        level = MetricEmphasisLevel.Standard,
                        color = colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QueueMetric(
                    modifier = Modifier.weight(1f),
                    label = "Calls",
                    value = formatCount(subscriptionCard.remainingCalls),
                    highEmphasisMetrics = highEmphasisMetrics
                )
                QueueMetric(
                    modifier = Modifier.weight(1f),
                    label = "Models",
                    value = formatCount(subscriptionCard.modelCount),
                    highEmphasisMetrics = highEmphasisMetrics
                )
                QueueMetric(
                    modifier = Modifier.weight(1f),
                    label = "State",
                    value = when (risk) {
                        QuotaRisk.Critical -> "Critical"
                        QuotaRisk.Watch -> "Watch"
                        QuotaRisk.Healthy -> "Healthy"
                    },
                    highEmphasisMetrics = highEmphasisMetrics,
                    valueColor = when (risk) {
                        QuotaRisk.Critical -> colorScheme.error
                        QuotaRisk.Watch -> colorScheme.tertiary
                        QuotaRisk.Healthy -> colorScheme.onSurface
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Open model-level detail and refresh controls",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colorScheme.onSurfaceVariant
                    )
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun QueueMetric(
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

private fun List<SubscriptionCardUiModel>.dominantRisk(): QuotaRisk {
    return when {
        any { it.risk == QuotaRisk.Critical } -> QuotaRisk.Critical
        any { it.risk == QuotaRisk.Watch } -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

@Composable
private fun HomeEmptyState(
    onAddClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(30.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.DataUsage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            FilledTonalButton(
                onClick = onAddClick,
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "Connect",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun ProviderAccessSection(
    providers: List<QuotaProvider>,
    connectedProviderKeys: Set<String>,
    onAddClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeSectionHeader(
            title = "Provider access",
            subtitle = "Keep connection entry points visible on the home surface instead of hiding them behind menus."
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(30.dp),
            tonalElevation = 1.dp
        ) {
            Column {
                providers.forEachIndexed { index, provider ->
                    ProviderAccessRow(
                        provider = provider,
                        isConnected = connectedProviderKeys.contains(provider.subtitle),
                        onAddClick = onAddClick
                    )

                    if (index < providers.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 80.dp, end = 18.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderAccessRow(
    provider: QuotaProvider,
    isConnected: Boolean,
    onAddClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val statusColor by animateColorAsState(
        targetValue = if (isConnected) colorScheme.secondaryContainer else colorScheme.surface,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "providerStatusColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = statusColor,
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(
                painter = painterResource(provider.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .padding(10.dp),
                tint = Color.Unspecified
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = provider.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = provider.detailDescription,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colorScheme.onSurfaceVariant
                )
            )
        }
        TextButton(onClick = onAddClick) {
            Text(
                text = if (isConnected) "Add another" else "Connect",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    providers: List<QuotaProvider>,
    onProviderClick: (QuotaProvider) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Connect provider",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Choose a source to validate credentials and add it into the subscription queue.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            providers.forEach { provider ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProviderClick(provider) },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(
                                painter = painterResource(provider.iconRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(46.dp)
                                    .padding(10.dp),
                                tint = Color.Unspecified
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = provider.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = provider.connectDescription,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderApiKeyDialog(
    provider: QuotaProvider,
    customTitleInput: String,
    credentialInput: String,
    isSaving: Boolean,
    errorMessage: String?,
    onCustomTitleChange: (String) -> Unit,
    onCredentialChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Connect ${provider.title}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Name the subscription if needed, then provide your ${provider.credentialLabel.lowercase()} to validate and cache the first quota snapshot.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                OutlinedTextField(
                    value = customTitleInput,
                    onValueChange = onCustomTitleChange,
                    label = { Text("Subscription name") },
                    singleLine = true,
                    placeholder = { Text("e.g., My MiniMax workspace") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = credentialInput,
                    onValueChange = onCredentialChange,
                    label = { Text(provider.credentialLabel) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = credentialInput.isNotBlank() && !isSaving
            ) {
                Text(if (isSaving) "Connecting..." else "Connect")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}
