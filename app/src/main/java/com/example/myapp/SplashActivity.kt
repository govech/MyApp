package com.example.myapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.launcher.ARouter
import com.example.constant.RouterPath
import com.example.utils.ktx.startActivityKt

class SplashActivity : AppCompatActivity() {
    
    private val SPLASH_DELAY = 150L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 使用启动页主题，无需设置内容视图
        
        // 延迟跳转到登录页面
        Handler(Looper.getMainLooper()).postDelayed({
//            startActivityKt<MainActivity>()


            // 直接跳转到登录页面
            ARouter.getInstance()
                .build(RouterPath.Login.LOGIN_ACTIVITY)
                .navigation()
            finish()

        }, SPLASH_DELAY)
    }
}