package com.example.lib_tab_navigation.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.indices
import com.example.lib_tab_navigation.R
import com.example.lib_tab_navigation.model.TabItem

class BottomTabLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private var currentSelectedIndex: Int = -1 // 当前选中的tab索引
    private var onTabSelected: ((Int) -> Unit)? = null
    private lateinit var tabs: List<TabItem>
    private val badgeViews = mutableListOf<TextView>()

    private var tabTextSize = 12f.spToPx(context)
    private var tabTextColor = context.getColor(R.color.colorTabNormal)
    private var tabSelectedTextColor = context.getColor(R.color.colorTabSelected)
    private var tabIconSize = 24.dpToPx(context)
    private var tabSelectedScale = 1.1f

    init {
        gravity = Gravity.CENTER_VERTICAL
        orientation = HORIZONTAL


        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BottomTabLayout)
        tabTextSize =
            typedArray.getDimension(R.styleable.BottomTabLayout_tabTextSize, tabTextSize)
        tabTextColor = typedArray.getColor(R.styleable.BottomTabLayout_tabTextColor, tabTextColor)
        tabSelectedTextColor =
            typedArray.getColor(
                R.styleable.BottomTabLayout_tabSelectedTextColor,
                tabSelectedTextColor
            )
        tabIconSize =
            typedArray.getDimensionPixelSize(
                R.styleable.BottomTabLayout_tabIconSize,
                tabIconSize
            )
        tabSelectedScale =
            typedArray.getFloat(R.styleable.BottomTabLayout_tabSelectedScale, tabSelectedScale)
        typedArray.recycle()
    }

    fun setupTabs(tabItems: List<TabItem>) {
        this.tabs = tabItems

        removeAllViews()
        badgeViews.clear()

        tabItems.forEachIndexed { index, item ->
            val tabView =
                LayoutInflater.from(context).inflate(R.layout.item_bottom_tab, this, false)
            val layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT)
            layoutParams.weight = 1f
            tabView.layoutParams = layoutParams


            val tabTitle = tabView.findViewById<TextView>(R.id.tabTitle)
            tabTitle.text = item.title
            tabTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabTextSize)
            tabTitle.setTextColor(tabTextColor)


            val tabIcon = tabView.findViewById<ImageView>(R.id.tabIcon)
            tabIcon.setImageResource(item.iconResId)
            tabIcon.layoutParams.width = tabIconSize
            tabIcon.layoutParams.height = tabIconSize

            val badge = tabView.findViewById<TextView>(R.id.tabBadge)
            badgeViews.add(badge)
            applyBadge(badge, item.badgeCount)


            tabView.setOnClickListener {
                updateSelection(index)
                onTabSelected?.invoke(index)
            }

            addView(tabView)
        }

        updateSelection(0)
    }

    fun updateSelection(index: Int) {
        if (index == currentSelectedIndex) return // 当前索引未改变,避免重复设置
        currentSelectedIndex = index

        if (index !in tabs.indices || index !in this.indices) return // 索引越界

        for (i in 0 until childCount) {
            val view = getChildAt(i)
            val tabIcon = view.findViewById<ImageView>(R.id.tabIcon)
            val tabTitle = view.findViewById<TextView>(R.id.tabTitle)
            val item = tabs[i]
            if (i == index) {
                tabIcon.setImageResource(item.selectedIconResId)
                tabTitle.setTextColor(tabSelectedTextColor)
                tabIcon.applyScale(true)
            } else {
                tabIcon.setImageResource(item.iconResId)
                tabIcon.applyScale(false)
                tabTitle.setTextColor(tabTextColor)
            }
        }
    }


    fun setBadge(index: Int, count: Int?) {
        if (index in tabs.indices) {
            tabs[index].badgeCount = count
            applyBadge(badgeViews[index], count)
        }
    }

    /**
     * 显示小红点
     */
    @SuppressLint("SetTextI18n")
    private fun applyBadge(badgeView: TextView, count: Int?) {
        when {
            count == null -> {
                badgeView.visibility = View.GONE
            }
            // 当计数为0时，显示徽章但不带数字，仅作为一个小圆点
            count == 0 -> {
                badgeView.visibility = View.VISIBLE
                badgeView.text = ""
                badgeView.setBackgroundResource(R.drawable.bg_badge_circle)
                badgeView.layoutParams = badgeView.layoutParams.apply {
                    width = 5.dpToPx(context)
                    height = 5.dpToPx(context)
                }
            }
            // 当计数在1到9之间时，圆形背景
            count in 1..9 -> {
                badgeView.visibility = View.VISIBLE
                badgeView.text = count.toString()
                badgeView.setBackgroundResource(R.drawable.bg_badge_circle)
            }
            // 当计数大于9时，圆角矩形背景,如果大于99则显示"99+"
            count > 9 -> {
                badgeView.visibility = View.VISIBLE
                badgeView.text = if (count > 99) "99+" else count.toString()
                badgeView.setBackgroundResource(R.drawable.bg_badge)
            }
        }
    }


    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        this.onTabSelected = listener
    }

    private fun ImageView.applyScale(selected: Boolean) {
        val scale = if (selected) tabSelectedScale else 1.0f
        animate().scaleX(scale).scaleY(scale).setDuration(150).start()
    }

    companion object {


        fun Float.spToPx(context: Context): Float {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                this,
                context.resources.displayMetrics
            )
        }

        fun Int.dpToPx(context: Context): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                this.toFloat(),
                context.resources.displayMetrics
            ).toInt()
        }
    }
}

