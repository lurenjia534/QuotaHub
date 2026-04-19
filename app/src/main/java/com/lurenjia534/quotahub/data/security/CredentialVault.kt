package com.lurenjia534.quotahub.data.security

import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.SecretBundle
import kotlinx.serialization.json.Json

sealed interface VaultCredentialState {
    data class Available(val credentials: SecretBundle) : VaultCredentialState

    data class Missing(val reason: String) : VaultCredentialState

    data class Broken(val reason: String) : VaultCredentialState
}

interface CredentialVault {
    fun seal(credentials: SecretBundle): String

    fun resolve(
        storedPayload: String,
        provider: ProviderDescriptor
    ): VaultCredentialState
}

class EncryptedCredentialVault(
    private val cipher: ApiKeyCipher,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
) : CredentialVault {
    override fun seal(credentials: SecretBundle): String {
        return cipher.encrypt(
            json.encodeToString(SecretBundle.serializer(), credentials)
        )
    }

    override fun resolve(
        storedPayload: String,
        provider: ProviderDescriptor
    ): VaultCredentialState {
        return runCatching {
            val decryptedPayload = cipher.decrypt(storedPayload)
            runCatching {
                json.decodeFromString(SecretBundle.serializer(), decryptedPayload)
            }.getOrElse {
                SecretBundle.single(provider.primaryCredentialField.key, decryptedPayload)
            }
        }.fold(
            onSuccess = { credentials ->
                val missingFields = provider.credentialFields.filter { field ->
                    field.isRequired && credentials.value(field.key) == null
                }
                if (missingFields.isEmpty()) {
                    VaultCredentialState.Available(credentials)
                } else {
                    VaultCredentialState.Missing(
                        reason = "Stored credentials are incomplete. Enter ${missingFields.joinToString { it.label }} again."
                    )
                }
            },
            onFailure = { error ->
                VaultCredentialState.Broken(
                    reason = error.message
                        ?: "Stored credentials can no longer be read on this device."
                )
            }
        )
    }
}
