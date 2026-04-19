package com.lurenjia534.quotahub.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val providerUi = providerUiRegistry.require(uiState.subscription.provider)
    val pullToRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val isRefreshing = uiState.isLoading && uiState.detail.hasData
    val needsCredentialRepair = uiState.subscription.syncStatus.state == SyncState.AuthFailed
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

    if (uiState.showCredentialDialog) {
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
                        text = "Enter a fresh set of ${uiState.subscription.provider.displayName} credentials. QuotaHub will validate them before replacing the stored secret.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    uiState.subscription.provider.credentialFields.forEachIndexed { index, field ->
                        ProviderCredentialInputField(
                            field = field,
                            value = uiState.credentialInputs[field.key].orEmpty(),
                            isLastField = index == uiState.subscription.provider.credentialFields.lastIndex,
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
                    enabled = uiState.subscription.provider.credentialFields.all {
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

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = uiState.subscription.displayTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                subtitle = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(providerUi.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.subscription.provider.displayName} • ${providerUi.subtitle}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                actions = {
                    if (needsCredentialRepair) {
                        IconButton(onClick = viewModel::showCredentialDialog) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Update credentials",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = viewModel::showRenameDialog) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit name"
                        )
                    }
                    IconButton(onClick = { showDisconnectDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Disconnect",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = requestRefresh,
            state = pullToRefreshState,
            enabled = uiState.isConnected && !uiState.isBootstrapping,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
                contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    AnimatedSection(visible = summaryVisible) {
                        QuotaSummaryBoard(
                            providerIconRes = providerUi.iconRes,
                            summary = uiState.detail.summary,
                            highEmphasisMetrics = highEmphasisMetrics,
                            isBootstrapping = uiState.isBootstrapping,
                            isRefreshing = isRefreshing
                        )
                    }
                }

                if (uiState.error != null) {
                    item {
                        AnimatedSection(visible = statusVisible) {
                            DetailErrorStrip(
                                title = if (needsCredentialRepair) {
                                    "Credentials need attention"
                                } else {
                                    "Refresh failed"
                                },
                                message = uiState.error!!,
                                actionLabel = if (needsCredentialRepair) {
                                    "Update credentials"
                                } else {
                                    "Retry"
                                },
                                onAction = if (needsCredentialRepair) {
                                    viewModel::showCredentialDialog
                                } else {
                                    requestRefresh
                                }
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
private fun QuotaSummaryBoard(
    providerIconRes: Int,
    summary: ProviderQuotaSummaryUiModel?,
    highEmphasisMetrics: Boolean,
    isBootstrapping: Boolean,
    isRefreshing: Boolean
) {
    if (summary == null) {
        return
    }

    val colorScheme = MaterialTheme.colorScheme
    val accentColor = riskColor(summary.risk)

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
                            accentColor.copy(alpha = 0.24f),
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = accentColor.copy(alpha = 0.22f),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(providerIconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Current limits",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = summary.stateLabel,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                    RiskPill(risk = summary.risk)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Sync ${summary.syncLabel}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = colorScheme.onSurfaceVariant
                        )
                    )
                    QuotaMetricText(
                        text = summary.headlineValue,
                        emphasized = highEmphasisMetrics,
                        level = MetricEmphasisLevel.Hero
                    )
                    Text(
                        text = summary.headlineLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = summary.stateDescription,
                        style = MaterialTheme.typography.bodySmall.copy(
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

                Surface(
                    color = colorScheme.surface.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column {
                        SummaryMetricRow(
                            row = summary.primaryMetrics,
                            highEmphasisMetrics = highEmphasisMetrics
                        )
                        summary.secondaryMetrics?.let { secondaryMetrics ->
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                            SummaryMetricRow(
                                row = secondaryMetrics,
                                highEmphasisMetrics = highEmphasisMetrics
                            )
                        }
                    }
                }

                if (isBootstrapping || isRefreshing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QuotaLoadingIndicator(modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isBootstrapping) {
                                "Reading cached data and preparing provider detail"
                            } else {
                                "Refreshing provider quota snapshot"
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
private fun SummaryMetricRow(
    row: SummaryMetricRowUiModel,
    highEmphasisMetrics: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SummaryMetric(
            modifier = Modifier.weight(1f),
            metric = row.first,
            highEmphasisMetrics = highEmphasisMetrics
        )
        VerticalDivider()
        SummaryMetric(
            modifier = Modifier.weight(1f),
            metric = row.second,
            highEmphasisMetrics = highEmphasisMetrics
        )
        VerticalDivider()
        SummaryMetric(
            modifier = Modifier.weight(1f),
            metric = row.third,
            highEmphasisMetrics = highEmphasisMetrics
        )
    }
}

@Composable
private fun SummaryMetric(
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
            )
        )
        QuotaMetricText(
            text = metric.value,
            emphasized = highEmphasisMetrics,
            level = MetricEmphasisLevel.Standard
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}

@Composable
private fun DetailErrorStrip(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
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
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    color = colorScheme.onErrorContainer
                )
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
private fun ModelQuotaSection(
    resources: List<ProviderQuotaResourceUiModel>,
    highEmphasisMetrics: Boolean
) {
    SectionSurface {
        resources.forEachIndexed { index, resource ->
            ModelQuotaRow(
                resource = resource,
                highEmphasisMetrics = highEmphasisMetrics
            )
            if (index < resources.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 88.dp, end = 18.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ModelQuotaRow(
    resource: ProviderQuotaResourceUiModel,
    highEmphasisMetrics: Boolean
) {
    val progressColor = riskColor(resource.risk)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = progressColor.copy(alpha = 0.18f),
            shape = CircleShape,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (resource.risk == QuotaRisk.Healthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = progressColor
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

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
                        text = resource.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    QuotaMetricText(
                        text = resource.resetLabel,
                        emphasized = highEmphasisMetrics,
                        level = MetricEmphasisLevel.Compact,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                RiskPill(risk = resource.risk)
            }

            QuotaProgressBar(progress = resource.progress, color = progressColor)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                resource.primaryMetrics.forEach { metric ->
                    DetailMetric(
                        modifier = Modifier.weight(1f),
                        metric = metric,
                        risk = resource.risk,
                        highEmphasisMetrics = highEmphasisMetrics
                    )
                }
            }

            if (resource.secondaryMetrics.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    resource.secondaryMetrics.forEach { metric ->
                        DetailMetric(
                            modifier = Modifier.weight(1f),
                            metric = metric,
                            risk = resource.risk,
                            highEmphasisMetrics = highEmphasisMetrics
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotaProgressBar(
    progress: Float,
    color: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(99.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(99.dp)
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
            )
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
                        painter = painterResource(providerIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
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
private fun SectionSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 1.dp
    ) {
        Column(content = content)
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
