package com.example.myapp

import android.app.Application
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import com.tencent.mmkv.MMKV

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
    }
    
    private fun initThirdPartyLibs() {
        // 初始化第三方库
        // 例如：图片加载库、日志库、崩溃收集等
        XLog.init(
            LogLevel.ALL, AndroidPrinter()
        )
        MMKV.initialize(this)
    }
}