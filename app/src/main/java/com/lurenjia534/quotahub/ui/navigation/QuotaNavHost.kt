package com.lurenjia534.quotahub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * NavHostController 是导航系统的核心，负责：
 * 1. 管理导航栈（back stack）
 * 2. 执行导航操作（navigate）
 * 3. 提供当前导航状态
 */
import androidx.navigation.NavHostController

/**
 * NavHost 是导航系统的容器组件，它：
 * 1. 定义了导航图谱（哪些路由对应哪些屏幕）
 * 2. 管理导航栈的切换动画
 * 3. 协调 NavController 和各个 composable 屏幕
 */
import androidx.navigation.compose.NavHost

/**
 * composable 是用于在 NavHost 中定义单个路由的 DSL 函数
 * 每个 composable {} 块对应一个屏幕
 */
import androidx.navigation.compose.composable

/**
 * 导入各个屏幕组件
 * 这些组件是导航的目标页面
 */
import com.lurenjia534.quotahub.ui.screens.explore.ExploreScreen
import com.lurenjia534.quotahub.ui.screens.home.HomeScreen
import com.lurenjia534.quotahub.ui.screens.profile.ProfileScreen

/**
 * 应用的主导航宿主组件
 *
 * 作用：定义整个应用的导航图谱，告诉 NavHost：
 * - 路由地址 "home" 应该显示 HomeScreen
 * - 路由地址 "explore" 应该显示 ExploreScreen
 * - 路由地址 "profile" 应该显示 ProfileScreen
 *
 * @param navController 导航控制器，由调用方（通常是 MainActivity）提供
 * @param modifier 用于调整布局的修饰符，如 padding、fillMaxSize 等
 *
 * 使用方式：
 * 在 MainActivity 的 Scaffold 中调用：
 * QuotaNavHost(
 *     navController = rememberNavController(),
 *     modifier = Modifier.padding(innerPadding)
 * )
 *
 * 这样 Scaffold 的 innerPadding 会被传递给 NavHost，
 * 确保导航栏内容不会被系统栏遮挡
 */
@Composable
fun QuotaNavHost(
    navController: NavHostController, // 导航控制器，负责管理导航栈
    modifier: Modifier = Modifier       // 可选的布局修饰符
) {
    /**
     * NavHost 是 Compose 导航系统的核心组件
     *
     * @param navController 导航控制器实例
     * @param startDestination 应用启动时显示的第一个屏幕（路由地址）
     * @param modifier 布局修饰符
     */
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route, // 默认显示 Home 屏幕
        modifier = modifier
    ) {
        /**
         * composable 函数用于定义路由和对应屏幕的映射关系
         *
         * @param route 路由地址字符串，必须与 Screen 中定义的 route 一致
         * @param content 到达此路由时显示的 composable 屏幕
         */
        composable(route = Screen.Home.route) {
            // 当路由为 "home" 时显示 HomeScreen
            HomeScreen()
        }

        composable(route = Screen.Explore.route) {
            // 当路由为 "explore" 时显示 ExploreScreen
            ExploreScreen()
        }

        composable(route = Screen.Profile.route) {
            // 当路由为 "profile" 时显示 ProfileScreen
            ProfileScreen()
        }
    }
}
