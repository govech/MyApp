package com.example.utils

import androidx.appcompat.app.AppCompatDelegate
import com.tencent.mmkv.MMKV

object NightModeUtils {
    private const val KEY_NIGHT_MODE = "night_mode"

    fun setNightMode(mode: Int) {
        MMKV.defaultMMKV().putInt(KEY_NIGHT_MODE, mode)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getNightMode(): Int {
        return MMKV.defaultMMKV().getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun applySavedNightMode() {
        AppCompatDelegate.setDefaultNightMode(getNightMode())
    }
} 