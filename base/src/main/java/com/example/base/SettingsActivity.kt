package com.example.base

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.constant.RouterPath
import com.example.utils.NightModeUtils
@Route(path = RouterPath.Setting.BASE_SETTING)
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.btnDay).setOnClickListener {
            NightModeUtils.setNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        findViewById<Button>(R.id.btnNight).setOnClickListener {
            NightModeUtils.setNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        findViewById<Button>(R.id.btnSystem).setOnClickListener {
            NightModeUtils.setNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
} 