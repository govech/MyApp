package com.example.lib_tab_navigation.model

import androidx.fragment.app.Fragment
import kotlin.reflect.KClass

data class TabItem(
    val title: String,
    val iconResId: Int,
    val selectedIconResId: Int,
    val fragmentClass: KClass<out Fragment>
)