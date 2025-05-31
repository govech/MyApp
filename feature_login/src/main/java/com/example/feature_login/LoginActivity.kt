package com.example.feature_login

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.elvishew.xlog.XLog
import com.example.base.BaseActivity
import com.example.constant.RouterPath
import com.example.feature_login.data.LoginRepository
import com.example.feature_login.databinding.ActivityLoginBinding
import com.example.feature_login.ui.LoginViewModel
import com.example.feature_login.ui.LoginViewModelFactory
import com.example.image.ImageLoadCallback
import com.example.image.ImageLoader
import com.example.image.ImageOptions
import com.example.lib_network.model.ResultWrapper
import com.example.utils.ktx.binding
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import kotlinx.coroutines.launch

@Route(path = RouterPath.Login.LOGIN_ACTIVITY)
class LoginActivity : BaseActivity() {
    private val  binding by binding(ActivityLoginBinding::inflate)
    private val viewModel: LoginViewModel by viewModels { LoginViewModelFactory(LoginRepository()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBanner()
        binding.titleBar.title = "登录"

        binding.titleBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar?) {
                super.onLeftClick(titleBar)
                finish()
            }
        })

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            viewModel.login(username, password)
        }

        binding.btnSettings.setOnClickListener {
            ARouter.getInstance().build(RouterPath.Setting.BASE_SETTING).navigation()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginResult.collect { state ->
                    when (state) {
                        ResultWrapper.Loading -> {
                        }

                        is ResultWrapper.Success -> {
                            Toast.makeText(
                                this@LoginActivity,
                                "登录成功",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is ResultWrapper.Error -> {
                            Toast.makeText(
                                this@LoginActivity,
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }


        ImageLoader.load(
            "https://avatars.githubusercontent.com/u/102040668?v=4",
            binding.ivLoginBg,
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
                override fun onStart() { /* 加载开始 */
                }

                override fun onSuccess(result: Drawable?) { /* 加载成功 */
                }

                override fun onError(error: Drawable?) { /* 加载失败 */
                }
            }
        )
    }

    override fun initView() {
        super.initView()

    }

    private fun initBanner() {

        binding.bannerView.apply {
            attachToLifecycle(lifecycle)

            setImages(
                listOf(
                    "https://avatars.githubusercontent.com/u/102040668?v=4",
                    "https://bing.ee123.net/img/?date=20250529&size=640x480&imgtype=jpg\n",
                    "https://bing.ee123.net/img/?date=20250528&size=640x480&imgtype=jpg\n",
                    "https://bing.ee123.net/img/?date=20250527&size=640x480&imgtype=jpg\n",
                    "https://bing.ee123.net/img/?date=20250526&size=640x480&imgtype=jpg\n",
                    "https://bing.ee123.net/img/?date=20250525&size=640x480&imgtype=jpg\n",
                    "https://bing.ee123.net/img/?date=20250524&size=640x480&imgtype=jpg\n",
                    "https://bing.ee123.net/img/?date=20250523&size=640x480&imgtype=jpg\n",
                    "https://bing.ee123.net/img/?date=20250522&size=640x480&imgtype=jpg\n",
                    "https://bing.ee123.net/img/?date=20250521&size=640x480&imgtype=jpg\n"
                )
            )

            setOnItemClickListener {
                 Toast.makeText(
                    this@LoginActivity,
                    "点击了第${it + 1}张图片",
                    Toast.LENGTH_SHORT
                ).show()
            }

            setOnPageChangeListener { position ->
//                Toast.makeText(
//                    this@LoginActivity,
//                    "滑动到第${position + 1}张图片",
//                    Toast.LENGTH_SHORT
//                ).show()
            }
        }
    }
}