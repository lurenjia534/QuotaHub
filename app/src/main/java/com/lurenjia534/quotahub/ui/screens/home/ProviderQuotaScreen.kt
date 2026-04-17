package com.lurenjia534.quotahub.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lurenjia534.quotahub.data.model.ModelRemain
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.atRiskModelCount
import com.lurenjia534.quotahub.data.model.dominantQuotaRisk
import com.lurenjia534.quotahub.data.model.effectiveUsageProgress
import com.lurenjia534.quotahub.data.model.effectiveRemainingCount
import com.lurenjia534.quotahub.data.model.hasVisibleWeeklyQuota
import com.lurenjia534.quotahub.data.model.hasPlanLevelWeeklyQuota
import com.lurenjia534.quotahub.data.model.intervalRemainingCount
import com.lurenjia534.quotahub.data.model.intervalUsageProgress
import com.lurenjia534.quotahub.data.model.intervalUsedCount
import com.lurenjia534.quotahub.data.model.quotaRisk
import com.lurenjia534.quotahub.data.model.relevantResetTime
import com.lurenjia534.quotahub.data.model.weeklyRemainingCount
import com.lurenjia534.quotahub.data.model.weeklyUsedCount
import com.lurenjia534.quotahub.data.provider.SubscriptionGateway
import com.lurenjia534.quotahub.ui.components.MetricEmphasisLevel
import com.lurenjia534.quotahub.ui.components.rememberQuotaHaptics
import com.lurenjia534.quotahub.ui.components.QuotaMetricText
import com.lurenjia534.quotahub.ui.components.QuotaLoadingIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProviderQuotaScreen(
    modifier: Modifier = Modifier,
    subscriptionGateway: SubscriptionGateway,
    highEmphasisMetrics: Boolean,
    hapticConfirmation: Boolean,
    onBackClick: () -> Unit
) {
    val viewModel: ProviderQuotaViewModel = viewModel(
        key = "provider-quota-${subscriptionGateway.subscription.id}",
        factory = ProviderQuotaViewModel.Factory(subscriptionGateway)
    )
    val uiState by viewModel.uiState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val isRefreshing = uiState.isLoading && uiState.modelRemains.isNotEmpty()
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
                        placeholder = { Text("e.g., My China MiniMax plan") },
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
                            painter = painterResource(uiState.subscription.provider.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.subscription.subtitle,
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
                            subscription = uiState.subscription,
                            modelRemains = uiState.modelRemains,
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
                                message = uiState.error!!,
                                onDismiss = requestRefresh
                            )
                        }
                    }
                }

                when {
                    uiState.isBootstrapping || (uiState.isLoading && uiState.modelRemains.isEmpty()) -> {
                        item {
                            AnimatedSection(visible = statusVisible) {
                                DetailLoadingRow()
                            }
                        }
                    }

                    uiState.modelRemains.isNotEmpty() -> {
                        item {
                            AnimatedSection(visible = modelsVisible) {
                                SectionHeader(
                                    title = "Model quota",
                                    subtitle = "Interval usage is grouped here for quick scanning. Pull down anytime to refresh remote values."
                                )
                            }
                        }
                        item {
                            AnimatedSection(visible = modelsVisible) {
                                ModelQuotaSection(
                                    modelRemains = uiState.modelRemains,
                                    highEmphasisMetrics = highEmphasisMetrics
                                )
                            }
                        }
                    }

                    else -> {
                        item {
                            AnimatedSection(visible = modelsVisible) {
                                DetailEmptyState(provider = uiState.subscription.provider)
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
    subscription: com.lurenjia534.quotahub.data.model.Subscription,
    modelRemains: List<ModelRemain>,
    highEmphasisMetrics: Boolean,
    isBootstrapping: Boolean,
    isRefreshing: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val planHasWeeklyQuota = modelRemains.hasPlanLevelWeeklyQuota
    val totalIntervalAllowance = modelRemains.sumOf { it.currentIntervalTotalCount }
    val totalIntervalUsed = modelRemains.sumOf { it.intervalUsedCount }
    val totalIntervalRemaining = modelRemains.sumOf { it.intervalRemainingCount }
    val modelsWithVisibleWeeklyQuota = modelRemains.filter { it.hasVisibleWeeklyQuota(planHasWeeklyQuota) }
    val totalWeeklyAllowance = modelsWithVisibleWeeklyQuota.sumOf { it.currentWeeklyTotalCount }
    val totalWeeklyUsed = modelsWithVisibleWeeklyQuota.sumOf { it.weeklyUsedCount }
    val totalWeeklyRemaining = modelsWithVisibleWeeklyQuota.sumOf { it.weeklyRemainingCount }
    val totalEffectiveRemaining = modelRemains.sumOf {
        it.effectiveRemainingCount(planHasWeeklyQuota)
    }
    val intervalProgress = if (totalIntervalAllowance > 0) {
        totalIntervalUsed.toFloat() / totalIntervalAllowance.toFloat()
    } else {
        0f
    }
    val weeklyProgress = if (totalWeeklyAllowance > 0) {
        totalWeeklyUsed.toFloat() / totalWeeklyAllowance.toFloat()
    } else {
        0f
    }
    val soonestReset = modelRemains.mapNotNull {
        it.relevantResetTime(planHasWeeklyQuota)
    }.minOrNull()
    val soonestIntervalReset = modelRemains.map { it.remainsTime }.minOrNull()
    val soonestWeeklyReset = modelsWithVisibleWeeklyQuota
        .map { it.weeklyRemainsTime }
        .minOrNull()
    val watchCount = modelRemains.atRiskModelCount(planHasWeeklyQuota)
    val dominantRisk = modelRemains.dominantQuotaRisk(planHasWeeklyQuota)
    val accentColor = riskColor(dominantRisk)
    val stateLabel = when {
        modelRemains.isEmpty() -> "Waiting for first snapshot"
        dominantRisk == QuotaRisk.Critical -> "Critical attention"
        dominantRisk == QuotaRisk.Watch -> "Watch list active"
        planHasWeeklyQuota -> "Interval and weekly caps active"
        else -> "Healthy coverage"
    }
    val stateDescription = when {
        modelRemains.isEmpty() -> "Pull to refresh once data is available from the provider."
        dominantRisk == QuotaRisk.Critical -> "At least one model is close to exhausting its available quota."
        dominantRisk == QuotaRisk.Watch -> "Some models are trending low and should be monitored."
        planHasWeeklyQuota -> "This plan combines interval and weekly caps. The headline value reflects the tighter limit for each model."
        else -> "Quota levels are stable and no model is near its critical threshold."
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
                                    painter = painterResource(subscription.provider.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (planHasWeeklyQuota) "Current limits" else "Current interval",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = stateLabel,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                    RiskPill(risk = dominantRisk)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuotaMetricText(
                        text = formatCount(totalEffectiveRemaining),
                        emphasized = highEmphasisMetrics,
                        level = MetricEmphasisLevel.Hero
                    )
                    Text(
                        text = if (planHasWeeklyQuota) {
                            "usable calls left across ${modelRemains.size} tracked models"
                        } else {
                            "calls left across ${modelRemains.size} tracked models"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = stateDescription,
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
                            highEmphasisMetrics = highEmphasisMetrics,
                            firstLabel = if (planHasWeeklyQuota) "Interval left" else "Calls left",
                            firstValue = formatCount(totalIntervalRemaining),
                            secondLabel = if (planHasWeeklyQuota) "Interval reset" else "Soonest reset",
                            secondValue = (if (planHasWeeklyQuota) soonestIntervalReset else soonestReset)
                                ?.let { formatTimeRemaining(it) }
                                ?: "Waiting",
                            thirdLabel = "Models to watch",
                            thirdValue = watchCount.toString()
                        )
                        if (planHasWeeklyQuota) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                            SummaryMetricRow(
                                highEmphasisMetrics = highEmphasisMetrics,
                                firstLabel = "Weekly left",
                                firstValue = formatCount(totalWeeklyRemaining),
                                secondLabel = "Weekly reset",
                                secondValue = soonestWeeklyReset?.let { formatTimeRemaining(it) } ?: "Waiting",
                                thirdLabel = "Weekly used",
                                thirdValue = "${(weeklyProgress * 100).roundToInt()}%"
                            )
                        } else {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                            SummaryMetricRow(
                                highEmphasisMetrics = highEmphasisMetrics,
                                firstLabel = "Used",
                                firstValue = "${(intervalProgress * 100).roundToInt()}%",
                                secondLabel = "Interval total",
                                secondValue = formatCount(totalIntervalAllowance),
                                thirdLabel = "Used calls",
                                thirdValue = formatCount(totalIntervalUsed)
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
    highEmphasisMetrics: Boolean,
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
    thirdLabel: String,
    thirdValue: String
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
            label = firstLabel,
            value = firstValue,
            highEmphasisMetrics = highEmphasisMetrics
        )
        VerticalDivider()
        SummaryMetric(
            modifier = Modifier.weight(1f),
            label = secondLabel,
            value = secondValue,
            highEmphasisMetrics = highEmphasisMetrics
        )
        VerticalDivider()
        SummaryMetric(
            modifier = Modifier.weight(1f),
            label = thirdLabel,
            value = thirdValue,
            highEmphasisMetrics = highEmphasisMetrics
        )
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    highEmphasisMetrics: Boolean,
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
                    text = "Refresh failed",
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
                    text = "Retry",
                    color = colorScheme.onErrorContainer
                )
            }
        }
    }
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
    modelRemains: List<ModelRemain>,
    highEmphasisMetrics: Boolean
) {
    val planHasWeeklyQuota = modelRemains.hasPlanLevelWeeklyQuota

    SectionSurface {
        modelRemains.forEachIndexed { index, modelRemain ->
            ModelQuotaRow(
                modelRemain = modelRemain,
                planHasWeeklyQuota = planHasWeeklyQuota,
                highEmphasisMetrics = highEmphasisMetrics
            )
            if (index < modelRemains.lastIndex) {
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
    modelRemain: ModelRemain,
    planHasWeeklyQuota: Boolean,
    highEmphasisMetrics: Boolean
) {
    val intervalRemaining = modelRemain.intervalRemainingCount
    val intervalUsed = modelRemain.intervalUsedCount
    val intervalTotal = modelRemain.currentIntervalTotalCount
    val weeklyRemaining = modelRemain.weeklyRemainingCount
    val weeklyUsed = modelRemain.weeklyUsedCount
    val weeklyTotal = modelRemain.currentWeeklyTotalCount
    val showWeeklyQuota = modelRemain.hasVisibleWeeklyQuota(planHasWeeklyQuota)
    val progress = modelRemain.effectiveUsageProgress(planHasWeeklyQuota)
    val risk = modelRemain.quotaRisk(planHasWeeklyQuota)
    val progressColor = riskColor(risk)

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
                    imageVector = if (risk == QuotaRisk.Healthy) Icons.Default.CheckCircle else Icons.Default.Warning,
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
                        text = modelRemain.modelName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    QuotaMetricText(
                        text = if (showWeeklyQuota) {
                            "Interval reset in ${formatTimeRemaining(modelRemain.remainsTime)}"
                        } else {
                            "Reset in ${formatTimeRemaining(modelRemain.remainsTime)}"
                        },
                        emphasized = highEmphasisMetrics,
                        level = MetricEmphasisLevel.Compact,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                RiskPill(risk = risk)
            }

            QuotaProgressBar(progress = progress, color = progressColor)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DetailMetric(
                    modifier = Modifier.weight(1f),
                    label = if (showWeeklyQuota) "Interval left" else "Remaining",
                    value = formatCount(intervalRemaining),
                    highEmphasisMetrics = highEmphasisMetrics,
                    valueColor = progressColor
                )
                DetailMetric(
                    modifier = Modifier.weight(1f),
                    label = "Used",
                    value = "${formatCount(intervalUsed)} / ${formatCount(intervalTotal)}",
                    highEmphasisMetrics = highEmphasisMetrics
                )
                DetailMetric(
                    modifier = Modifier.weight(1f),
                    label = if (showWeeklyQuota) "Interval usage" else "Usage",
                    value = "${(modelRemain.intervalUsageProgress * 100).roundToInt()}%",
                    highEmphasisMetrics = highEmphasisMetrics
                )
            }

            if (showWeeklyQuota) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DetailMetric(
                        modifier = Modifier.weight(1f),
                        label = "Weekly left",
                        value = formatCount(weeklyRemaining),
                        highEmphasisMetrics = highEmphasisMetrics,
                        valueColor = progressColor
                    )
                    DetailMetric(
                        modifier = Modifier.weight(1f),
                        label = "Weekly used",
                        value = "${formatCount(weeklyUsed)} / ${formatCount(weeklyTotal)}",
                        highEmphasisMetrics = highEmphasisMetrics
                    )
                    DetailMetric(
                        modifier = Modifier.weight(1f),
                        label = "Weekly reset",
                        value = formatTimeRemaining(modelRemain.weeklyRemainsTime),
                        highEmphasisMetrics = highEmphasisMetrics
                    )
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
            level = MetricEmphasisLevel.Compact,
            color = resolvedValueColor
        )
    }
}

@Composable
private fun DetailEmptyState(provider: QuotaProvider) {
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
                        painter = painterResource(provider.iconRes),
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
