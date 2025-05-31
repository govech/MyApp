package com.example.lib_tab_navigation.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.lib_tab_navigation.R
import com.example.lib_tab_navigation.model.TabItem

class BottomTabLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var onTabSelected: ((Int) -> Unit)? = null
    private lateinit var tabs: List<TabItem>

    init {
        gravity = Gravity.CENTER_VERTICAL
    }
    fun setupTabs(tabItems: List<TabItem>) {
        this.tabs = tabItems
        orientation = HORIZONTAL
        removeAllViews()

        tabItems.forEachIndexed { index, item ->
            val tabView = LayoutInflater.from(context).inflate(R.layout.item_bottom_tab, this, false)
            val icon = tabView.findViewById<ImageView>(R.id.tabIcon)
            val title = tabView.findViewById<TextView>(R.id.tabTitle)

            icon.setImageResource(item.iconResId)
            title.text = item.title

            tabView.setOnClickListener {
                updateSelection(index)
                onTabSelected?.invoke(index)
            }

            addView(tabView)
        }

        updateSelection(0)
    }

    fun updateSelection(index: Int) {
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            val icon = view.findViewById<ImageView>(R.id.tabIcon)
            val title = view.findViewById<TextView>(R.id.tabTitle)
            val item = tabs[i]
            if (i == index) {
                icon.setImageResource(item.selectedIconResId)
                title.setTextColor(ContextCompat.getColor(context, R.color.colorTabSelected))
                icon.scaleX = 1.1f
                icon.scaleY = 1.1f
            } else {
                icon.setImageResource(item.iconResId)
                title.setTextColor(ContextCompat.getColor(context, R.color.colorTabNormal))
                icon.scaleX = 1.0f
                icon.scaleY = 1.0f
            }
        }
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        this.onTabSelected = listener
    }
}