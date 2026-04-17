package com.lurenjia534.quotahub.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
private val NavigationIndicatorShape = RoundedCornerShape(26.dp)
private val NavigationShadowShape = RoundedCornerShape(24.dp)
private val NavigationTrayHeight = 76.dp
private val NavigationItemHeight = 56.dp
private val NavigationItemGap = 8.dp
private val NavigationItemHorizontalPadding = 10.dp

@Composable
fun QuotaNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentIndex = bottomNavItemsData.indexOfFirst { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }.coerceAtLeast(0)
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .offset(y = 14.dp)
                    .height(28.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = NavigationShadowShape
                    )
                    .background(
                        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
                        shape = NavigationShadowShape
                    )
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NavigationTrayHeight),
                shape = NavigationTrayShape,
                color = colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 4.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.22f)
                )
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val itemCount = bottomNavItemsData.size.coerceAtLeast(1)
                    val slotWidth = (
                        maxWidth -
                            (NavigationItemHorizontalPadding * 2) -
                            (NavigationItemGap * (itemCount - 1))
                        ) / itemCount
                    val indicatorOffset by animateDpAsState(
                        targetValue = NavigationItemHorizontalPadding +
                            ((slotWidth + NavigationItemGap) * currentIndex),
                        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
                        label = "navigationIndicatorOffset"
                    )

                    Surface(
                        modifier = Modifier
                            .offset(x = indicatorOffset, y = 10.dp)
                            .width(slotWidth)
                            .height(NavigationItemHeight),
                        shape = NavigationIndicatorShape,
                        color = colorScheme.inverseSurface,
                        shadowElevation = 5.dp
                    ) {}

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = NavigationItemHorizontalPadding,
                                vertical = 10.dp
                            )
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(NavigationItemGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomNavItemsData.forEach { item ->
                            val selected = currentDestination
                                ?.hierarchy
                                ?.any { it.route == item.route } == true

                            QuotaNavigationDestination(
                                item = item,
                                selected = selected,
                                showExpandedLabel = slotWidth >= 104.dp,
                                slotWidth = slotWidth,
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
}

@Composable
private fun QuotaNavigationDestination(
    item: BottomNavItem,
    selected: Boolean,
    showExpandedLabel: Boolean,
    slotWidth: Dp,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            colorScheme.inverseOnSurface
        } else {
            colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "navigationContentColor"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.88f),
        label = "navigationContentScale"
    )
    val verticalLift by animateFloatAsState(
        targetValue = if (selected) -1.2f else 0f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.9f),
        label = "navigationVerticalLift"
    )

    Box(
        modifier = Modifier
            .width(slotWidth)
            .height(NavigationItemHeight)
            .graphicsLayer {
                scaleX = contentScale
                scaleY = contentScale
                translationY = verticalLift
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (selected) 22.dp else 24.dp)
            )

            AnimatedVisibility(
                visible = selected && showExpandedLabel,
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
                Row(
                    modifier = Modifier.padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
