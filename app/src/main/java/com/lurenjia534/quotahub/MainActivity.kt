package com.lurenjia534.quotahub

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.data.preferences.UiPreferencesRepository
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lurenjia534.quotahub.ui.components.QuotaNavigationBar
import com.lurenjia534.quotahub.ui.navigation.BottomNavAction
import com.lurenjia534.quotahub.ui.navigation.QuotaNavHost
import com.lurenjia534.quotahub.ui.navigation.bottomNavItems
import com.lurenjia534.quotahub.ui.navigation.bottomNavItemsData
import com.lurenjia534.quotahub.ui.navigation.Screen
import com.lurenjia534.quotahub.ui.provider.ProviderUiRegistry
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaDetailProjectorRegistry
import com.lurenjia534.quotahub.sync.SubscriptionRefreshPolicy
import com.lurenjia534.quotahub.ui.theme.QuotaHubTheme

private val FloatingBottomNavClearance = 120.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        val application = application as QuotaApplication
        val subscriptionRegistry = application.subscriptionRegistry
        val providerQuotaDetailProjectorRegistry = application.providerQuotaDetailProjectorRegistry
        val providerUiRegistry = application.providerUiRegistry
        val uiPreferencesRepository = application.uiPreferencesRepository
        val subscriptionRefreshPolicy = application.subscriptionRefreshPolicy
        setContent {
            QuotaHubTheme {
                QuotaApp(
                    subscriptionRegistry = subscriptionRegistry,
                    providerQuotaDetailProjectorRegistry = providerQuotaDetailProjectorRegistry,
                    providerUiRegistry = providerUiRegistry,
                    uiPreferencesRepository = uiPreferencesRepository,
                    subscriptionRefreshPolicy = subscriptionRefreshPolicy
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideNavigationBar()
        }
    }

    private fun hideNavigationBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}

@Composable
fun QuotaApp(
    subscriptionRegistry: com.lurenjia534.quotahub.data.provider.SubscriptionRegistry,
    providerQuotaDetailProjectorRegistry: ProviderQuotaDetailProjectorRegistry,
    providerUiRegistry: ProviderUiRegistry,
    uiPreferencesRepository: UiPreferencesRepository,
    subscriptionRefreshPolicy: SubscriptionRefreshPolicy
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val uiPreferences by uiPreferencesRepository.preferences.collectAsState()
    val subscriptionCards by subscriptionRegistry.snapshots.collectAsState(initial = emptyList())
    val navigationItems = remember(subscriptionCards) {
        bottomNavItemsData(showAddSubscription = subscriptionCards.isNotEmpty())
    }
    var addSubscriptionRequestKey by remember { mutableIntStateOf(0) }
    val showBottomNavigation = currentRoute in bottomNavItems.map { it.route }
    val landscapeMonitorMode = uiPreferences.landscapeMonitorMode

    LandscapeMonitorModeEffect(enabled = landscapeMonitorMode)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                val appContentModifier = if (landscapeMonitorMode) {
                    Modifier
                        .fillMaxSize()
                        .keepScreenOn()
                        .padding(innerPadding)
                } else {
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                }

                Box(modifier = appContentModifier) {
                    QuotaNavHost(
                        navController = navController,
                        subscriptionRegistry = subscriptionRegistry,
                        providerQuotaDetailProjectorRegistry = providerQuotaDetailProjectorRegistry,
                        providerUiRegistry = providerUiRegistry,
                        subscriptionRefreshPolicy = subscriptionRefreshPolicy,
                        highEmphasisMetrics = uiPreferences.highEmphasisMetrics,
                        hapticConfirmation = uiPreferences.hapticConfirmation,
                        landscapeMonitorMode = landscapeMonitorMode,
                        onHighEmphasisMetricsChange = uiPreferencesRepository::setHighEmphasisMetrics,
                        onHapticConfirmationChange = uiPreferencesRepository::setHapticConfirmation,
                        onLandscapeMonitorModeChange = uiPreferencesRepository::setLandscapeMonitorMode,
                        bottomContentPadding = if (showBottomNavigation) FloatingBottomNavClearance else 0.dp,
                        addSubscriptionRequestKey = addSubscriptionRequestKey,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (showBottomNavigation) {
                        QuotaNavigationBar(
                            navController = navController,
                            items = navigationItems,
                            onActionClick = { action ->
                                when (action) {
                                    BottomNavAction.AddSubscription -> {
                                        addSubscriptionRequestKey += 1
                                        if (currentRoute != Screen.Home.route) {
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeMonitorModeEffect(enabled: Boolean) {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity, enabled) {
        if (activity == null) {
            return@DisposableEffect onDispose {}
        }

        val previousOrientation = activity.requestedOrientation
        activity.requestedOrientation = if (enabled) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        onDispose {
            activity.requestedOrientation = previousOrientation
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
