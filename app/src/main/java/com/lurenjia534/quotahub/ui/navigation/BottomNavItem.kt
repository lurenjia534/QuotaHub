package com.lurenjia534.quotahub.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavAction {
    AddSubscription
}

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String? = null,
    val action: BottomNavAction? = null
)

fun bottomNavItemsData(
    showAddSubscription: Boolean
): List<BottomNavItem> {
    return buildList {
        add(
            BottomNavItem(
                label = "Home",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                route = Screen.Home.route
            )
        )
        add(
            BottomNavItem(
                label = "Settings",
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                route = Screen.Settings.route
            )
        )
        if (showAddSubscription) {
            add(
                BottomNavItem(
                    label = "Add",
                    selectedIcon = Icons.Filled.Add,
                    unselectedIcon = Icons.Filled.Add,
                    action = BottomNavAction.AddSubscription
                )
            )
        }
    }
}
