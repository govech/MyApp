package com.example.myapp

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 可在此初始化全局配置
    }
}