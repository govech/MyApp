package com.example.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.base.databinding.ActivitySettingsBinding
import com.example.constant.RouterPath
import com.example.utils.NightModeUtils
import com.example.utils.ktx.binding

@Route(path = RouterPath.Setting.BASE_SETTING)
class SettingsActivity : BaseActivity() {

    private val binding by binding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnDay.setOnClickListener{
            NightModeUtils.setNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        binding.btnNight.setOnClickListener{
            NightModeUtils.setNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        
        binding.btnSystem.setOnClickListener{
            NightModeUtils.setNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
} 