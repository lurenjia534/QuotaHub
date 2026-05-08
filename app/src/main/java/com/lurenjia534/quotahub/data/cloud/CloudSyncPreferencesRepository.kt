package com.lurenjia534.quotahub.data.cloud

import android.content.Context
import com.lurenjia534.quotahub.data.security.ApiKeyCipher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CloudSyncSettings(
    val enabled: Boolean = false,
    val relayBaseUrl: String = "",
    val hasClientToken: Boolean = false,
    val lastSyncAt: Long? = null,
    val lastResultMessage: String? = null
) {
    val isConfigured: Boolean
        get() = relayBaseUrl.isNotBlank() && hasClientToken
}

class CloudSyncPreferencesRepository(
    context: Context,
    private val cipher: ApiKeyCipher
) {
    private val sharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<CloudSyncSettings> = _settings.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        _settings.value = _settings.value.copy(enabled = enabled)
    }

    fun setRelayBaseUrl(baseUrl: String) {
        sharedPreferences.edit()
            .putString(KEY_RELAY_BASE_URL, baseUrl.trim())
            .apply()
        _settings.value = _settings.value.copy(relayBaseUrl = baseUrl.trim())
    }

    fun setClientToken(token: String) {
        val trimmedToken = token.trim()
        if (trimmedToken.isBlank()) {
            return
        }
        sharedPreferences.edit()
            .putString(KEY_CLIENT_TOKEN, cipher.encrypt(trimmedToken))
            .apply()
        _settings.value = _settings.value.copy(hasClientToken = true)
    }

    fun clearClientToken() {
        sharedPreferences.edit()
            .remove(KEY_CLIENT_TOKEN)
            .apply()
        _settings.value = _settings.value.copy(hasClientToken = false)
    }

    fun clientToken(): Result<String> {
        val encryptedToken = sharedPreferences.getString(KEY_CLIENT_TOKEN, null)
            ?: return Result.failure(IllegalStateException("Client token is not set."))
        return runCatching { cipher.decrypt(encryptedToken) }
    }

    fun recordSyncResult(message: String, syncedAt: Long? = System.currentTimeMillis()) {
        sharedPreferences.edit()
            .putString(KEY_LAST_RESULT_MESSAGE, message)
            .apply {
                if (syncedAt != null) {
                    putLong(KEY_LAST_SYNC_AT, syncedAt)
                }
            }
            .apply()
        _settings.value = loadSettings()
    }

    private fun loadSettings(): CloudSyncSettings {
        return CloudSyncSettings(
            enabled = sharedPreferences.getBoolean(KEY_ENABLED, false),
            relayBaseUrl = sharedPreferences.getString(KEY_RELAY_BASE_URL, null).orEmpty(),
            hasClientToken = sharedPreferences.contains(KEY_CLIENT_TOKEN),
            lastSyncAt = sharedPreferences.takeIf { it.contains(KEY_LAST_SYNC_AT) }
                ?.getLong(KEY_LAST_SYNC_AT, 0L)
                ?.takeIf { it > 0L },
            lastResultMessage = sharedPreferences.getString(KEY_LAST_RESULT_MESSAGE, null)
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "quotahub_cloud_sync_preferences"
        const val KEY_ENABLED = "enabled"
        const val KEY_RELAY_BASE_URL = "relay_base_url"
        const val KEY_CLIENT_TOKEN = "client_token"
        const val KEY_LAST_SYNC_AT = "last_sync_at"
        const val KEY_LAST_RESULT_MESSAGE = "last_result_message"
    }
}
