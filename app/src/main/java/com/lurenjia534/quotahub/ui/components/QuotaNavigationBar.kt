package com.lurenjia534.quotahub.ui.components

/**
 * Icon 组件用于在导航栏中显示图标
 * 来自 androidx.compose.material3 包
 */
import androidx.compose.material3.Icon

/**
 * NavigationBar 是 Material 3 的底部导航栏组件
 * 替代了旧版本的 BottomNavigation
 *
 * Material 3 设计规范：
 * - 底部导航栏通常包含 3-5 个导航项
 * - 每个导航项包含图标和可选的标签文本
 * - 高度为 80dp，图标尺寸为 24dp
 */
import androidx.compose.material3.NavigationBar

/**
 * NavigationBarItem 是底部导航栏中的单个导航项
 * 包含图标、标签和处理点击事件
 */
import androidx.compose.material3.NavigationBarItem

/**
 * NavigationBarItemDefaults 用于自定义导航项的颜色
 * 可以设置选中/未选中状态下的图标颜色、文字颜色和指示器颜色
 */
import androidx.compose.material3.NavigationBarItemDefaults

/**
 * MaterialTheme 提供对 Material Design 颜色系统的访问
 * 使用 colorScheme 可以获取当前主题下的各种颜色
 */
import androidx.compose.material3.MaterialTheme

/**
 * Text 组件用于显示导航项的标签文字
 */
import androidx.compose.material3.Text

/**
 * Composable 注解表示这是一个 Compose UI 函数
 * 只有标记为 @Composable 的函数才能使用其他 Compose 组件
 */
import androidx.compose.runtime.Composable

/**
 * byValue 是 Kotlin 的属性委托语法
 * 用于简化 State 的读取（不需要 .value）
 */
import androidx.compose.runtime.getValue

/**
 * Modifier 用于调整组件的布局和样式
 * 如 padding、fillMaxSize、background 等
 */
import androidx.compose.ui.Modifier

/**
 * NavController 是导航系统的核心类
 * 负责管理导航栈和触发导航操作
 */
import androidx.navigation.NavController

/**
 * currentBackStackEntryAsState() 是一个扩展函数
 * 返回一个 State 对象，其值是当前的 NavBackStackEntry
 * 用于观察导航状态的变化
 */
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * 导入导航项数据，提供图标、标签和路由信息
 */
import com.lurenjia534.quotahub.ui.navigation.bottomNavItemsData

/**
 * 应用底部导航栏组件
 *
 * 功能说明：
 * 1. 显示 3 个导航项：Home、Explore、Profile
 * 2. 根据当前路由高亮对应的导航项
 * 3. 处理导航项的点击事件，执行页面切换
 *
 * 设计考量：
 * - 使用 Material 3 的 NavigationBar 组件
 * - 图标在选中/未选中状态下使用不同的样式（Filled vs Outlined）
 * - 选中项有背景色指示器（indicator），符合 Material 3 规范
 *
 * @param navController 导航控制器，用于执行 navigate 操作
 * @param modifier 可选的布局修饰符
 */
@Composable
fun QuotaNavigationBar(
    navController: NavController, // 导航控制器，用于执行页面切换
    modifier: Modifier = Modifier   // 可选的布局修饰符
) {
    /**
     * 获取当前导航栈的条目
     *
     * currentBackStackEntryAsState() 返回一个 State<NavBackStackEntry?>
     * 当导航栈变化时，这个 State 会自动触发重组（recomposition）
     * 使用 by getValue 语法可以直接读取其值
     */
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    /**
     * 从当前导航条目中获取当前路由的字符串
     *
     * ?.destination?.route 的解释：
     * - navBackStackEntry: 可能为 null（尚未导航到任何页面）
     * - ?. (安全调用): 如果 navBackStackEntry 为 null，整个表达式返回 null
     * - .destination: NavBackStackEntry 的属性，表示当前目的地
     * - .route: 目的地的路由字符串
     */
    val currentRoute = navBackStackEntry?.destination?.route

    /**
     * Material 3 的 NavigationBar 组件
     *
     * containerColor: 导航栏的背景色
     * contentColor: 导航栏内容（图标、文字）的默认颜色
     *
     * 使用 MaterialTheme.colorScheme 可以自动适配深色/浅色主题
     */
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,      // 表面色（自动适配主题）
        contentColor = MaterialTheme.colorScheme.onSurface       // 内容色
    ) {
        /**
         * forEach 遍历所有导航项数据，为每个导航项创建一个 NavigationBarItem
         */
        bottomNavItemsData.forEach { item ->
            /**
             * 判断当前导航项是否被选中
             * selected 为 true 时显示实心图标和指示器
             */
            val selected = currentRoute == item.route

            /**
             * NavigationBarItem 是单个导航项
             *
             * 主要参数：
             * - selected: 是否选中，影响图标样式和指示器显示
             * - onClick: 点击事件处理
             * - icon: 显示的图标
             * - label: 显示的文本标签
             * - colors: 颜色配置
             */
            NavigationBarItem(
                selected = selected, // 当前项是否被选中

                /**
                 * onClick 处理导航逻辑：
                 * 1. 首先判断目标路由是否与当前路由不同（避免重复导航）
                 * 2. 调用 navController.navigate() 执行导航
                 *
                 * navigate 的参数解释：
                 * - item.route: 目标路由地址
                 * - navOptions: 导航选项配置
                 */
                onClick = {
                    // 只有目标路由与当前路由不同时才执行导航
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            /**
                             * popUpTo 的作用：
                             * 将导航栈中指定页面之前的所有页面弹出
                             * navController.graph.startDestinationId 是 NavHost 的起始页面
                             *
                             * saveState = true 表示在弹出前保存这些页面的状态
                             * 这样当用户按返回键或恢复导航时，状态可以被恢复
                             */
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }

                            /**
                             * launchSingleTop = true 的作用：
                             * 如果目标页面已在栈顶（已显示），不会创建新的实例
                             * 防止用户多次点击同一导航项导致创建多个相同的页面实例
                             */
                            launchSingleTop = true

                            /**
                             * restoreState = true 的作用：
                             * 如果之前保存过目标页面的状态，恢复该状态
                             * 与 saveState = true 配合使用
                             *
                             * 注意：如果 popUpTo 中的页面被清除了，restoreState 可能无效
                             */
                            restoreState = true
                        }
                    }
                },

                /**
                 * icon 参数定义导航项显示的图标
                 * 根据 selected 状态切换实心/描边图标
                 */
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label // 无障碍访问：屏幕阅读器会读取此描述
                    )
                },

                /**
                 * label 定义导航项下方的文本标签
                 * Compose 会自动处理文字过长时的省略
                 */
                label = { Text(text = item.label) },

                /**
                 * colors 参数自定义导航项的颜色
                 * 使用 NavigationBarItemDefaults.colors() 配置各种状态的颜色
                 */
                colors = NavigationBarItemDefaults.colors(
                    /**
                     * selectedIconColor: 选中状态下图标颜色
                     * 使用 onSecondaryContainer（次级容器的深色）保证可读性
                     */
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,

                    /**
                     * selectedTextColor: 选中状态下文字颜色
                     * 使用 onSurface（表面的深色）作为主文字色
                     */
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,

                    /**
                     * indicatorColor: 选中项的背景指示器颜色
                     * 使用 secondaryContainer（次级容器色）
                     * 这是 Material 3 的新设计，为选中项添加圆形背景
                     */
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,

                    /**
                     * unselectedIconColor: 未选中状态下图标颜色
                     * 使用 onSurfaceVariant（表面的浅色变体）
                     */
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,

                    /**
                     * unselectedTextColor: 未选中状态下文字颜色
                     * 与图标颜色保持一致
                     */
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
