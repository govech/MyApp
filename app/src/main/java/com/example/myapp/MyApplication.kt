package com.example.myapp

import android.app.Application
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import com.tencent.mmkv.MMKV
import com.alibaba.android.arouter.launcher.ARouter
import com.example.image.ImageLoader
import com.example.image.impl.CoilImageLoader
import com.example.utils.NightModeUtils

class MyApplication : Application() {
    companion object {
        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化网络库

        // 初始化其他全局配置
        initThirdPartyLibs()

        // 初始化 ARouter
        if (BuildConfig.DEBUG) {           // 这两行必须写在init之前，否则这些配置在init过程中将无效
            ARouter.openLog()     // 打印日志
            ARouter.openDebug()   // 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
        }
        ARouter.init(this) // 尽可能早，推荐在Application中初始化
    }
    
    private fun initThirdPartyLibs() {
        // 初始化第三方库
        // 例如：图片加载库、日志库、崩溃收集等
        ImageLoader.init(CoilImageLoader())
        XLog.init(
            LogLevel.ALL, AndroidPrinter()
        )
        MMKV.initialize(this)
        // 设置暗黑模式或者白天模式
        NightModeUtils.applySavedNightMode()
    }
}