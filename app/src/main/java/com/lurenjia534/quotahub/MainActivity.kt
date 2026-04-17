package com.lurenjia534.quotahub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.data.preferences.UiPreferencesRepository
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lurenjia534.quotahub.ui.components.QuotaNavigationBar
import com.lurenjia534.quotahub.ui.navigation.QuotaNavHost
import com.lurenjia534.quotahub.ui.navigation.bottomNavItems
import com.lurenjia534.quotahub.ui.theme.QuotaHubTheme

private val FloatingBottomNavClearance = 120.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val application = application as QuotaApplication
        val subscriptionRegistry = application.subscriptionRegistry
        val uiPreferencesRepository = application.uiPreferencesRepository
        setContent {
            QuotaHubTheme {
                QuotaApp(
                    subscriptionRegistry = subscriptionRegistry,
                    uiPreferencesRepository = uiPreferencesRepository
                )
            }
        }
    }
}

@Composable
fun QuotaApp(
    subscriptionRegistry: com.lurenjia534.quotahub.data.provider.SubscriptionRegistry,
    uiPreferencesRepository: UiPreferencesRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val uiPreferences by uiPreferencesRepository.preferences.collectAsState()
    val showBottomNavigation = currentRoute in bottomNavItems.map { it.route }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                QuotaNavHost(
                    navController = navController,
                    subscriptionRegistry = subscriptionRegistry,
                    highEmphasisMetrics = uiPreferences.highEmphasisMetrics,
                    hapticConfirmation = uiPreferences.hapticConfirmation,
                    onHighEmphasisMetricsChange = uiPreferencesRepository::setHighEmphasisMetrics,
                    onHapticConfirmationChange = uiPreferencesRepository::setHapticConfirmation,
                    bottomContentPadding = if (showBottomNavigation) FloatingBottomNavClearance else 0.dp,
                    modifier = Modifier.fillMaxSize()
                )

                if (showBottomNavigation) {
                    QuotaNavigationBar(
                        navController = navController,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
