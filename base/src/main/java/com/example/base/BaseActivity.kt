package com.example.base

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.elvishew.xlog.XLog
import com.example.utils.ktx.color

/**
 * 基础Activity类，提供通用功能
 * 1. 生命周期日志记录
 * 2. 状态栏设置
 * 3. ViewBinding支持
 * 4. 通用的Toast显示方法
 */
abstract class BaseActivity : AppCompatActivity() {
    // 状态栏控制实例
    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    // 生命周期日志开关
    protected open val enableLifecycleLog = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logLifecycle("onCreate")
        initView()
        initData()
        setupObservers()
        initStatusBar()

    }

    private fun initStatusBar() {
        initWindowController()
        setNavigationBarColor(Color.TRANSPARENT)

        // 设置透明状态栏+深色文字
        setTransparentStatusBar(darkText = true)
        // 设置渐变状态栏
//        setGradientStatusBar(intArrayOf(Color.YELLOW, Color.BLUE))
        // 设置全屏模式（隐藏导航栏，滑动不显示系统栏）
        setFullScreen(hideNavigationBar = true)
        // 修改状态栏颜色
        setStatusBarColor(this.color(R.color.primary)) // 半透明红色
        // 显示系统状态栏
        showSystemBars()
    }

    /**
     * 必须实现的抽象方法 - 视图初始化
     */
     protected open fun initView(){}

    /**
     * 可选实现的扩展方法
     */
    protected open fun initData() {}          // 数据初始化
    protected open fun setupObservers() {}    // 观察者注册

    /**
     * 通用UI扩展
     */
    protected fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }







    /**
     * 初始化窗口控制器
     */
    private fun initWindowController() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    }

    /**
     * 设置状态栏颜色
     * @param color 颜色值 (支持透明度)
     */
    fun setStatusBarColor(@ColorInt color: Int) {
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = color
        }
    }

    /**
     * 设置状态栏文字颜色
     * @param isDark 是否使用深色文字
     */
    fun setStatusBarTextColor(isDark: Boolean) {
        windowInsetsController.isAppearanceLightStatusBars = isDark
    }

    /**
     * 设置导航栏颜色
     * @param color 颜色值 (支持透明度)
     */
    fun setNavigationBarColor(@ColorInt color: Int) {
        window.navigationBarColor = color
    }

    /**
     * 设置全屏模式
     * @param hideNavigationBar 是否同时隐藏导航栏
     * @param sticky 是否保持全屏（用户滑动时不显示系统栏）
     */
    fun setFullScreen(hideNavigationBar: Boolean = false, sticky: Boolean = true) {
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            if (hideNavigationBar) {
                hide(WindowInsetsCompat.Type.navigationBars())
            }
            systemBarsBehavior = if (sticky) {
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
            }
        }
    }

    /**
     * 显示系统状态栏
     */
    fun showSystemBars() {
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * 设置透明状态栏
     * @param darkText 是否使用深色文字
     */
    fun setTransparentStatusBar(darkText: Boolean = false) {
        window.apply {
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        setStatusBarTextColor(darkText)
    }

    /**
     * 设置状态栏渐变效果
     * @param colors 渐变颜色数组
     */
    fun setGradientStatusBar(colors: IntArray) {
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            setBackgroundDrawable(
                GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    colors
                )
            )
        }
    }

    /**
     * 设置状态栏可见性
     * @param visible 是否显示状态栏
     */
    fun setStatusBarVisibility(visible: Boolean) {
        if (visible) {
            windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        }
    }























    override fun onStart() {
        super.onStart()
        logLifecycle("onStart")
    }
    
    override fun onResume() {
        super.onResume()
        logLifecycle("onResume")
    }
    
    override fun onPause() {
        super.onPause()
        logLifecycle("onPause")
    }
    
    override fun onStop() {
        super.onStop()
        logLifecycle("onStop")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        logLifecycle("onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        logLifecycle("onRestart")
    }

    private fun logLifecycle(event: String) {
        if (enableLifecycleLog) {
            XLog.d("Lifecycle: ${this::class.simpleName} $event")
        }
    }



}