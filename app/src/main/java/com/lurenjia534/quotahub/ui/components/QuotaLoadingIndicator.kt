package com.lurenjia534.quotahub.ui.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuotaLoadingIndicator(modifier: Modifier = Modifier) {
    LoadingIndicator(
        modifier = modifier.size(48.dp),
        color = MaterialTheme.colorScheme.primary
    )
}
