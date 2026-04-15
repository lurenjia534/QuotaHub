package com.lurenjia534.quotahub.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lurenjia534.quotahub.data.model.ModelRemain
import com.lurenjia534.quotahub.data.provider.SubscriptionGateway
import com.lurenjia534.quotahub.ui.components.QuotaLoadingIndicator

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

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect Subscription") },
            text = { Text("Are you sure you want to disconnect this subscription? All cached quota data will be cleared.") },
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
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
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
            title = { Text("Edit Subscription Name") },
            text = {
                Column {
                    Text(
                        text = "Update how this subscription appears in the app. Leave it blank to use the default name.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.titleInput,
                        onValueChange = viewModel::updateTitleInput,
                        label = { Text("Subscription Name") },
                        singleLine = true,
                        placeholder = { Text("e.g., My China Minimax Plan") },
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
            LargeFlexibleTopAppBar(
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
                            text = uiState.subscription.provider.subtitle,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullToRefresh(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    enabled = uiState.isConnected && !uiState.isBootstrapping,
                    onRefresh = viewModel::refresh
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (uiState.isBootstrapping || (uiState.isLoading && uiState.modelRemains.isEmpty())) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        QuotaLoadingIndicator()
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (uiState.error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (uiState.modelRemains.isNotEmpty()) {
                    QuotaOverviewSection(uiState.modelRemains)
                    Spacer(modifier = Modifier.height(24.dp))
                    QuotaDetailSection(uiState.modelRemains)
                } else if (!uiState.isBootstrapping && !uiState.isLoading) {
                    ProviderEmptyStateSection(provider = uiState.subscription.provider)
                }
            }

            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = pullToRefreshState
            )
        }
    }
}

@Composable
private fun ProviderEmptyStateSection(provider: QuotaProvider) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(provider.iconRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No quota data yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pull down to refresh and load quota data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuotaOverviewSection(modelRemains: List<ModelRemain>) {
    val totalRemainingTime = modelRemains.sumOf { it.remainsTime }
    val totalAllowance = modelRemains.sumOf { it.currentIntervalTotalCount }
    val totalRemainingCalls = modelRemains.sumOf { it.currentIntervalUsageCount }
    val totalUsedCalls = (totalAllowance - totalRemainingCalls).coerceAtLeast(0)

    val progress = if (totalAllowance > 0) {
        totalUsedCalls.toFloat() / totalAllowance.toFloat()
    } else 0f

    SectionSurface {
        ListItem(
            overlineContent = {
                Text(
                    text = "Calls Remaining",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            headlineContent = {
                Text(
                    text = formatCount(totalRemainingCalls),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "of ${formatCount(totalAllowance)} across ${modelRemains.size} models",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = getProgressColor(progress),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.DataUsage,
                    contentDescription = null,
                    tint = getProgressColor(progress)
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        ListItem(
            overlineContent = {
                Text(
                    text = "Time Left",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            headlineContent = {
                Text(
                    text = formatTimeRemaining(totalRemainingTime),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Text(
                    text = "Aggregate remaining availability across ${modelRemains.size} models",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun QuotaDetailSection(modelRemains: List<ModelRemain>) {
    Text(
        text = "Model Details",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(12.dp))

    modelRemains.forEachIndexed { index, modelRemain ->
        QuotaDetailListItem(modelRemain)
        if (index < modelRemains.lastIndex) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun QuotaDetailListItem(modelRemain: ModelRemain) {
    val remaining = modelRemain.currentIntervalUsageCount
    val total = modelRemain.currentIntervalTotalCount
    val used = total - remaining
    val progress = if (total > 0) {
        used.toFloat() / total.toFloat()
    } else 0f
    val remainingLabel = "${formatCount(remaining)} left"

    val progressColor = getProgressColor(progress)

    ListItem(
        overlineContent = {
            Text(
                text = formatTimeRemaining(modelRemain.remainsTime),
                style = MaterialTheme.typography.labelMedium
            )
        },
        headlineContent = {
            Text(
                text = modelRemain.modelName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "${formatCount(used)} / ${formatCount(total)} used",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (progress >= 1f) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = if (progress >= 1f) "Quota depleted" else "Quota available",
                tint = progressColor
            )
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = remainingLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = progressColor
                )
                Text(
                    text = "${(progress * 100).toInt()}% used",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SectionSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun getProgressColor(progress: Float): Color {
    return when {
        progress >= 0.95f -> MaterialTheme.colorScheme.error
        progress >= 0.80f -> MaterialTheme.colorScheme.tertiary
        progress >= 0.60f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
}
