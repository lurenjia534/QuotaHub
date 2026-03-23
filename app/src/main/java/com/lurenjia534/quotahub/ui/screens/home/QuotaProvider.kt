package com.lurenjia534.quotahub.ui.screens.home

import androidx.annotation.DrawableRes
import com.lurenjia534.quotahub.R

enum class QuotaProvider(
    val id: String,
    val title: String,
    val subtitle: String,
    @param:DrawableRes val iconRes: Int
) {
    MiniMax(
        id = "minimax",
        title = "MiniMax Coding Plan",
        subtitle = "minimaxi.com",
        iconRes = R.drawable.minimax_color
    );

    companion object {
        fun fromId(id: String): QuotaProvider? {
            return values().firstOrNull { it.id == id }
        }
    }
}
