package com.lurenjia534.quotahub.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.SubscriptionRegistry
import com.lurenjia534.quotahub.ui.provider.ProviderUiRegistry
import com.lurenjia534.quotahub.ui.screens.home.overview.*
import kotlinx.coroutines.delay

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
            HomeAnimatedSection(visible = boardVisible) {
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
                HomeAnimatedSection(visible = statusVisible) {
                    HomeErrorStrip(
                        message = error,
                        onDismiss = onDismissError
                    )
                }
            }
        }

        item {
            HomeAnimatedSection(visible = statusVisible) {
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
                HomeAnimatedSection(visible = queueVisible) {
                    HomeLoadingRow()
                }
            }
        } else if (hasSubscriptions) {
            item {
                HomeAnimatedSection(visible = queueVisible) {
                    SubscriptionSourceSummary(subscriptionCards = subscriptionCards)
                }
            }
            subscriptionCards.sourceGroups().forEach { group ->
                item(key = "source-header-${group.key}") {
                    HomeAnimatedSection(visible = queueVisible) {
                        SubscriptionSourceHeader(group = group)
                    }
                }
                items(
                    items = group.cards,
                    key = { it.subscriptionId }
                ) { subscriptionCard ->
                    SubscriptionSignalLane(
                        subscriptionCard = subscriptionCard,
                        highEmphasisMetrics = highEmphasisMetrics,
                        onClick = { onSubscriptionClick(subscriptionCard.subscriptionId) }
                    )
                }
            }
        } else {
            item {
                HomeAnimatedSection(visible = queueVisible) {
                    HomeEmptyState(onAddClick = onAddClick)
                }
            }
        }

        item {
            HomeAnimatedSection(visible = providerVisible) {
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
    var hudActionsVisible by remember { mutableStateOf(hideLandscapeMonitorHud) }
    var hudActionsRevealKey by remember { mutableStateOf(0) }

    fun revealHudActions() {
        if (hideLandscapeMonitorHud) {
            hudActionsVisible = true
            hudActionsRevealKey += 1
        }
    }

    LaunchedEffect(hideLandscapeMonitorHud, hudActionsRevealKey) {
        if (hideLandscapeMonitorHud) {
            hudActionsVisible = true
            delay(3_200L)
            hudActionsVisible = false
        } else {
            hudActionsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                )
            )
            .padding(
                start = 12.dp,
                top = 4.dp,
                end = 12.dp,
                bottom = 6.dp + bottomContentPadding
            )
            .pointerInput(hideLandscapeMonitorHud) {
                if (!hideLandscapeMonitorHud) {
                    return@pointerInput
                }
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (up != null) {
                        revealHudActions()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!hideLandscapeMonitorHud) {
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
                HomeAnimatedSection(visible = statusVisible) {
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
                    showHeader = !hideLandscapeMonitorHud || hudActionsVisible,
                    onSubscriptionClick = onSubscriptionClick,
                    onAddClick = onAddClick,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        AnimatedVisibility(
            visible = hideLandscapeMonitorHud && hudActionsVisible,
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
            ),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LandscapeHudAction(
                    icon = Icons.Filled.Settings,
                    contentDescription = "Open settings",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onSettingsClick
                )
                LandscapeHudAction(
                    icon = Icons.Filled.Add,
                    contentDescription = "Add provider",
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = onAddClick
                )
            }
        }
    }
}
