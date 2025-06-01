package com.example.lib_tab_navigation.core


import android.os.Bundle
import android.os.Looper
import androidx.collection.SparseArrayCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.lib_tab_navigation.model.TabItem

class TabFragmentNavigator(
    private val fragmentManager: FragmentManager,
    private val containerId: Int,
    private val tabItems: List<TabItem>
) {
    private var currentIndex = -1
    private val fragments = SparseArrayCompat<Fragment>()

    var onTabChanged: ((oldIndex: Int, newIndex: Int) -> Unit)? = null

    fun switchTo(index: Int) {
        // ✅ 检查是否运行在主线程
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "TabFragmentNavigator.switchTo() must be called on the main thread."
        }

        if (index == currentIndex) return
        if (index !in tabItems.indices) return

        val transaction = fragmentManager.beginTransaction()

        // 隐藏当前 Fragment
        if (currentIndex >= 0) {
            fragments[currentIndex]?.let {
                transaction.hide(it)
                (it as? LazyLoadable)?.onInvisibleToUser()
            }
        }

        // 获取或创建目标 Fragment
        val tag = makeFragmentTag(index)
        var targetFragment = fragmentManager.findFragmentByTag(tag)

        if (targetFragment == null) {
            targetFragment = createFragment(index)
            transaction.add(containerId, targetFragment, tag)
        } else if (!targetFragment.isAdded) {
            transaction.add(containerId, targetFragment, tag)
        } else {
            transaction.show(targetFragment)
        }

        fragments.put(index, targetFragment)
        transaction.commitNowAllowingStateLoss()

        val previousIndex = currentIndex
        currentIndex = index

        onTabChanged?.invoke(previousIndex, index)

        // 懒加载支持
        (targetFragment as? LazyLoadable)?.onVisibleToUser()
    }

    private fun createFragment(index: Int): Fragment {
        val clazz = tabItems[index].fragmentClass
        val classLoader = requireNotNull(clazz.java.classLoader) {
            "ClassLoader for fragment ${clazz.simpleName} is null"
        }
        return fragmentManager.fragmentFactory.instantiate(classLoader, clazz.java.name)
    }

    fun saveState(outState: Bundle) {
        outState.putInt(KEY_CURRENT_INDEX, currentIndex)
    }

    fun restoreState(savedInstanceState: Bundle) {
        val restoredIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX, 0)
        switchTo(restoredIndex)
    }

    private fun makeFragmentTag(index: Int): String = "tab_fragment_$index"

    interface LazyLoadable {
        fun onVisibleToUser()
        fun onInvisibleToUser() {}
    }

    companion object {
        private const val KEY_CURRENT_INDEX = "tab_current_index"
    }
}
