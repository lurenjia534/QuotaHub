package com.lurenjia534.quotahub.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lurenjia534.quotahub.ui.navigation.BottomNavItem
import com.lurenjia534.quotahub.ui.navigation.bottomNavItemsData

private val NavigationTrayShape = RoundedCornerShape(32.dp)
private val NavigationShadowShape = RoundedCornerShape(24.dp)
private val NavigationActiveShape = RoundedCornerShape(26.dp)
private val TrayMinWidth = 252.dp
private val TrayMaxWidth = 332.dp
private val TrayHorizontalPadding = 12.dp
private val TrayVerticalPadding = 10.dp
private val TrayItemGap = 8.dp
private val NavigationItemHeight = 56.dp
private val CompactItemWidth = 56.dp

@Composable
fun QuotaNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier.widthIn(min = TrayMinWidth, max = TrayMaxWidth),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = 18.dp, start = 14.dp, end = 14.dp)
                    .height(26.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = NavigationShadowShape
                    )
                    .background(
                        color = Color.Transparent,
                        shape = NavigationShadowShape
                    )
            )

            Surface(
                shape = NavigationTrayShape,
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(
                            horizontal = TrayHorizontalPadding,
                            vertical = TrayVerticalPadding
                        )
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(TrayItemGap),
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
                            }
                        )
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
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val itemWidth by animateDpAsState(
        targetValue = if (selected) selectedWidthFor(item) else CompactItemWidth,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.88f),
        label = "navigationItemWidth"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            colorScheme.inverseOnSurface
        } else {
            colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "navigationContentColor"
    )
    val outlineColor by animateColorAsState(
        targetValue = if (selected) {
            Color.Transparent
        } else {
            colorScheme.outlineVariant.copy(alpha = 0.16f)
        },
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.92f),
        label = "navigationOutlineColor"
    )
    val inactiveWash by animateColorAsState(
        targetValue = if (selected) {
            Color.Transparent
        } else {
            colorScheme.surfaceContainerHighest.copy(alpha = 0.16f)
        },
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.92f),
        label = "navigationInactiveWash"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.86f),
        label = "navigationContentScale"
    )

    Box(
        modifier = Modifier
            .width(itemWidth)
            .height(NavigationItemHeight)
            .clip(NavigationActiveShape)
            .background(inactiveWash)
            .border(width = 1.dp, color = outlineColor, shape = NavigationActiveShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            )
            .semantics {
                contentDescription = item.label
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Surface(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(
                        elevation = 4.dp,
                        shape = NavigationActiveShape,
                        clip = false
                    ),
                shape = NavigationActiveShape,
                color = colorScheme.inverseSurface
            ) {}
        }

        Row(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (selected) 22.dp else 24.dp)
            )

            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(
                    animationSpec = spring(stiffness = 450f, dampingRatio = 0.9f)
                ) + expandHorizontally(
                    animationSpec = spring(stiffness = 450f, dampingRatio = 0.9f),
                    expandFrom = Alignment.Start
                ),
                exit = fadeOut(
                    animationSpec = spring(stiffness = 500f, dampingRatio = 0.95f)
                ) + shrinkHorizontally(
                    animationSpec = spring(stiffness = 500f, dampingRatio = 0.95f),
                    shrinkTowards = Alignment.Start
                )
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    ),
                    maxLines = 1,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}

private fun selectedWidthFor(item: BottomNavItem): Dp {
    return when {
        item.label.length >= 8 -> 158.dp
        item.label.length >= 6 -> 148.dp
        else -> 140.dp
    }
}
