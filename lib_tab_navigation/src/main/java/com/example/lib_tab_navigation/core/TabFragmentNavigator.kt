package com.example.lib_tab_navigation.core

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.lib_tab_navigation.model.TabItem

class TabFragmentNavigator(
    private val fragmentManager: FragmentManager,
    private val containerId: Int,
    private val tabItems: List<TabItem>
) {
    private var currentIndex = -1
    private val fragments = mutableListOf<Fragment?>()

    init {
        repeat(tabItems.size) { fragments.add(null) }
    }

    fun switchTo(index: Int) {
        if (index == currentIndex) return

        val transaction = fragmentManager.beginTransaction()

        if (currentIndex >= 0) {
            fragments[currentIndex]?.let { transaction.hide(it) }
        }

        var fragment = fragments[index]
        if (fragment == null) {
            fragment = tabItems[index].fragmentClass.java.newInstance()
            fragments[index] = fragment
            transaction.add(containerId, fragment)
        } else {
            transaction.show(fragment)
        }

        transaction.commitAllowingStateLoss()
        currentIndex = index
    }

    fun saveState(outState: Bundle) {
        outState.putInt("tab_index", currentIndex)
    }

    fun restoreState(savedInstanceState: Bundle) {
        val restored = savedInstanceState.getInt("tab_index", 0)
        switchTo(restored)
    }
}