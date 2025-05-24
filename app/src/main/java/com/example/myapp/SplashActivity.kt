package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.feature_login.LoginActivity

class SplashActivity : AppCompatActivity() {
    
    private val SPLASH_DELAY = 150L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 使用启动页主题，无需设置内容视图
        
        // 延迟跳转到登录页面
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, SPLASH_DELAY)
    }
}