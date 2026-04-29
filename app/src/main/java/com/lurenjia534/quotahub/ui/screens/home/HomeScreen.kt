package com.lurenjia534.quotahub.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.QuotaProgressMetric
import com.lurenjia534.quotahub.data.provider.SubscriptionRegistry
import com.lurenjia534.quotahub.ui.components.MetricEmphasisLevel
import com.lurenjia534.quotahub.ui.components.QuotaMetricText
import com.lurenjia534.quotahub.ui.components.QuotaLoadingIndicator
import com.lurenjia534.quotahub.ui.provider.ProviderUiRegistry
import kotlinx.coroutines.delay
import java.text.NumberFormat
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    subscriptionRegistry: SubscriptionRegistry,
    providerUiRegistry: ProviderUiRegistry,
    highEmphasisMetrics: Boolean,
    hideLandscapeMonitorHud: Boolean,
    bottomContentPadding: Dp = 0.dp,
    addSubscriptionRequestKey: Int = 0,
    onSubscriptionClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val viewModel: HomeHubViewModel = viewModel(
        factory = HomeHubViewModel.Factory(
            subscriptionRegistry = subscriptionRegistry,
            providerUiRegistry = providerUiRegistry
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var lastHandledAddSubscriptionRequest by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState()

    val subscriptionCards = uiState.subscriptionCards
    val connectedCount = subscriptionCards.count { it.isConnected }
    val providerCount = subscriptionRegistry.providers.size
    val trackedResources = subscriptionCards.sumOf { it.resourceCount }
    val nextRefreshWindow = subscriptionCards.mapNotNull { it.nextResetAt }.minOrNull()
    val dominantRisk = subscriptionCards.dominantRisk()
    val dominantSyncState = subscriptionCards.dominantSyncState()
    val hasSubscriptions = subscriptionCards.isNotEmpty()
    val connectedProviderIds = subscriptionCards
        .map { it.providerId }
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

    LaunchedEffect(addSubscriptionRequestKey) {
        if (addSubscriptionRequestKey > 0 &&
            addSubscriptionRequestKey != lastHandledAddSubscriptionRequest
        ) {
            showBottomSheet = true
            lastHandledAddSubscriptionRequest = addSubscriptionRequestKey
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.surfaceContainerLowest,
                        colorScheme.primaryContainer.copy(alpha = 0.12f),
                        colorScheme.surfaceContainerLowest
                    )
                )
            )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscapeOverview = maxWidth > maxHeight && maxWidth >= 640.dp

            if (isLandscapeOverview) {
                LandscapeHomeContent(
                    subscriptionCards = subscriptionCards,
                    connectedCount = connectedCount,
                    providerCount = providerCount,
                    trackedResources = trackedResources,
                    nextRefreshWindow = nextRefreshWindow,
                    highEmphasisMetrics = highEmphasisMetrics,
                    hideLandscapeMonitorHud = hideLandscapeMonitorHud,
                    dominantRisk = dominantRisk,
                    dominantSyncState = dominantSyncState,
                    isBootstrapping = uiState.isBootstrapping,
                    error = uiState.error,
                    showCredentialDialog = uiState.showCredentialDialog,
                    hasSubscriptions = hasSubscriptions,
                    bottomContentPadding = bottomContentPadding,
                    statusVisible = statusVisible,
                    queueVisible = queueVisible,
                    onDismissError = viewModel::clearError,
                    onSubscriptionClick = onSubscriptionClick,
                    onAddClick = { showBottomSheet = true },
                    onSettingsClick = onSettingsClick
                )
            } else {
                PortraitHomeContent(
                    subscriptionCards = subscriptionCards,
                    providers = subscriptionRegistry.providers,
                    providerUiRegistry = providerUiRegistry,
                    connectedProviderIds = connectedProviderIds,
                    connectedCount = connectedCount,
                    providerCount = providerCount,
                    trackedResources = trackedResources,
                    nextRefreshWindow = nextRefreshWindow,
                    highEmphasisMetrics = highEmphasisMetrics,
                    dominantRisk = dominantRisk,
                    dominantSyncState = dominantSyncState,
                    isBootstrapping = uiState.isBootstrapping,
                    error = uiState.error,
                    showCredentialDialog = uiState.showCredentialDialog,
                    hasSubscriptions = hasSubscriptions,
                    bottomContentPadding = bottomContentPadding,
                    boardVisible = boardVisible,
                    statusVisible = statusVisible,
                    queueVisible = queueVisible,
                    providerVisible = providerVisible,
                    onDismissError = viewModel::clearError,
                    onSubscriptionClick = onSubscriptionClick,
                    onAddClick = { showBottomSheet = true },
                    onProviderClick = { provider ->
                        viewModel.clearError()
                        viewModel.showCredentialDialog(provider)
                    }
                )
            }
        }

        if (showBottomSheet) {
            ProviderBottomSheet(
                sheetState = sheetState,
                onDismiss = { showBottomSheet = false },
                providers = subscriptionRegistry.providers,
                providerUiRegistry = providerUiRegistry,
                onProviderClick = { provider ->
                    viewModel.clearError()
                    viewModel.showCredentialDialog(provider)
                    showBottomSheet = false
                }
            )
        }

        if (uiState.showCredentialDialog) {
            ProviderCredentialDialog(
                provider = uiState.selectedProvider ?: subscriptionRegistry.providers.first(),
                customTitleInput = uiState.customTitleInput,
                credentialInputs = uiState.credentialInputs,
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
private fun PortraitHomeContent(
    subscriptionCards: List<SubscriptionCardUiModel>,
    providers: List<ProviderDescriptor>,
    providerUiRegistry: ProviderUiRegistry,
    connectedProviderIds: Set<String>,
    connectedCount: Int,
    providerCount: Int,
    trackedResources: Int,
    nextRefreshWindow: Long?,
    highEmphasisMetrics: Boolean,
    dominantRisk: QuotaRisk,
    dominantSyncState: SyncState,
    isBootstrapping: Boolean,
    error: String?,
    showCredentialDialog: Boolean,
    hasSubscriptions: Boolean,
    bottomContentPadding: Dp,
    boardVisible: Boolean,
    statusVisible: Boolean,
    queueVisible: Boolean,
    providerVisible: Boolean,
    onDismissError: () -> Unit,
    onSubscriptionClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onProviderClick: (ProviderDescriptor) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 18.dp,
            end = 18.dp,
            bottom = 34.dp + bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            AnimatedSection(visible = boardVisible) {
                HomeRadarSurface(
                    connectedCount = connectedCount,
                    providerCount = providerCount,
                    trackedResources = trackedResources,
                    nextRefreshWindow = nextRefreshWindow,
                    highEmphasisMetrics = highEmphasisMetrics,
                    dominantRisk = dominantRisk,
                    dominantSyncState = dominantSyncState,
                    isBootstrapping = isBootstrapping
                )
            }
        }

        if (error != null && !showCredentialDialog) {
            item {
                AnimatedSection(visible = statusVisible) {
                    HomeErrorStrip(
                        message = error,
                        onDismiss = onDismissError
                    )
                }
            }
        }

        item {
            AnimatedSection(visible = statusVisible) {
                HomeLaneHeader(
                    title = if (hasSubscriptions) "Subscription queue" else "Subscriptions",
                    subtitle = if (hasSubscriptions) {
                        "Active sources ordered for fast scanning. Open any row for resource-level quota detail."
                    } else {
                        "No sources connected yet. Add a provider to start caching quota snapshots."
                    }
                )
            }
        }

        if (isBootstrapping) {
            item {
                AnimatedSection(visible = queueVisible) {
                    HomeLoadingRow()
                }
            }
        } else if (hasSubscriptions) {
            items(
                items = subscriptionCards,
                key = { it.subscriptionId }
            ) { subscriptionCard ->
                SubscriptionSignalLane(
                    subscriptionCard = subscriptionCard,
                    highEmphasisMetrics = highEmphasisMetrics,
                    onClick = { onSubscriptionClick(subscriptionCard.subscriptionId) }
                )
            }
        } else {
            item {
                AnimatedSection(visible = queueVisible) {
                    HomeEmptyState(onAddClick = onAddClick)
                }
            }
        }

        item {
            AnimatedSection(visible = providerVisible) {
                ProviderAccessSection(
                    providers = providers,
                    providerUiRegistry = providerUiRegistry,
                    subscriptionCards = subscriptionCards,
                    connectedProviderIds = connectedProviderIds,
                    onProviderClick = onProviderClick
                )
            }
        }
    }
}

@Composable
private fun LandscapeHomeContent(
    subscriptionCards: List<SubscriptionCardUiModel>,
    connectedCount: Int,
    providerCount: Int,
    trackedResources: Int,
    nextRefreshWindow: Long?,
    highEmphasisMetrics: Boolean,
    hideLandscapeMonitorHud: Boolean,
    dominantRisk: QuotaRisk,
    dominantSyncState: SyncState,
    isBootstrapping: Boolean,
    error: String?,
    showCredentialDialog: Boolean,
    hasSubscriptions: Boolean,
    bottomContentPadding: Dp,
    statusVisible: Boolean,
    queueVisible: Boolean,
    onDismissError: () -> Unit,
    onSubscriptionClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                start = 12.dp,
                top = 4.dp,
                end = 12.dp,
                bottom = 6.dp + bottomContentPadding
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (hideLandscapeMonitorHud) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LandscapeHudAction(
                    icon = Icons.Filled.Settings,
                    contentDescription = "Open settings",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onSettingsClick
                )
                Spacer(modifier = Modifier.width(8.dp))
                LandscapeHudAction(
                    icon = Icons.Filled.Add,
                    contentDescription = "Add provider",
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = onAddClick
                )
            }
        } else {
            LandscapeHubStatusBar(
                connectedCount = connectedCount,
                providerCount = providerCount,
                trackedResources = trackedResources,
                nextRefreshWindow = nextRefreshWindow,
                highEmphasisMetrics = highEmphasisMetrics,
                dominantRisk = dominantRisk,
                dominantSyncState = dominantSyncState,
                isBootstrapping = isBootstrapping,
                onAddClick = onAddClick,
                onSettingsClick = onSettingsClick
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
        }

        if (error != null && !showCredentialDialog) {
            AnimatedSection(visible = statusVisible) {
                HomeErrorStrip(
                    message = error,
                    onDismiss = onDismissError
                )
            }
        }

        if (queueVisible) {
            LandscapeProviderMonitorTable(
                subscriptionCards = subscriptionCards,
                highEmphasisMetrics = highEmphasisMetrics,
                isBootstrapping = isBootstrapping,
                hasSubscriptions = hasSubscriptions,
                onSubscriptionClick = onSubscriptionClick,
                onAddClick = onAddClick,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LandscapeHubStatusBar(
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
private fun LandscapeHudAction(
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
private fun LandscapeProviderMonitorTable(
    subscriptionCards: List<SubscriptionCardUiModel>,
    highEmphasisMetrics: Boolean,
    isBootstrapping: Boolean,
    hasSubscriptions: Boolean,
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
                    LandscapeProviderHeader()
                    subscriptionCards.forEachIndexed { index, subscriptionCard ->
                        HubProviderSignalRow(
                            subscriptionCard = subscriptionCard,
                            highEmphasisMetrics = highEmphasisMetrics,
                            onClick = { onSubscriptionClick(subscriptionCard.subscriptionId) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        if (index < subscriptionCards.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
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

private fun List<QuotaProgressMetric>.landscapeMetricOrder(): List<QuotaProgressMetric> {
    return sortedWith(
        compareBy<QuotaProgressMetric> {
            when {
                it.label.contains("5h", ignoreCase = true) -> 0
                it.label.contains("week", ignoreCase = true) -> 1
                it.label.contains("plan", ignoreCase = true) -> 2
                else -> 3
            }
        }.thenBy { it.label }
    )
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
private fun HomeRadarSurface(
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
private fun CompactHomeRadarSurface(
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

@Composable
private fun HomeLaneHeader(
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
private fun SignalGlyph(
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
private fun SignalChip(
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HomeLoadingRow() {
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
private fun HomeErrorStrip(
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
private fun SubscriptionSignalLane(
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
                SignalChip(
                    label = when (risk) {
                        QuotaRisk.Critical -> "Critical"
                        QuotaRisk.Watch -> "Watch"
                        QuotaRisk.Healthy -> "Healthy"
                    },
                    color = stateColor
                )
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

private fun List<SubscriptionCardUiModel>.dominantSyncState(): SyncState {
    return when {
        any { it.syncState == SyncState.AuthFailed } -> SyncState.AuthFailed
        any { it.syncState == SyncState.SyncError } -> SyncState.SyncError
        any { it.syncState == SyncState.Stale } -> SyncState.Stale
        any { it.syncState == SyncState.Syncing } -> SyncState.Syncing
        any { it.syncState == SyncState.NeverSynced } -> SyncState.NeverSynced
        else -> SyncState.Active
    }
}

@Composable
private fun HomeEmptyState(
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

@Composable
private fun ProviderAccessSection(
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

private fun List<SubscriptionCardUiModel>.providerStatusLabel(isConnected: Boolean): String {
    return when {
        size > 1 -> "$size linked"
        isConnected -> "Linked"
        else -> "Open"
    }
}

private fun List<SubscriptionCardUiModel>.providerAccessSummary(): String {
    if (isEmpty()) return "Ready to connect"

    val accountText = if (size == 1) "1 account" else "$size accounts"
    val resourceCount = sumOf { it.resourceCount }
    val resourceText = if (resourceCount == 1) "1 resource" else "$resourceCount resources"
    val syncText = dominantSyncState().providerAccessLabel()
    return "$accountText / $resourceText / $syncText"
}

private fun SyncState.providerAccessLabel(): String {
    return when (this) {
        SyncState.AuthFailed -> "auth attention"
        SyncState.SyncError -> "sync error"
        SyncState.Stale -> "stale"
        SyncState.Syncing -> "syncing"
        SyncState.NeverSynced -> "not synced"
        SyncState.Active -> "active"
    }
}

private fun String.percentValueOrNull(): Float? {
    return trim()
        .removeSuffix("%")
        .toFloatOrNull()
        ?.coerceIn(0f, 100f)
}

private fun formatLongCount(value: Long): String {
    return NumberFormat.getIntegerInstance().format(value)
}

@Composable
private fun syncStateColor(state: SyncState): Color {
    return when (state) {
        SyncState.AuthFailed -> MaterialTheme.colorScheme.error
        SyncState.SyncError -> MaterialTheme.colorScheme.error
        SyncState.Stale -> MaterialTheme.colorScheme.tertiary
        SyncState.Syncing -> MaterialTheme.colorScheme.primary
        SyncState.NeverSynced -> MaterialTheme.colorScheme.onSurfaceVariant
        SyncState.Active -> MaterialTheme.colorScheme.onSurface
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    providers: List<ProviderDescriptor>,
    providerUiRegistry: ProviderUiRegistry,
    onProviderClick: (ProviderDescriptor) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
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
                val providerUi = providerUiRegistry.require(provider)
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
                                painter = painterResource(providerUi.iconRes),
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
                                text = provider.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = providerUi.connectDescription,
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
private fun ProviderCredentialDialog(
    provider: ProviderDescriptor,
    customTitleInput: String,
    credentialInputs: Map<String, String>,
    isSaving: Boolean,
    errorMessage: String?,
    onCustomTitleChange: (String) -> Unit,
    onCredentialChange: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Connect ${provider.displayName}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Name the subscription if needed, then provide the credentials below to validate and cache the first quota snapshot.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                OutlinedTextField(
                    value = customTitleInput,
                    onValueChange = onCustomTitleChange,
                    label = { Text("Subscription name") },
                    singleLine = true,
                    placeholder = { Text("e.g., Personal workspace") },
                    modifier = Modifier.fillMaxWidth()
                )
                provider.credentialFields.forEachIndexed { index, field ->
                    CredentialInputField(
                        field = field,
                        value = credentialInputs[field.key].orEmpty(),
                        isLastField = index == provider.credentialFields.lastIndex,
                        onValueChange = { onCredentialChange(field.key, it) },
                        onSubmit = onConfirm
                    )
                }

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
                enabled = provider.credentialFields.all { field ->
                    !field.isRequired || !credentialInputs[field.key].isNullOrBlank()
                } && !isSaving
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

@Composable
private fun CredentialInputField(
    field: CredentialFieldSpec,
    value: String,
    isLastField: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(field.label) },
        singleLine = true,
        visualTransformation = if (field.isSecret) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            imeAction = if (isLastField) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onDone = { if (isLastField) onSubmit() }
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
