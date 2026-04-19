package com.lurenjia534.quotahub.data.provider

import kotlinx.serialization.Serializable

data class CredentialFieldSpec(
    val key: String,
    val label: String,
    val isSecret: Boolean = true,
    val isRequired: Boolean = true
)

data class ProviderDescriptor(
    val id: String,
    val displayName: String,
    val credentialFields: List<CredentialFieldSpec>
) {
    init {
        require(credentialFields.isNotEmpty()) {
            "ProviderDescriptor must expose at least one credential field"
        }
    }

    val primaryCredentialField: CredentialFieldSpec
        get() = credentialFields.first()
}

@Serializable
data class SecretBundle(
    val values: Map<String, String>
) {
    fun value(key: String): String? {
        return values[key]?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun requireValue(key: String): String {
        return value(key) ?: throw IllegalStateException("Missing required credential: $key")
    }

    companion object {
        fun of(values: Map<String, String>): SecretBundle {
            return SecretBundle(
                values = values
                    .mapValues { (_, value) -> value.trim() }
                    .filterValues { it.isNotEmpty() }
            )
        }

        fun single(key: String, value: String): SecretBundle {
            return of(mapOf(key to value))
        }
    }
}
