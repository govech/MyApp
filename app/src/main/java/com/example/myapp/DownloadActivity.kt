package com.example.myapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.base.BaseActivity
import com.example.constant.RouterPath
import com.example.download.downloadmanage.DownloadHelper
import com.example.myapp.databinding.ActivityDownloadBinding
import com.example.utils.ktx.binding
import com.example.utils.ktx.startActivityKt

@Route(path = RouterPath.App.APP_DOWNLOAD)
class DownloadActivity : BaseActivity() {

    private val mBinding by binding(ActivityDownloadBinding::inflate)
    private val downloadHelper by lazy { DownloadHelper(this) }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        mBinding.btnDownload.setOnClickListener {
            downloadHelper.download {
                url = "https://speed.cloudflare.com/__down?during=download&bytes=104857600"
//                url = "https://speed.cloudfl"
                title = "文件下载"
                description = "正在下载 file.zip"
                fileName = "file.zip"
                mimeType = "application/zip"
            }.onComplete {
                Log.d("DownloadHelper", "onComplete: $it")
                runOnUiThread {
                    Toast.makeText(this@DownloadActivity, "下载完成: $it", Toast.LENGTH_SHORT)
                        .show()
                }
            }.onFailed { i, s ->
                Log.d("DownloadHelper", "onFailed: $i $s")
                runOnUiThread {
                    Toast.makeText(this@DownloadActivity, "下载失败: $s", Toast.LENGTH_SHORT)
                        .show()
                }
            }


        }

        mBinding.btnCancle.setOnClickListener {
            downloadHelper.cancel()
        }


        mBinding.button2.setOnClickListener {
            startActivityKt<DownLoadFromOkhttpActivity>()
        }

    }

}