package com.lurenjia534.quotahub.ui.screens.home

/**
 * Box 是一个基础的布局组件，类似于传统 ViewSystem 中的 FrameLayout
 * 它的作用是将子元素堆叠在一起（后声明的在上层）
 * 在这里我们用它来居中显示 Text 组件
 */
import androidx.compose.foundation.layout.Box

/**
 * fillMaxSize() 是一个 Modifier 扩展函数
 * 作用是让组件填充父容器的全部尺寸
 * 相当于 Match_Parent（宽度和高度都100%）
 */
import androidx.compose.foundation.layout.fillMaxSize

/**
 * Text 是 Material 3 的文本组件，用于显示文字内容
 * 类似于传统 ViewSystem 中的 TextView
 */
import androidx.compose.material3.Text

/**
 * Composable 注解标记这是一个 Compose UI 函数
 * 只有 @Composable 函数才能调用其他 Compose 组件
 */
import androidx.compose.runtime.Composable

/**
 * Alignment 是 Compose 提供的对齐方式工具类
 * Alignment.Center 表示居中对齐
 * 在 Box 中可以指定内容的对齐方式
 */
import androidx.compose.ui.Alignment

/**
 * Modifier 用于修改组件的布局参数和样式
 * 所有 Compose 组件都可以接收 Modifier 参数
 */
import androidx.compose.ui.Modifier

/**
 * 首页屏幕组件
 *
 * 功能说明：
 * - 这是用户点击底部导航栏 "Home" 后看到的第一个屏幕
 * - 目前只是一个占位页面，显示 "Home Screen" 文字
 *
 * 设计考量：
 * - 使用 Box + Alignment.Center 实现内容居中
 * - 使用 fillMaxSize() 让 Box 填满父容器
 *
 * @param modifier 外部传入的布局修饰符，用于调整组件的布局和样式
 *                  这个修饰符会被传递给父容器 Box
 *
 * 使用方式：
 * 在 QuotaNavHost 中作为 composable 的 content 被调用：
 * composable(route = Screen.Home.route) {
 *     HomeScreen()
 * }
 *
 * 或者带有修饰符：
 * HomeScreen(modifier = Modifier.padding(16.dp))
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier  // 默认修饰符为空，允许外部传入自定义修饰符
) {
    /**
     * Box 布局组件
     * - contentAlignment = Alignment.Center: 让内部内容居中显示
     * - modifier = modifier.fillMaxSize(): 填满父容器全部空间
     *
     * 相当于 Android ViewSystem 中的：
     * <FrameLayout>
     *     <TextView
     *         android:layout_gravity="center"
     *         android:layout_width="match_parent"
     *         android:layout_height="match_parent" />
     * </FrameLayout>
     */
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        /**
         * Text 组件显示文本内容
         * - text = "Home Screen": 要显示的文字
         *
         * 注意：这里没有传递 modifier 参数给 Text
         * 因为 Text 会自动使用 Box 提供的居中对齐
         */
        Text(text = "Home Screen")
    }
}
