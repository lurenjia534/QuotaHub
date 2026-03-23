package com.lurenjia534.quotahub.ui.screens.home

import androidx.annotation.DrawableRes
import com.lurenjia534.quotahub.R

enum class QuotaProvider(
    val id: String,
    val title: String,
    val subtitle: String,
    @param:DrawableRes val iconRes: Int,
    val credentialLabel: String,
    val connectDescription: String,
    val detailDescription: String
) {
    MiniMax(
        id = "minimax",
        title = "MiniMax Coding Plan",
        subtitle = "minimaxi.com",
        iconRes = R.drawable.minimax_color,
        credentialLabel = "API Key",
        connectDescription = "Connect to MiniMax API",
        detailDescription = "Monitor your MiniMax quota usage"
    );

    companion object {
        fun fromId(id: String): QuotaProvider? {
            return values().firstOrNull { it.id == id }
        }
    }
}
