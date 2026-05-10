package com.lurenjia534.quotahub.ui.screens.home.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.ui.provider.ProviderUiRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProviderBottomSheet(
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
internal fun ProviderCredentialDialog(
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
