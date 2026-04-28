package com.lurenjia534.quotahub.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")

    data object SubscriptionDetail : Screen("subscription/{subscriptionId}") {
        const val subscriptionIdArg = "subscriptionId"

        fun createRoute(subscriptionId: Long): String {
            return "subscription/$subscriptionId"
        }
    }

    data object Settings : Screen("settings")

    data object About : Screen("about")
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Settings
)
