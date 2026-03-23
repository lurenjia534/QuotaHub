package com.lurenjia534.quotahub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lurenjia534.quotahub.data.repository.QuotaRepository
import com.lurenjia534.quotahub.ui.screens.explore.ExploreScreen
import com.lurenjia534.quotahub.ui.screens.home.HomeScreen
import com.lurenjia534.quotahub.ui.screens.home.MiniMaxQuotaScreen
import com.lurenjia534.quotahub.ui.screens.home.QuotaProvider
import com.lurenjia534.quotahub.ui.screens.profile.ProfileScreen

@Composable
fun QuotaNavHost(
    navController: NavHostController,
    repository: QuotaRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                repository = repository,
                onProviderClick = { provider ->
                    navController.navigate(Screen.ProviderDetail.createRoute(provider.id))
                }
            )
        }

        composable(route = Screen.ProviderDetail.route) { backStackEntry ->
            when (backStackEntry.arguments?.getString(Screen.ProviderDetail.providerIdArg)) {
                QuotaProvider.MiniMax.id -> MiniMaxQuotaScreen(repository = repository)
                else -> HomeScreen(
                    repository = repository,
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
