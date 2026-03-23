package com.lurenjia534.quotahub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lurenjia534.quotahub.ui.components.QuotaNavigationBar
import com.lurenjia534.quotahub.ui.navigation.QuotaNavHost
import com.lurenjia534.quotahub.ui.navigation.bottomNavItems
import com.lurenjia534.quotahub.ui.theme.QuotaHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val application = application as QuotaApplication
        val providerRegistry = application.providerRegistry
        setContent {
            QuotaHubTheme {
                QuotaApp(providerRegistry = providerRegistry)
            }
        }
    }
}

@Composable
fun QuotaApp(providerRegistry: com.lurenjia534.quotahub.data.provider.QuotaProviderRegistry) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute in bottomNavItems.map { it.route }) {
                QuotaNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        QuotaNavHost(
            navController = navController,
            providerRegistry = providerRegistry,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
