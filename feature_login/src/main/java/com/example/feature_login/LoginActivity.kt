package com.example.feature_login

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route

@Route(path = "/app/login")
class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        btnLogin.setOnClickListener {
            // 这里可以添加登录逻辑
        }
    }
}