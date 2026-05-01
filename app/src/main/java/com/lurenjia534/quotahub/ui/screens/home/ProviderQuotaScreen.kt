package com.lurenjia534.quotahub.ui.screens.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.preferences.RefreshCadence
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.SubscriptionGateway
import com.lurenjia534.quotahub.sync.SubscriptionRefreshPolicy
import com.lurenjia534.quotahub.ui.components.MetricEmphasisLevel
import com.lurenjia534.quotahub.ui.components.QuotaMetricText
import com.lurenjia534.quotahub.ui.components.QuotaLoadingIndicator
import com.lurenjia534.quotahub.ui.components.rememberQuotaHaptics
import com.lurenjia534.quotahub.ui.provider.ProviderUiRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun ProviderQuotaScreen(
    modifier: Modifier = Modifier,
    subscriptionGateway: SubscriptionGateway,
    detailProjectorRegistry: ProviderQuotaDetailProjectorRegistry,
    providerUiRegistry: ProviderUiRegistry,
    refreshPolicy: SubscriptionRefreshPolicy,
    refreshCadence: RefreshCadence,
    highEmphasisMetrics: Boolean,
    hapticConfirmation: Boolean,
    onBackClick: () -> Unit
) {
    val viewModel: ProviderQuotaViewModel = viewModel(
        key = "provider-quota-${subscriptionGateway.subscription.id}-${refreshCadence.name}",
        factory = ProviderQuotaViewModel.Factory(
            subscriptionGateway = subscriptionGateway,
            detailProjectorRegistry = detailProjectorRegistry,
            refreshPolicy = refreshPolicy,
            refreshCadence = refreshCadence
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val supportedProvider = uiState.subscription.supportedProvider
    val providerUi = providerUiRegistry.getOrFallback(uiState.subscription.provider.id)
    val providerDisplayName = uiState.subscription.provider.displayName
    val pullToRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findActivity()
    val widthSizeClass = activity?.let { calculateWindowSizeClass(it).widthSizeClass }
        ?: WindowWidthSizeClass.Compact
    val useSupportingPane = widthSizeClass != WindowWidthSizeClass.Compact
    val isRefreshing = uiState.isLoading && uiState.detail.hasData
    val needsCredentialRepair = uiState.canUpdateCredentials &&
        uiState.subscription.syncStatus.state == SyncState.AuthFailed
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val quotaHaptics = rememberQuotaHaptics(hapticConfirmation)
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var refreshTriggeredByUser by remember { mutableStateOf(false) }
    var thresholdActivated by remember { mutableStateOf(false) }
    var wasRefreshing by remember { mutableStateOf(false) }

    var summaryVisible by remember { mutableStateOf(false) }
    var statusVisible by remember { mutableStateOf(false) }
    var modelsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        summaryVisible = true
        delay(70)
        statusVisible = true
        delay(70)
        modelsVisible = true
    }

    LaunchedEffect(pullToRefreshState.distanceFraction, isRefreshing) {
        val pastThreshold = pullToRefreshState.distanceFraction >= 1f
        when {
            isRefreshing -> thresholdActivated = true
            pastThreshold && !thresholdActivated -> {
                thresholdActivated = true
                quotaHaptics.refreshThreshold()
            }
            !pastThreshold -> thresholdActivated = false
        }
    }

    LaunchedEffect(isRefreshing, uiState.error, refreshTriggeredByUser) {
        if (wasRefreshing && !isRefreshing && refreshTriggeredByUser) {
            quotaHaptics.refreshResult(success = uiState.error == null)
            refreshTriggeredByUser = false
        }
        wasRefreshing = isRefreshing
    }

    val requestRefresh = {
        refreshTriggeredByUser = true
        viewModel.refresh()
    }
    val pageStatus = remember(
        uiState.error,
        uiState.isBootstrapping,
        uiState.isLoading,
        uiState.detail.hasData,
        uiState.canRefresh,
        needsCredentialRepair
    ) {
        resolveDetailPageStatus(
            error = uiState.error,
            isBootstrapping = uiState.isBootstrapping,
            isLoading = uiState.isLoading,
            hasData = uiState.detail.hasData,
            canRefresh = uiState.canRefresh,
            needsCredentialRepair = needsCredentialRepair
        )
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = {
                Text(
                    text = "Disconnect subscription",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Text(
                    text = "This removes the subscription and clears cached quota data for this provider.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.disconnect()
                            showDisconnectDialog = false
                            onBackClick()
                        }
                    }
                ) {
                    Text(
                        text = "Disconnect",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showRenameDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isSavingTitle) {
                    viewModel.hideRenameDialog()
                }
            },
            title = {
                Text(
                    text = "Rename subscription",
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
                        text = "Update how this source appears throughout QuotaHub. Leave it blank to fall back to the default label.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    OutlinedTextField(
                        value = uiState.titleInput,
                        onValueChange = viewModel::updateTitleInput,
                        label = { Text("Subscription name") },
                        singleLine = true,
                        placeholder = { Text("e.g., Team subscription") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::renameSubscription,
                    enabled = !uiState.isSavingTitle
                ) {
                    Text(if (uiState.isSavingTitle) "Saving..." else "Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::hideRenameDialog,
                    enabled = !uiState.isSavingTitle
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showCredentialDialog && supportedProvider != null && uiState.canUpdateCredentials) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isSavingCredentials) {
                    viewModel.hideCredentialDialog()
                }
            },
            title = {
                Text(
                    text = "Update credentials",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter a fresh set of ${supportedProvider.displayName} credentials. QuotaHub will validate them before replacing the stored secret.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    supportedProvider.credentialFields.forEachIndexed { index, field ->
                        ProviderCredentialInputField(
                            field = field,
                            value = uiState.credentialInputs[field.key].orEmpty(),
                            isLastField = index == supportedProvider.credentialFields.lastIndex,
                            onValueChange = { viewModel.updateCredentialInput(field.key, it) },
                            onSubmit = viewModel::saveCredentials
                        )
                    }

                    if (uiState.credentialError != null) {
                        Text(
                            text = uiState.credentialError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::saveCredentials,
                    enabled = supportedProvider.credentialFields.all {
                        !it.isRequired || !uiState.credentialInputs[it.key].isNullOrBlank()
                    } && !uiState.isSavingCredentials
                ) {
                    Text(if (uiState.isSavingCredentials) "Saving..." else "Update")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::hideCredentialDialog,
                    enabled = !uiState.isSavingCredentials
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.subscription.displayTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(providerUi.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$providerDisplayName • ${providerUi.subtitle}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = colorScheme.surface.copy(alpha = 0.96f)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.surfaceContainerLowest,
                            colorScheme.surfaceContainerLow.copy(alpha = 0.72f),
                            colorScheme.surfaceContainerLowest
                        )
                    )
                )
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = requestRefresh,
                state = pullToRefreshState,
                enabled = uiState.canRefresh && uiState.isConnected && !uiState.isBootstrapping,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = isRefreshing,
                        state = pullToRefreshState
                    )
                }
            ) {
                DetailResponsiveContent(
                    useSupportingPane = useSupportingPane,
                    providerIconRes = providerUi.iconRes,
                    providerName = providerDisplayName,
                    providerSubtitle = providerUi.subtitle,
                    subscriptionTitle = uiState.subscription.displayTitle,
                    detail = uiState.detail,
                    pageStatus = pageStatus,
                    canRefresh = uiState.canRefresh && uiState.isConnected && !uiState.isBootstrapping,
                    canRename = uiState.canRename,
                    needsCredentialRepair = needsCredentialRepair,
                    isBootstrapping = uiState.isBootstrapping,
                    isRefreshing = isRefreshing,
                    highEmphasisMetrics = highEmphasisMetrics,
                    summaryVisible = summaryVisible,
                    statusVisible = statusVisible,
                    modelsVisible = modelsVisible,
                    onRefresh = requestRefresh,
                    onRepairCredentials = viewModel::showCredentialDialog,
                    onRename = viewModel::showRenameDialog,
                    onDisconnect = { showDisconnectDialog = true }
                )
            }
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
private fun DetailResponsiveContent(
    useSupportingPane: Boolean,
    providerIconRes: Int,
    providerName: String,
    providerSubtitle: String,
    subscriptionTitle: String,
    detail: ProviderQuotaDetailUiModel,
    pageStatus: DetailPageStatus?,
    canRefresh: Boolean,
    canRename: Boolean,
    needsCredentialRepair: Boolean,
    isBootstrapping: Boolean,
    isRefreshing: Boolean,
    highEmphasisMetrics: Boolean,
    summaryVisible: Boolean,
    statusVisible: Boolean,
    modelsVisible: Boolean,
    onRefresh: () -> Unit,
    onRepairCredentials: () -> Unit,
    onRename: () -> Unit,
    onDisconnect: () -> Unit
) {
    if (useSupportingPane) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DetailStatusPane(
                        providerIconRes = providerIconRes,
                        providerName = providerName,
                        providerSubtitle = providerSubtitle,
                        subscriptionTitle = subscriptionTitle,
                        detail = detail,
                        pageStatus = pageStatus,
                        canRefresh = canRefresh,
                        canRename = canRename,
                        needsCredentialRepair = needsCredentialRepair,
                        isBootstrapping = isBootstrapping,
                        isRefreshing = isRefreshing,
                        highEmphasisMetrics = highEmphasisMetrics,
                        summaryVisible = summaryVisible,
                        statusVisible = statusVisible,
                        onRefresh = onRefresh,
                        onRepairCredentials = onRepairCredentials,
                        onRename = onRename,
                        onDisconnect = onDisconnect
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                modelDetailItems(
                    detail = detail,
                    pageStatus = pageStatus,
                    providerIconRes = providerIconRes,
                    highEmphasisMetrics = highEmphasisMetrics,
                    modelsVisible = modelsVisible
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DetailStatusPane(
                    providerIconRes = providerIconRes,
                    providerName = providerName,
                    providerSubtitle = providerSubtitle,
                    subscriptionTitle = subscriptionTitle,
                    detail = detail,
                    pageStatus = pageStatus,
                    canRefresh = canRefresh,
                    canRename = canRename,
                    needsCredentialRepair = needsCredentialRepair,
                    isBootstrapping = isBootstrapping,
                    isRefreshing = isRefreshing,
                    highEmphasisMetrics = highEmphasisMetrics,
                    summaryVisible = summaryVisible,
                    statusVisible = statusVisible,
                    onRefresh = onRefresh,
                    onRepairCredentials = onRepairCredentials,
                    onRename = onRename,
                    onDisconnect = onDisconnect
                )
            }
            modelDetailItems(
                detail = detail,
                pageStatus = pageStatus,
                providerIconRes = providerIconRes,
                highEmphasisMetrics = highEmphasisMetrics,
                modelsVisible = modelsVisible
            )
        }
    }
}

@Composable
private fun DetailStatusPane(
    providerIconRes: Int,
    providerName: String,
    providerSubtitle: String,
    subscriptionTitle: String,
    detail: ProviderQuotaDetailUiModel,
    pageStatus: DetailPageStatus?,
    canRefresh: Boolean,
    canRename: Boolean,
    needsCredentialRepair: Boolean,
    isBootstrapping: Boolean,
    isRefreshing: Boolean,
    highEmphasisMetrics: Boolean,
    summaryVisible: Boolean,
    statusVisible: Boolean,
    onRefresh: () -> Unit,
    onRepairCredentials: () -> Unit,
    onRename: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AnimatedSection(visible = summaryVisible) {
            DetailStatusHeader(
                providerIconRes = providerIconRes,
                providerName = providerName,
                providerSubtitle = providerSubtitle,
                subscriptionTitle = subscriptionTitle,
                summary = detail.summary,
                highEmphasisMetrics = highEmphasisMetrics,
                isBootstrapping = isBootstrapping,
                isRefreshing = isRefreshing
            )
        }
        AnimatedSection(visible = statusVisible) {
            DetailActionRow(
                canRefresh = canRefresh,
                isRefreshing = isRefreshing,
                needsCredentialRepair = needsCredentialRepair,
                canRename = canRename,
                onRefresh = onRefresh,
                onRepairCredentials = onRepairCredentials,
                onRename = onRename
            )
        }
        pageStatus?.let { status ->
            AnimatedSection(visible = statusVisible) {
                DetailStatusBanner(
                    status = status,
                    onRepairCredentials = onRepairCredentials,
                    onRefresh = onRefresh
                )
            }
        }
        AnimatedSection(visible = statusVisible) {
            DetailManagementRow(onDisconnect = onDisconnect)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.modelDetailItems(
    detail: ProviderQuotaDetailUiModel,
    pageStatus: DetailPageStatus?,
    providerIconRes: Int,
    highEmphasisMetrics: Boolean,
    modelsVisible: Boolean
) {
    when {
        detail.hasData -> {
            item {
                AnimatedSection(visible = modelsVisible) {
                    SectionHeader(
                        title = detail.sectionTitle,
                        subtitle = detail.sectionSubtitle
                    )
                }
            }
            item {
                AnimatedSection(visible = modelsVisible) {
                    ModelQuotaSection(
                        resources = detail.resources,
                        highEmphasisMetrics = highEmphasisMetrics
                    )
                }
            }
        }
        pageStatus == null -> {
            item {
                AnimatedSection(visible = modelsVisible) {
                    DetailEmptyState(providerIconRes = providerIconRes)
                }
            }
        }
    }
}

private enum class DetailPageStatusType {
    Credentials,
    Error,
    ReadOnly,
    Loading,
    Empty
}

private data class DetailPageStatus(
    val type: DetailPageStatusType,
    val title: String,
    val message: String,
    val actionLabel: String? = null
)

private fun resolveDetailPageStatus(
    error: String?,
    isBootstrapping: Boolean,
    isLoading: Boolean,
    hasData: Boolean,
    canRefresh: Boolean,
    needsCredentialRepair: Boolean
): DetailPageStatus? {
    return when {
        needsCredentialRepair -> DetailPageStatus(
            type = DetailPageStatusType.Credentials,
            title = "Credentials need attention",
            message = error ?: "Update credentials before this subscription can refresh again.",
            actionLabel = "Update credentials"
        )
        error != null && !canRefresh -> DetailPageStatus(
            type = DetailPageStatusType.ReadOnly,
            title = "Read-only snapshot",
            message = error
        )
        error != null -> DetailPageStatus(
            type = DetailPageStatusType.Error,
            title = "Refresh failed",
            message = error,
            actionLabel = "Retry"
        )
        isBootstrapping || (isLoading && !hasData) -> DetailPageStatus(
            type = DetailPageStatusType.Loading,
            title = "Preparing model quota detail",
            message = "QuotaHub is reading model-level counters and reset windows for this provider."
        )
        !hasData -> DetailPageStatus(
            type = DetailPageStatusType.Empty,
            title = "No quota data yet",
            message = "Pull down to request the first provider snapshot and populate model-level usage data."
        )
        else -> null
    }
}

@Composable
private fun DetailStatusHeader(
    providerIconRes: Int,
    providerName: String,
    providerSubtitle: String,
    subscriptionTitle: String,
    summary: ProviderQuotaSummaryUiModel?,
    highEmphasisMetrics: Boolean,
    isBootstrapping: Boolean,
    isRefreshing: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor by animateColorAsState(
        targetValue = summary?.risk?.let { riskColor(it) } ?: colorScheme.primary,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "detailStatusAccent"
    )
    val headerShape = expressiveContainerShape(2)

    Surface(
        color = colorScheme.surfaceContainerLow,
        shape = headerShape,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 18.dp, end = 18.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(142.dp)
                    .background(
                        color = accentColor,
                        shape = RoundedCornerShape(99.dp)
                    )
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = expressiveContainerShape(1),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(providerIconRes),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = providerName,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = subscriptionTitle,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = providerSubtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    summary?.let { RiskPill(risk = it.risk) }
                }

                if (summary != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuotaMetricText(
                            text = summary.headlineValue,
                            emphasized = highEmphasisMetrics,
                            level = MetricEmphasisLevel.Hero
                        )
                        Text(
                            text = summary.headlineLabel,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = colorScheme.onSurface
                            )
                        )
                        Text(
                            text = summary.stateDescription,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colorScheme.onSurfaceVariant
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    KeyMetricsStrip(
                        summary = summary,
                        accentColor = accentColor,
                        highEmphasisMetrics = highEmphasisMetrics
                    )
                } else {
                    EmptySnapshotText()
                }

                if (isBootstrapping || isRefreshing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuotaLoadingIndicator(modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isBootstrapping) {
                                "Preparing provider detail"
                            } else {
                                "Refreshing provider snapshot"
                            },
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
private fun EmptySnapshotText() {
    Text(
        text = "Waiting for the first readable quota snapshot from this provider.",
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun KeyMetricsStrip(
    summary: ProviderQuotaSummaryUiModel,
    accentColor: Color,
    highEmphasisMetrics: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KeyMetricRow(
            metrics = listOf(
                summary.primaryMetrics.first,
                summary.primaryMetrics.second,
                summary.primaryMetrics.third
            ),
            accentColor = accentColor,
            highEmphasisMetrics = highEmphasisMetrics
        )
        Text(
            text = "Sync ${summary.syncLabel}: ${summary.syncDescription}",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun KeyMetricRow(
    metrics: List<LabeledValueUiModel>,
    accentColor: Color,
    highEmphasisMetrics: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = accentColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        metrics.forEachIndexed { index, metric ->
            KeyMetricItem(
                modifier = Modifier.weight(1f),
                metric = metric,
                highEmphasisMetrics = highEmphasisMetrics
            )
            if (index < metrics.lastIndex) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .width(1.dp)
                        .height(42.dp)
                        .background(
                            color = colorScheme.outlineVariant.copy(alpha = 0.38f),
                            shape = RoundedCornerShape(99.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun KeyMetricItem(
    metric: LabeledValueUiModel,
    highEmphasisMetrics: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = metric.label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        QuotaMetricText(
            text = metric.value,
            emphasized = highEmphasisMetrics,
            level = MetricEmphasisLevel.Compact
        )
    }
}

@Composable
private fun DetailActionRow(
    canRefresh: Boolean,
    isRefreshing: Boolean,
    needsCredentialRepair: Boolean,
    canRename: Boolean,
    onRefresh: () -> Unit,
    onRepairCredentials: () -> Unit,
    onRename: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onRefresh,
            enabled = canRefresh && !isRefreshing,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(99.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRefreshing) "Refreshing" else "Refresh",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (needsCredentialRepair) {
            FilledTonalIconButton(onClick = onRepairCredentials) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Update credentials",
                    tint = colorScheme.error
                )
            }
        }
        if (canRename) {
            IconButton(onClick = onRename) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit name"
                )
            }
        }
    }
}

@Composable
private fun DetailStatusBanner(
    status: DetailPageStatus,
    onRepairCredentials: () -> Unit,
    onRefresh: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isErrorStatus = status.type == DetailPageStatusType.Credentials ||
        status.type == DetailPageStatusType.Error
    val containerColor = if (isErrorStatus) {
        colorScheme.errorContainer
    } else {
        colorScheme.surfaceContainerLow
    }
    val contentColor = if (isErrorStatus) {
        colorScheme.onErrorContainer
    } else {
        colorScheme.onSurface
    }
    val action = when (status.type) {
        DetailPageStatusType.Credentials -> onRepairCredentials
        DetailPageStatusType.Error -> onRefresh
        else -> null
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(26.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = if (isErrorStatus) contentColor else colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = status.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                )
                Text(
                    text = status.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isErrorStatus) {
                            contentColor
                        } else {
                            colorScheme.onSurfaceVariant
                        }
                    )
                )
            }
            if (status.actionLabel != null && action != null) {
                TextButton(onClick = action) {
                    Text(
                        text = status.actionLabel,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailManagementRow(onDisconnect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDisconnect) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Disconnect",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ProviderCredentialInputField(
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
            VisualTransformation.None
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

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    val colorScheme = MaterialTheme.colorScheme
    val headerContent = remember(title, subtitle) {
        resolveSectionHeaderContent(title = title, subtitle = subtitle)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(76.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.primary,
                            colorScheme.tertiary
                        )
                    ),
                    shape = RoundedCornerShape(99.dp)
                )
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            headerContent.eyebrow?.let { eyebrow ->
                Surface(
                    color = colorScheme.primaryContainer,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 12.dp
                    )
                ) {
                    Text(
                        text = eyebrow,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = headerContent.body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private data class SectionHeaderContent(
    val eyebrow: String?,
    val body: String
)

private fun resolveSectionHeaderContent(
    title: String,
    subtitle: String
): SectionHeaderContent {
    val trimmedSubtitle = subtitle.trim()
    val planPrefix = "Plan:"
    if (trimmedSubtitle.startsWith(planPrefix, ignoreCase = true)) {
        val sentenceBreakIndex = trimmedSubtitle.indexOf(". ")
        if (sentenceBreakIndex > 0) {
            return SectionHeaderContent(
                eyebrow = trimmedSubtitle
                    .substring(0, sentenceBreakIndex)
                    .trim()
                    .removeSuffix("."),
                body = trimmedSubtitle.substring(sentenceBreakIndex + 2).trim()
            )
        }
    }

    val fallbackEyebrow = when {
        title.contains("quota", ignoreCase = true) -> "Provider detail"
        else -> null
    }

    return SectionHeaderContent(
        eyebrow = fallbackEyebrow,
        body = trimmedSubtitle
    )
}

@Composable
private fun ModelQuotaSection(
    resources: List<ProviderQuotaResourceUiModel>,
    highEmphasisMetrics: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        resources.forEachIndexed { index, resource ->
            ModelQuotaRow(
                resource = resource,
                index = index,
                highEmphasisMetrics = highEmphasisMetrics
            )
        }
    }
}

@Composable
private fun ModelQuotaRow(
    resource: ProviderQuotaResourceUiModel,
    index: Int,
    highEmphasisMetrics: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by rememberSaveable(resource.key) { mutableStateOf(false) }
    val progressColor by animateColorAsState(
        targetValue = riskColor(resource.risk),
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "modelProgressColor"
    )
    val rowShape = expressiveContainerShape(index + 6)
    val hasExtraDetails = resource.secondaryMetrics.isNotEmpty()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(stiffness = 420f, dampingRatio = 0.88f)
            )
            .clip(rowShape)
            .clickable(
                enabled = hasExtraDetails,
                onClick = { expanded = !expanded }
            ),
        color = colorScheme.surfaceContainerLow,
        shape = rowShape,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = progressColor.copy(alpha = 0.16f),
                    shape = expressiveContainerShape(index + 3),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (resource.risk == QuotaRisk.Healthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = progressColor
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = resource.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        RiskPill(risk = resource.risk)
                    }
                    QuotaMetricText(
                        text = resource.resetLabel,
                        emphasized = highEmphasisMetrics,
                        level = MetricEmphasisLevel.Compact,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                if (hasExtraDetails) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse details" else "Expand details",
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }

            QuotaProgressBar(progress = resource.progress, color = progressColor)
            DetailMetricRow(
                metrics = resource.primaryMetrics,
                risk = resource.risk,
                highEmphasisMetrics = highEmphasisMetrics
            )

            AnimatedVisibility(visible = expanded && hasExtraDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colorScheme.outlineVariant.copy(alpha = 0.24f))
                    )
                    DetailMetricRow(
                        metrics = resource.secondaryMetrics,
                        risk = resource.risk,
                        highEmphasisMetrics = highEmphasisMetrics
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailMetricRow(
    metrics: List<LabeledValueUiModel>,
    risk: QuotaRisk,
    highEmphasisMetrics: Boolean,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty()) {
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        metrics.forEach { metric ->
            DetailMetric(
                modifier = Modifier.weight(1f),
                metric = metric,
                risk = risk,
                highEmphasisMetrics = highEmphasisMetrics
            )
        }
    }
}

private fun expressiveContainerShape(index: Int): RoundedCornerShape {
    return when (index % 4) {
        0 -> RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 32.dp
        )
        1 -> RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 32.dp,
            bottomStart = 28.dp,
            bottomEnd = 18.dp
        )
        2 -> RoundedCornerShape(
            topStart = 32.dp,
            topEnd = 24.dp,
            bottomStart = 18.dp,
            bottomEnd = 28.dp
        )
        else -> RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 28.dp,
            bottomStart = 34.dp,
            bottomEnd = 20.dp
        )
    }
}

@Composable
private fun QuotaProgressBar(
    progress: Float,
    color: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.86f),
        label = "quotaProgress"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(
            topStart = 99.dp,
            topEnd = 42.dp,
            bottomStart = 99.dp,
            bottomEnd = 42.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(12.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(
                            topStart = 99.dp,
                            topEnd = 42.dp,
                            bottomStart = 99.dp,
                            bottomEnd = 42.dp
                        )
                    )
            )
        }
    }
}

@Composable
private fun DetailMetric(
    metric: LabeledValueUiModel,
    risk: QuotaRisk,
    highEmphasisMetrics: Boolean,
    modifier: Modifier = Modifier
) {
    val resolvedValueColor = if (metric.highlightRisk) {
        riskColor(risk)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = metric.label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        QuotaMetricText(
            text = metric.value,
            emphasized = highEmphasisMetrics,
            level = MetricEmphasisLevel.Compact,
            color = resolvedValueColor
        )
    }
}

@Composable
private fun DetailEmptyState(providerIconRes: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(
            topStart = 34.dp,
            topEnd = 22.dp,
            bottomStart = 22.dp,
            bottomEnd = 40.dp
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 26.dp
                ),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(providerIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color.Unspecified
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "No quota data yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Pull down to request the first provider snapshot and populate model-level usage data.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun RiskPill(risk: QuotaRisk) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = when (risk) {
        QuotaRisk.Critical -> colorScheme.errorContainer
        QuotaRisk.Watch -> colorScheme.tertiaryContainer
        QuotaRisk.Healthy -> colorScheme.secondaryContainer
    }
    val contentColor = when (risk) {
        QuotaRisk.Critical -> colorScheme.onErrorContainer
        QuotaRisk.Watch -> colorScheme.onTertiaryContainer
        QuotaRisk.Healthy -> colorScheme.onSecondaryContainer
    }
    val label = when (risk) {
        QuotaRisk.Critical -> "Critical"
        QuotaRisk.Watch -> "Watch"
        QuotaRisk.Healthy -> "Healthy"
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun riskColor(risk: QuotaRisk): Color {
    return when (risk) {
        QuotaRisk.Critical -> MaterialTheme.colorScheme.error
        QuotaRisk.Watch -> MaterialTheme.colorScheme.tertiary
        QuotaRisk.Healthy -> MaterialTheme.colorScheme.primary
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
