package com.example.myapp

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.base.BaseActivity
import com.example.constant.RouterPath
import com.example.feature_home.HomeFragment
import com.example.feature_user.MineFragment
import com.example.lib_tab_navigation.core.TabFragmentNavigator
import com.example.lib_tab_navigation.model.TabItem
import com.example.myapp.databinding.ActivityMainBinding
import com.example.utils.ktx.binding

@Route(path = RouterPath.App.APP_MAIN)
class MainActivity : BaseActivity() {

    private val mBinding by binding(ActivityMainBinding::inflate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val tabItems = listOf(
            TabItem(
                "首页",
                com.example.feature_home.R.drawable.ic_home,
                com.example.feature_home.R.drawable.ic_home_selected,
                HomeFragment::class
            ),
            TabItem(
                "消息",
                R.drawable.ic_message,
                R.drawable.ic_message_selected,
                MessageFragment::class
            ),
            TabItem(
                "我的",
                com.example.feature_user.R.drawable.ic_mine,
                com.example.feature_user.R.drawable.ic_mine_selected,
                MineFragment::class
            )
        )

        mBinding.bottomTabLayout.setupTabs(tabItems)


        val tabNavigator =
            TabFragmentNavigator(supportFragmentManager, R.id.fragmentContainer, tabItems)
        tabNavigator.switchTo(0) // 默认显示第一个


        mBinding.bottomTabLayout.setOnTabSelectedListener {
            // 切换tab
            tabNavigator.switchTo(it)
        }

        mBinding.bottomTabLayout.setBadge(2, 100)
        mBinding.bottomTabLayout.setBadge(1, 9)
        mBinding.bottomTabLayout.setBadge(0, 0)
    }
}