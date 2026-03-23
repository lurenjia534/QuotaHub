package com.lurenjia534.quotahub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lurenjia534.quotahub.data.provider.QuotaProviderRegistry
import com.lurenjia534.quotahub.ui.screens.explore.ExploreScreen
import com.lurenjia534.quotahub.ui.screens.home.HomeScreen
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaScreen
import com.lurenjia534.quotahub.ui.screens.profile.ProfileScreen

@Composable
fun QuotaNavHost(
    navController: NavHostController,
    providerRegistry: QuotaProviderRegistry,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                providerRegistry = providerRegistry,
                onProviderClick = { provider ->
                    navController.navigate(Screen.ProviderDetail.createRoute(provider.id))
                }
            )
        }

        composable(route = Screen.ProviderDetail.route) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString(Screen.ProviderDetail.providerIdArg)
            val providerGateway = providerRegistry.get(providerId.orEmpty())

            if (providerGateway != null) {
                ProviderQuotaScreen(
                    providerGateway = providerGateway,
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                HomeScreen(
                    providerRegistry = providerRegistry,
                    onProviderClick = { provider ->
                        navController.navigate(Screen.ProviderDetail.createRoute(provider.id))
                    }
                )
            }
        }

        composable(route = Screen.Explore.route) {
            ExploreScreen()
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen()
        }
    }
}
