package com.lurenjia534.quotahub

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.data.preferences.UiPreferencesRepository
import com.lurenjia534.quotahub.data.update.AvailableUpdate
import com.lurenjia534.quotahub.data.update.UpdateChecker
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
    var availableUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }

    LandscapeMonitorModeEffect(enabled = landscapeMonitorMode)
    UpdateCheckEffect(
        currentVersionName = LocalContext.current.versionNameOrFallback(),
        dismissedUpdateTag = uiPreferences.dismissedUpdateTag,
        onUpdateAvailable = { update ->
            availableUpdate = update
        }
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { innerPadding ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val landscapeHub = currentRoute == Screen.Home.route && maxWidth > maxHeight
                val showFloatingNavigation = showBottomNavigation && !landscapeHub
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
                        bottomContentPadding = if (showFloatingNavigation) FloatingBottomNavClearance else 0.dp,
                        addSubscriptionRequestKey = addSubscriptionRequestKey,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (showFloatingNavigation) {
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
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(1f)
                        )
                    }

                }
            }
        }
    }

    availableUpdate?.let { update ->
        UpdateAvailableDialog(
            update = update,
            onOpenRelease = {
                uiPreferencesRepository.setDismissedUpdateTag(update.tagName)
                availableUpdate = null
            },
            onDismiss = {
                uiPreferencesRepository.setDismissedUpdateTag(update.tagName)
                availableUpdate = null
            }
        )
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

@Composable
private fun UpdateCheckEffect(
    currentVersionName: String,
    dismissedUpdateTag: String?,
    onUpdateAvailable: (AvailableUpdate) -> Unit
) {
    LaunchedEffect(currentVersionName, dismissedUpdateTag) {
        val update = UpdateChecker().checkForUpdate(currentVersionName).getOrNull()
        if (update != null && update.tagName != dismissedUpdateTag) {
            onUpdateAvailable(update)
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    update: AvailableUpdate,
    onOpenRelease: () -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(
            topStart = 30.dp,
            topEnd = 22.dp,
            bottomStart = 22.dp,
            bottomEnd = 34.dp
        ),
        title = {
            Column {
                Surface(
                    color = colorScheme.primaryContainer.copy(alpha = 0.72f),
                    contentColor = colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 20.dp
                    ),
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = update.tagName,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
                Text(
                    text = "Update available",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = update.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    color = colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 28.dp,
                        bottomStart = 28.dp,
                        bottomEnd = 20.dp
                    )
                ) {
                    Text(
                        text = update.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOpenRelease()
                    uriHandler.openUri(update.releaseUrl)
                }
            ) {
                Text("Open GitHub")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

@Suppress("DEPRECATION")
private fun Context.versionNameOrFallback(): String {
    return runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull().orEmpty().ifBlank {
        "0"
    }
}
