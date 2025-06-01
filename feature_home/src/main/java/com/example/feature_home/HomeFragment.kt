package com.example.feature_home

import com.example.base.BaseLazyFragment
import com.example.feature_home.databinding.FragmentHomeBinding


class HomeFragment : BaseLazyFragment<FragmentHomeBinding>() {

    override fun onLazyLoad() {
        binding.textView.text = "首页"
    }

    override fun onBindingCreated() {

    }






}