package com.example.feature_user

import com.example.base.BaseLazyFragment
import com.example.feature_user.databinding.FragmentMineBinding


class MineFragment : BaseLazyFragment<FragmentMineBinding>() {

    override fun onLazyLoad() {
        binding.textView.text = "我的"
    }

    override fun onBindingCreated() {

    }






}