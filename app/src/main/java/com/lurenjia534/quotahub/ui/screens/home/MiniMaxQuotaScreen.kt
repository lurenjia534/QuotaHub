package com.lurenjia534.quotahub.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lurenjia534.quotahub.R
import com.lurenjia534.quotahub.data.model.ModelRemain
import com.lurenjia534.quotahub.data.repository.QuotaRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniMaxQuotaScreen(
    modifier: Modifier = Modifier,
    repository: QuotaRepository
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(repository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = uiState.isLoading && uiState.modelRemains.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                enabled = uiState.hasApiKey && !uiState.isBootstrapping,
                onRefresh = viewModel::fetchModelRemains
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            MiniMaxGreetingSection()
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isBootstrapping || (uiState.isLoading && uiState.modelRemains.isEmpty())) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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
                            contentDescription = null,
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
                QuotaDetailCards(uiState.modelRemains)
            } else if (!uiState.isBootstrapping && !uiState.isLoading) {
                MiniMaxEmptyStateSection()
            }
        }

        PullToRefreshDefaults.Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = isRefreshing,
            state = pullToRefreshState
        )

        if (uiState.showApiKeyDialog) {
            MiniMaxApiKeyDialog(
                apiKey = uiState.apiKey,
                onApiKeyChange = viewModel::updateApiKey,
                onDismiss = viewModel::hideApiKeyDialog,
                onConfirm = viewModel::saveApiKeyAndFetch
            )
        }
    }
}

@Composable
private fun MiniMaxApiKeyDialog(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter API Key") },
        text = {
            Column {
                Text(
                    text = "Enter your MiniMax API key to view your quota information.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = apiKey.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MiniMaxGreetingSection() {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.minimax_color),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "MiniMax Coding Plan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Monitor your MiniMax quota usage",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MiniMaxEmptyStateSection() {
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
                painter = painterResource(R.drawable.minimax_color),
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
                text = "Add your MiniMax API key from the Home page to start syncing quota data.",
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
    val totalRemaining = modelRemains.sumOf { it.currentIntervalUsageCount }

    val progress = if (totalAllowance > 0) {
        (totalAllowance - totalRemaining).toFloat() / totalAllowance.toFloat()
    } else 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OverviewCard(
            title = "Calls Remaining",
            used = totalRemaining.toString(),
            total = totalAllowance.toString(),
            progress = progress,
            icon = Icons.Default.DataUsage,
            iconTint = getProgressColor(progress),
            modifier = Modifier.weight(1f)
        )
        OverviewCard(
            title = "Time Left",
            used = formatTimeRemaining(totalRemainingTime),
            total = "",
            progress = 0f,
            icon = Icons.Default.Schedule,
            iconTint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OverviewCard(
    title: String,
    used: String,
    total: String,
    progress: Float,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = used,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
                if (total.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "/ $total",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            if (progress > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = getProgressColor(progress),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuotaDetailCards(modelRemains: List<ModelRemain>) {
    Text(
        text = "Model Details",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(12.dp))

    modelRemains.forEachIndexed { index, modelRemain ->
        if (index > 0) {
            Spacer(modifier = Modifier.height(12.dp))
        }
        QuotaDetailItem(modelRemain)
    }
}

@Composable
private fun QuotaDetailItem(modelRemain: ModelRemain) {
    val remaining = modelRemain.currentIntervalUsageCount
    val total = modelRemain.currentIntervalTotalCount
    val used = total - remaining
    val progress = if (total > 0) {
        used.toFloat() / total.toFloat()
    } else 0f

    val progressColor = getProgressColor(progress)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (progress >= 1f) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = progressColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = modelRemain.modelName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatTimeRemaining(modelRemain.remainsTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = remaining.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    Text(
                        text = "left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progress * 100).toInt()}% used",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$used / $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
