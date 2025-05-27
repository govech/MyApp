package com.example.myapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.launcher.ARouter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 直接跳转到登录页面
        ARouter.getInstance()
            .build("/app/login")
            .navigation()
        finish()
    }
}