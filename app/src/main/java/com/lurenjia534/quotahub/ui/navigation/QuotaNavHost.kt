package com.lurenjia534.quotahub.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lurenjia534.quotahub.data.provider.SubscriptionGateway
import com.lurenjia534.quotahub.data.provider.SubscriptionRegistry
import com.lurenjia534.quotahub.ui.components.QuotaLoadingIndicator
import com.lurenjia534.quotahub.ui.screens.home.HomeScreen
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaScreen
import com.lurenjia534.quotahub.ui.screens.settings.SettingsScreen

@Composable
fun QuotaNavHost(
    navController: NavHostController,
    subscriptionRegistry: SubscriptionRegistry,
    highEmphasisMetrics: Boolean,
    hapticConfirmation: Boolean,
    onHighEmphasisMetricsChange: (Boolean) -> Unit,
    onHapticConfirmationChange: (Boolean) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                subscriptionRegistry = subscriptionRegistry,
                highEmphasisMetrics = highEmphasisMetrics,
                bottomContentPadding = bottomContentPadding,
                onSubscriptionClick = { subscriptionId ->
                    navController.navigate(Screen.SubscriptionDetail.createRoute(subscriptionId))
                }
            )
        }

        composable(
            route = Screen.SubscriptionDetail.route,
            arguments = listOf(
                navArgument(Screen.SubscriptionDetail.subscriptionIdArg) {
                    type = androidx.navigation.NavType.LongType
                }
            )
        ) { backStackEntry ->
            val subscriptionId = backStackEntry.arguments?.getLong(Screen.SubscriptionDetail.subscriptionIdArg)
            var subscriptionGateway by remember { mutableStateOf<SubscriptionGateway?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(subscriptionId) {
                if (subscriptionId != null) {
                    subscriptionGateway = subscriptionRegistry.getGatewayById(subscriptionId)
                }
                isLoading = false
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        QuotaLoadingIndicator()
                    }
                }
                subscriptionGateway != null -> {
                    ProviderQuotaScreen(
                        subscriptionGateway = subscriptionGateway!!,
                        highEmphasisMetrics = highEmphasisMetrics,
                        hapticConfirmation = hapticConfirmation,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                else -> {
                    LaunchedEffect(subscriptionId) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        QuotaLoadingIndicator()
                    }
                }
            }
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                highEmphasisMetrics = highEmphasisMetrics,
                hapticConfirmation = hapticConfirmation,
                bottomContentPadding = bottomContentPadding,
                onHighEmphasisMetricsChange = onHighEmphasisMetricsChange,
                onHapticConfirmationChange = onHapticConfirmationChange
            )
        }
    }
}
