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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProviderQuotaScreen(
    modifier: Modifier = Modifier,
    subscriptionGateway: SubscriptionGateway,
    detailProjectorRegistry: ProviderQuotaDetailProjectorRegistry,
    providerUiRegistry: ProviderUiRegistry,
    refreshPolicy: SubscriptionRefreshPolicy,
    highEmphasisMetrics: Boolean,
    hapticConfirmation: Boolean,
    onBackClick: () -> Unit
) {
    val viewModel: ProviderQuotaViewModel = viewModel(
        key = "provider-quota-${subscriptionGateway.subscription.id}",
        factory = ProviderQuotaViewModel.Factory(
            subscriptionGateway = subscriptionGateway,
            detailProjectorRegistry = detailProjectorRegistry,
            refreshPolicy = refreshPolicy
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val supportedProvider = uiState.subscription.supportedProvider
    val providerUi = providerUiRegistry.getOrFallback(uiState.subscription.provider.id)
    val providerDisplayName = uiState.subscription.provider.displayName
    val pullToRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AnimatedSection(visible = summaryVisible) {
                            DetailHeroPanel(
                                providerIconRes = providerUi.iconRes,
                                providerName = providerDisplayName,
                                providerSubtitle = providerUi.subtitle,
                                subscriptionTitle = uiState.subscription.displayTitle,
                                summary = uiState.detail.summary,
                                highEmphasisMetrics = highEmphasisMetrics,
                                isBootstrapping = uiState.isBootstrapping,
                                isRefreshing = isRefreshing
                            )
                        }
                    }

                    item {
                        AnimatedSection(visible = statusVisible) {
                            DetailActionDock(
                                canRefresh = uiState.canRefresh && uiState.isConnected && !uiState.isBootstrapping,
                                isRefreshing = isRefreshing,
                                needsCredentialRepair = needsCredentialRepair,
                                canRename = uiState.canRename,
                                onRefresh = requestRefresh,
                                onRepairCredentials = viewModel::showCredentialDialog,
                                onRename = viewModel::showRenameDialog,
                                onDisconnect = { showDisconnectDialog = true }
                            )
                        }
                    }

                    if (uiState.error != null) {
                        item {
                            val actionLabel: String? = when {
                                needsCredentialRepair -> "Update credentials"
                                uiState.canRefresh -> "Retry"
                                else -> null
                            }
                            val actionHandler: (() -> Unit)? = when {
                                needsCredentialRepair -> viewModel::showCredentialDialog
                                uiState.canRefresh -> requestRefresh
                                else -> null
                            }
                            AnimatedSection(visible = statusVisible) {
                                DetailErrorStrip(
                                    title = if (needsCredentialRepair) {
                                        "Credentials need attention"
                                    } else if (!uiState.canRefresh) {
                                        "Read-only snapshot"
                                    } else {
                                        "Refresh failed"
                                    },
                                    message = uiState.error!!,
                                    actionLabel = actionLabel,
                                    onAction = actionHandler
                                )
                            }
                        }
                    }

                    when {
                        uiState.isBootstrapping || (uiState.isLoading && !uiState.detail.hasData) -> {
                            item {
                                AnimatedSection(visible = statusVisible) {
                                    DetailLoadingRow()
                                }
                            }
                        }

                        uiState.detail.hasData -> {
                            item {
                                AnimatedSection(visible = modelsVisible) {
                                    SectionHeader(
                                        title = uiState.detail.sectionTitle,
                                        subtitle = uiState.detail.sectionSubtitle
                                    )
                                }
                            }
                            item {
                                AnimatedSection(visible = modelsVisible) {
                                    ModelQuotaSection(
                                        resources = uiState.detail.resources,
                                        highEmphasisMetrics = highEmphasisMetrics
                                    )
                                }
                            }
                        }

                        else -> {
                            item {
                                AnimatedSection(visible = modelsVisible) {
                                    DetailEmptyState(providerIconRes = providerUi.iconRes)
                                }
                            }
                        }
                    }
                }
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
private fun DetailHeroPanel(
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
        label = "detailHeroAccent"
    )
    val heroShape = RoundedCornerShape(
        topStart = 42.dp,
        topEnd = 24.dp,
        bottomStart = 28.dp,
        bottomEnd = 52.dp
    )

    Surface(
        color = colorScheme.surfaceContainerHigh,
        shape = heroShape,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.16f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.34f),
                            colorScheme.secondaryContainer.copy(alpha = 0.22f),
                            colorScheme.surfaceContainerHigh
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = colorScheme.surface.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(
                            topStart = 26.dp,
                            topEnd = 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 30.dp
                        ),
                        modifier = Modifier.size(62.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(providerIconRes),
                                contentDescription = null,
                                modifier = Modifier.size(34.dp),
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
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
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
                        Text(
                            text = "Sync ${summary.syncLabel}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
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
                            )
                        )
                        Text(
                            text = summary.syncDescription,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    SummaryMetricCloud(
                        summary = summary,
                        accentColor = accentColor,
                        highEmphasisMetrics = highEmphasisMetrics
                    )
                } else {
                    Text(
                        text = "Waiting for the first readable quota snapshot from this provider.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (isBootstrapping || isRefreshing) {
                    Surface(
                        color = colorScheme.surface.copy(alpha = 0.74f),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
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
}

@Composable
private fun SummaryMetricCloud(
    summary: ProviderQuotaSummaryUiModel,
    accentColor: Color,
    highEmphasisMetrics: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryMetricCloudRow(
            row = summary.primaryMetrics,
            accentColor = accentColor,
            rowIndex = 0,
            highEmphasisMetrics = highEmphasisMetrics
        )
        summary.secondaryMetrics?.let { secondaryMetrics ->
            SummaryMetricCloudRow(
                row = secondaryMetrics,
                accentColor = accentColor,
                rowIndex = 1,
                highEmphasisMetrics = highEmphasisMetrics
            )
        }
    }
}

@Composable
private fun SummaryMetricCloudRow(
    row: SummaryMetricRowUiModel,
    accentColor: Color,
    rowIndex: Int,
    highEmphasisMetrics: Boolean
) {
    val metrics = listOf(row.first, row.second, row.third)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        metrics.forEachIndexed { index, metric ->
            SummaryMetricCapsule(
                modifier = Modifier.weight(1f),
                metric = metric,
                accentColor = accentColor,
                shapeIndex = rowIndex * 3 + index,
                highEmphasisMetrics = highEmphasisMetrics
            )
        }
    }
}

@Composable
private fun SummaryMetricCapsule(
    metric: LabeledValueUiModel,
    accentColor: Color,
    shapeIndex: Int,
    highEmphasisMetrics: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = expressiveContainerShape(shapeIndex)

    Surface(
        modifier = modifier,
        color = if (shapeIndex % 2 == 0) {
            colorScheme.surface.copy(alpha = 0.78f)
        } else {
            accentColor.copy(alpha = 0.13f)
        },
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = colorScheme.onSurfaceVariant
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
}

@Composable
private fun DetailActionDock(
    canRefresh: Boolean,
    isRefreshing: Boolean,
    needsCredentialRepair: Boolean,
    canRename: Boolean,
    onRefresh: () -> Unit,
    onRepairCredentials: () -> Unit,
    onRename: () -> Unit,
    onDisconnect: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        color = colorScheme.surfaceContainerHigh,
        contentColor = colorScheme.onSurface,
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 36.dp,
            bottomStart = 36.dp,
            bottomEnd = 24.dp
        ),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DetailDockPrimaryAction(
                label = if (isRefreshing) "Refreshing" else "Refresh",
                enabled = canRefresh && !isRefreshing,
                onClick = onRefresh,
                modifier = Modifier.weight(1f)
            )
            if (needsCredentialRepair) {
                DetailDockIconAction(
                    icon = Icons.Default.Warning,
                    contentDescription = "Update credentials",
                    tint = colorScheme.error,
                    containerColor = colorScheme.errorContainer,
                    onClick = onRepairCredentials
                )
            }
            if (canRename) {
                DetailDockIconAction(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit name",
                    tint = colorScheme.onSecondaryContainer,
                    containerColor = colorScheme.secondaryContainer,
                    onClick = onRename
                )
            }
            DetailDockIconAction(
                icon = Icons.Default.Delete,
                contentDescription = "Disconnect",
                tint = colorScheme.onErrorContainer,
                containerColor = colorScheme.errorContainer,
                onClick = onDisconnect
            )
        }
    }
}

@Composable
private fun DetailDockPrimaryAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (enabled) {
        colorScheme.primaryContainer
    } else {
        colorScheme.surfaceContainerHighest
    }
    val contentColor = if (enabled) {
        colorScheme.onPrimaryContainer
    } else {
        colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
    }
    val actionShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 28.dp,
        bottomStart = 28.dp,
        bottomEnd = 18.dp
    )

    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(actionShape)
            .clickable(enabled = enabled, onClick = onClick),
        color = containerColor,
        contentColor = contentColor,
        shape = actionShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailDockIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    val actionShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 22.dp,
        bottomStart = 22.dp,
        bottomEnd = 18.dp
    )

    Surface(
        modifier = Modifier
            .size(52.dp)
            .clip(actionShape)
            .clickable(onClick = onClick),
        color = containerColor.copy(alpha = 0.92f),
        shape = actionShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }
}

@Composable
private fun DetailErrorStrip(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
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
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
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
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        color = colorScheme.onErrorContainer
                    )
                }
            }
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
private fun DetailLoadingRow() {
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
                    text = "Preparing model quota detail",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "QuotaHub is reading model-level counters and reset windows for this provider.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
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
    val progressColor by animateColorAsState(
        targetValue = riskColor(resource.risk),
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "modelProgressColor"
    )
    val rowShape = expressiveContainerShape(index + 6)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorScheme.surfaceContainerLow,
        shape = rowShape,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = progressColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 14.dp,
                        bottomStart = 14.dp,
                        bottomEnd = 24.dp
                    ),
                    modifier = Modifier.size(54.dp)
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
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = resource.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
            }

            QuotaProgressBar(progress = resource.progress, color = progressColor)
            DetailMetricRow(
                metrics = resource.primaryMetrics,
                risk = resource.risk,
                highEmphasisMetrics = highEmphasisMetrics
            )

            if (resource.secondaryMetrics.isNotEmpty()) {
                Surface(
                    color = progressColor.copy(alpha = 0.08f),
                    shape = expressiveContainerShape(index + 9)
                ) {
                    DetailMetricRow(
                        metrics = resource.secondaryMetrics,
                        risk = resource.risk,
                        highEmphasisMetrics = highEmphasisMetrics,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
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
