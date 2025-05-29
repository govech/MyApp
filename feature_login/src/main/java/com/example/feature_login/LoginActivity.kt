package com.example.feature_login

import android.graphics.drawable.Drawable
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.example.base.BaseActivity
import com.example.constant.RouterPath
import com.example.feature_login.databinding.ActivityLoginBinding
import com.example.image.ImageLoadCallback
import com.example.image.ImageLoader
import com.example.image.ImageOptions
import com.example.utils.ktx.binding
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar

@Route(path = RouterPath.Login.LOGIN_ACTIVITY)
class LoginActivity : BaseActivity() {
    private val mBinding by binding(ActivityLoginBinding::inflate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun initView() {
        super.initView()
        mBinding.titleBar.title = "登录"

        mBinding.titleBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar?) {
                super.onLeftClick(titleBar)
                finish()
            }
        })

        mBinding.btnLogin.setOnClickListener {
            // 这里可以添加登录逻辑
        }
        mBinding.btnSettings.setOnClickListener {
            ARouter.getInstance().build(RouterPath.Setting.BASE_SETTING).navigation()
        }


//        ImageLoader.load("https://avatars.githubusercontent.com/u/102040668?v=4", mBinding.ivLoginBg)
        ImageLoader.load(
            "https://avatars.githubusercontent.com/u/102040668?v=4",
            mBinding.ivLoginBg,
            options = ImageOptions(
                placeholder = com.example.base.R.drawable.placeholder,
                error = com.example.base.R.drawable.error,
                isCircle = true,
                cornerRadius = 16f,
                isBlur = true,
                blurRadius = 20f,
                thumbnailUrl = "缩略图url",
                allowGif = true
            ),
            callback = object : ImageLoadCallback {
                override fun onStart() { /* 加载开始 */ }
                override fun onSuccess(result: Drawable?) { /* 加载成功 */ }
                override fun onError(error: Drawable?) { /* 加载失败 */ }
            }
        )
    }
}