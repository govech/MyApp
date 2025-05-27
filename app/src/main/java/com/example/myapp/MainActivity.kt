package com.example.myapp

import android.os.Bundle
import com.alibaba.android.arouter.launcher.ARouter
import com.example.base.BaseActivity
import com.example.constant.RouterPath

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 直接跳转到登录页面
        ARouter.getInstance()
            .build(RouterPath.Login.LOGIN_ACTIVITY)
            .navigation()
        finish()
    }
}