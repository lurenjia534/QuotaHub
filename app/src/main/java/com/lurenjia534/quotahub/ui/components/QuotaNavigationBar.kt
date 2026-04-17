package com.lurenjia534.quotahub.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lurenjia534.quotahub.ui.navigation.BottomNavItem
import com.lurenjia534.quotahub.ui.navigation.bottomNavItemsData

@Composable
fun QuotaNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(0.84f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, start = 14.dp, end = 14.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(26.dp),
                color = colorScheme.surfaceContainerHighest.copy(alpha = 0.88f)
            ) {}

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(34.dp),
                color = colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.22f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    colorScheme.surfaceContainerLow.copy(alpha = 0.98f),
                                    colorScheme.surface,
                                    colorScheme.secondaryContainer.copy(alpha = 0.12f)
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomNavItemsData.forEach { item ->
                            val selected = currentDestination
                                ?.hierarchy
                                ?.any { it.route == item.route } == true

                            QuotaNavigationDestination(
                                item = item,
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier
                            )

                            if (item != bottomNavItemsData.last()) {
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotaNavigationDestination(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val destinationWidth by animateDpAsState(
        targetValue = if (selected) 176.dp else 66.dp,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.88f),
        label = "navDestinationWidth"
    )
    val selectedContainerColor by animateColorAsState(
        targetValue = if (selected) colorScheme.inverseSurface else Color.Transparent,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "navSelectedContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colorScheme.inverseOnSurface else colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "navContentColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.97f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.86f),
        label = "navDestinationScale"
    )

    Box(
        modifier = modifier
            .width(destinationWidth)
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                contentDescription = item.label
            }
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                color = selectedContainerColor,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.selectedIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor
                        )
                    )
                }
            }
        } else {
            Icon(
                imageVector = item.unselectedIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
