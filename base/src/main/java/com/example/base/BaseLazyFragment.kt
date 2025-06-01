package com.example.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.lib_tab_navigation.core.TabFragmentNavigator
/**
 * 懒加载 Fragment 的抽象基类
 * 实现了 TabFragmentNavigator.LazyLoadable 接口，用于在 Fragment 可见时进行懒加载
 */
abstract class BaseLazyFragment : Fragment(), TabFragmentNavigator.LazyLoadable {

    // 标记 Fragment 是否已加载
    private var hasLoaded = false

    /**
     * 当 Fragment 视图创建时调用
     * 初始化视图并尝试进行懒加载
     *
     * @param view 视图对象
     * @param savedInstanceState 保存的实例状态
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tryLazyLoad()
    }
    /**
     * 当 Fragment 对用户可见时调用
     * 尝试进行懒加载
     */
    override fun onVisibleToUser() {
        tryLazyLoad()
    }
    /**
     * 尝试进行懒加载
     * 如果 Fragment 未加载且视图存在，则标记为已加载，并调用 onLazyLoad 方法进行懒加载
     */
    private fun tryLazyLoad() {
        if (!hasLoaded && view != null) {
            hasLoaded = true
            onLazyLoad()
        }
    }

    /** 懒加载触发时执行的逻辑（由子类实现） */
    abstract fun onLazyLoad()
}
