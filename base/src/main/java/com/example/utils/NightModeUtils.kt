package com.example.utils

import androidx.appcompat.app.AppCompatDelegate
import com.tencent.mmkv.MMKV

/**
 * 夜间模式工具类，用于管理和应用应用的夜间模式设置
 */
object NightModeUtils {
    // 存储在MMKV中的夜间模式键名
    private const val KEY_NIGHT_MODE = "night_mode"

    /**
     * 设置夜间模式
     *
     * @param mode 夜间模式的值，可以是AppCompatDelegate定义的模式值
     */
    fun setNightMode(mode: Int) {
        // 将夜间模式的值存储到MMKV中
        MMKV.defaultMMKV().putInt(KEY_NIGHT_MODE, mode)
        // 应用所设置的夜间模式
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * 获取已保存的夜间模式
     *
     * @return 返回保存的夜间模式，如果没有保存过，则返回系统模式
     */
    fun getNightMode(): Int {
        // 从MMKV中获取保存的夜间模式值，如果没有，则默认为跟随系统模式
        return MMKV.defaultMMKV().getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /**
     * 应用保存的夜间模式
     */
    fun applySavedNightMode() {
        // 将保存的夜间模式应用到应用中
        AppCompatDelegate.setDefaultNightMode(getNightMode())
    }
}
