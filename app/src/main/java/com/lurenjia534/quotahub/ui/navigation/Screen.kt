package com.lurenjia534.quotahub.ui.navigation

/**
 * 定义应用中所有屏幕（页面）的路由地址
 *
 * 使用 sealed class 的优势：
 * 1. 类型安全 - 所有 Screen 实例都是预定义的，无法创建其他实例
 * 2. 编译器检查 - 当使用 when 表达式时，编译器会检查是否处理了所有情况
 * 3. 易于维护 - 新增屏幕只需添加一个新的 data object
 *
 * route 字符串用于在 NavHost 中匹配对应的 composable 屏幕
 */
sealed class Screen(val route: String) {
    // 首页 - 应用启动后的第一个屏幕
    data object Home : Screen("home")

    data object ProviderDetail : Screen("provider/{providerId}") {
        const val providerIdArg = "providerId"

        fun createRoute(providerId: String): String {
            return "provider/$providerId"
        }
    }

    // 设置页 - 用户配置和应用设置
    data object Settings : Screen("settings")
}

/**
 * 底部导航栏需要展示的屏幕列表
 *
 * 这个列表用于：
 * 1. 在 MainActivity 中判断当前路由是否应该显示底部导航栏
 * 2. 未来可用于动态配置导航项（如图标、是否显示等）
 *
 * 注意：这里只包含需要在底部导航栏显示的屏幕，
 * 如果有不需要显示的屏幕（如详情页、设置页），不要加入此列表
 */
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Settings
)
