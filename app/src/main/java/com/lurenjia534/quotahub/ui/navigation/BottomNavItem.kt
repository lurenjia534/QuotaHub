package com.lurenjia534.quotahub.ui.navigation

/**
 * 导入 Material Design 图标库
 *
 * Icons.Filled 和 Icons.Outlined 的区别：
 * - Filled（实心）：用于表示当前选中的状态，视觉上更突出
 * - Outlined（描边）：用于表示未选中状态，视觉上更轻量
 *
 * Material Icons 包含 5000+ 图标，material-icons-extended 包提供了全部图标
 */
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person

/**
 * ImageVector 是 Compose 中用于表示图标的数据类型
 * 由 androidx.compose.ui.graphics.vector 提供
 */
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航栏每一项的数据模型
 *
 * 为什么使用 data class 而不是普通 class？
 * - data class 自动生成 equals()、hashCode()、toString()、copy() 方法
 * - 更适合用于数据存储和传递
 *
 * @param label 导航项的文本标签，显示在图标下方
 * @param selectedIcon 选中状态显示的图标（实心）
 * @param unselectedIcon 未选中状态显示的图标（描边）
 * @param route 导航目标屏幕的路由，与 Screen.route 对应
 */
data class BottomNavItem(
    val label: String,              // 显示的文本，如 "Home"
    val selectedIcon: ImageVector, // 选中时的图标（实心样式）
    val unselectedIcon: ImageVector, // 未选中时的图标（描边样式）
    val route: String              // 导航目标路由，与 Screen.route 对应
)

/**
 * 底部导航栏的完整数据列表
 *
 * 这里定义了三项导航：Home、Explore、Profile
 * 每个导航项都配置了：
 * - label: 用户可见的文本
 * - selectedIcon: 选中时显示实心图标
 * - unselectedIcon: 未选中时显示描边图标
 * - route: 对应的路由地址
 *
 * 为什么要区分 Filled 和 Outlined？
 * - 这是 Material Design 3 的设计规范
 * - 选中状态用实心图标表示"这是你当前所在的位置"
 * - 未选中状态用描边图标表示"这里还有其他可导航的位置"
 */
val bottomNavItemsData = listOf(
    BottomNavItem(
        label = "Home",
        selectedIcon = Icons.Filled.Home,      // 实心 Home 图标
        unselectedIcon = Icons.Outlined.Home,   // 描边 Home 图标
        route = Screen.Home.route               // 路由地址 "home"
    ),
    BottomNavItem(
        label = "Explore",
        selectedIcon = Icons.Filled.Explore,    // 实心 Explore 图标
        unselectedIcon = Icons.Outlined.Explore, // 描边 Explore 图标
        route = Screen.Explore.route            // 路由地址 "explore"
    ),
    BottomNavItem(
        label = "Profile",
        selectedIcon = Icons.Filled.Person,     // 实心 Person 图标
        unselectedIcon = Icons.Outlined.Person,  // 描边 Person 图标
        route = Screen.Profile.route            // 路由地址 "profile"
    )
)
