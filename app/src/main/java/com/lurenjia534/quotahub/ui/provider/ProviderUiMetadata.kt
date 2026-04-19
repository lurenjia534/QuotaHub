package com.lurenjia534.quotahub.ui.provider

import androidx.annotation.DrawableRes
import com.lurenjia534.quotahub.R
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.minimax.MiniMaxCodingPlanProvider

data class ProviderUiMetadata(
    val subtitle: String,
    @param:DrawableRes val iconRes: Int,
    val connectDescription: String,
    val detailDescription: String
)

object ProviderUiRegistry {
    private val metadataById = mapOf(
        MiniMaxCodingPlanProvider.ID to ProviderUiMetadata(
            subtitle = "minimaxi.com",
            iconRes = R.drawable.minimax_color,
            connectDescription = "Connect to MiniMax API",
            detailDescription = "Monitor your MiniMax quota usage"
        )
    )

    fun require(provider: ProviderDescriptor): ProviderUiMetadata {
        return require(provider.id)
    }

    fun require(providerId: String): ProviderUiMetadata {
        return metadataById[providerId]
            ?: throw IllegalArgumentException("Missing UI metadata for provider: $providerId")
    }
}
