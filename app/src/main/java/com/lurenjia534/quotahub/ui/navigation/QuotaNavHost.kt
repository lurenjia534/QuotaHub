package com.lurenjia534.quotahub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lurenjia534.quotahub.data.repository.QuotaRepository
import com.lurenjia534.quotahub.ui.screens.explore.ExploreScreen
import com.lurenjia534.quotahub.ui.screens.home.HomeScreen
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
            HomeScreen(repository = repository)
        }

        composable(route = Screen.Explore.route) {
            ExploreScreen()
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen()
        }
    }
}