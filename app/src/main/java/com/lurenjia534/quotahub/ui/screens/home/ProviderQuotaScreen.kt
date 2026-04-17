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
import com.lurenjia534.quotahub.data.provider.SubscriptionGateway
import com.lurenjia534.quotahub.ui.components.QuotaLoadingIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val CriticalResetThresholdMillis = 60 * 60 * 1000L
private const val WatchResetThresholdMillis = 6 * 60 * 60 * 1000L

private enum class QuotaRisk {
    Healthy,
    Watch,
    Critical
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProviderQuotaScreen(
    modifier: Modifier = Modifier,
    subscriptionGateway: SubscriptionGateway,
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
    var showDisconnectDialog by remember { mutableStateOf(false) }

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
            onRefresh = viewModel::refresh,
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
                                onDismiss = viewModel::refresh
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
                                ModelQuotaSection(modelRemains = uiState.modelRemains)
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
    isBootstrapping: Boolean,
    isRefreshing: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val totalAllowance = modelRemains.sumOf { it.currentIntervalTotalCount }
    val totalRemainingCalls = modelRemains.sumOf { it.currentIntervalUsageCount }
    val totalUsedCalls = (totalAllowance - totalRemainingCalls).coerceAtLeast(0)
    val overallProgress = if (totalAllowance > 0) {
        totalUsedCalls.toFloat() / totalAllowance.toFloat()
    } else {
        0f
    }
    val soonestReset = modelRemains.map { it.remainsTime }.minOrNull()
    val watchCount = modelRemains.count { quotaRiskOf(it) != QuotaRisk.Healthy }
    val dominantRisk = when {
        modelRemains.any { quotaRiskOf(it) == QuotaRisk.Critical } -> QuotaRisk.Critical
        watchCount > 0 -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
    val accentColor = riskColor(dominantRisk)
    val stateLabel = when {
        modelRemains.isEmpty() -> "Waiting for first snapshot"
        dominantRisk == QuotaRisk.Critical -> "Critical attention"
        dominantRisk == QuotaRisk.Watch -> "Watch list active"
        else -> "Healthy coverage"
    }
    val stateDescription = when {
        modelRemains.isEmpty() -> "Pull to refresh once data is available from the provider."
        dominantRisk == QuotaRisk.Critical -> "At least one model is near depletion or its reset window is very close."
        dominantRisk == QuotaRisk.Watch -> "Some models need monitoring, but the subscription is still usable."
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
                                text = "Current interval",
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
                    Text(
                        text = formatCount(totalRemainingCalls),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                    )
                    Text(
                        text = "calls left across ${modelRemains.size} tracked models",
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryMetric(
                            modifier = Modifier.weight(1f),
                            label = "Used",
                            value = "${(overallProgress * 100).roundToInt()}%"
                        )
                        VerticalDivider()
                        SummaryMetric(
                            modifier = Modifier.weight(1f),
                            label = "Soonest reset",
                            value = soonestReset?.let { formatTimeRemaining(it) } ?: "Waiting"
                        )
                        VerticalDivider()
                        SummaryMetric(
                            modifier = Modifier.weight(1f),
                            label = "Models to watch",
                            value = watchCount.toString()
                        )
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
private fun SummaryMetric(
    label: String,
    value: String,
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
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
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
private fun ModelQuotaSection(modelRemains: List<ModelRemain>) {
    SectionSurface {
        modelRemains.forEachIndexed { index, modelRemain ->
            ModelQuotaRow(modelRemain = modelRemain)
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
private fun ModelQuotaRow(modelRemain: ModelRemain) {
    val remaining = modelRemain.currentIntervalUsageCount
    val total = modelRemain.currentIntervalTotalCount
    val used = (total - remaining).coerceAtLeast(0)
    val progress = quotaProgress(remaining = remaining, total = total)
    val risk = quotaRiskOf(modelRemain)
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
                    imageVector = if (risk == QuotaRisk.Critical) Icons.Default.Warning else Icons.Default.CheckCircle,
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
                    Text(
                        text = "Reset in ${formatTimeRemaining(modelRemain.remainsTime)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    label = "Remaining",
                    value = formatCount(remaining),
                    valueColor = progressColor
                )
                DetailMetric(
                    modifier = Modifier.weight(1f),
                    label = "Used",
                    value = "${formatCount(used)} / ${formatCount(total)}"
                )
                DetailMetric(
                    modifier = Modifier.weight(1f),
                    label = "Usage",
                    value = "${(progress * 100).roundToInt()}%"
                )
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
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = resolvedValueColor
            )
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

private fun quotaProgress(remaining: Int, total: Int): Float {
    val used = (total - remaining).coerceAtLeast(0)
    return if (total > 0) used.toFloat() / total.toFloat() else 0f
}

private fun quotaRiskOf(modelRemain: ModelRemain): QuotaRisk {
    val progress = quotaProgress(
        remaining = modelRemain.currentIntervalUsageCount,
        total = modelRemain.currentIntervalTotalCount
    )

    return when {
        progress >= 0.95f || modelRemain.remainsTime <= CriticalResetThresholdMillis -> QuotaRisk.Critical
        progress >= 0.80f || modelRemain.remainsTime <= WatchResetThresholdMillis -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
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
