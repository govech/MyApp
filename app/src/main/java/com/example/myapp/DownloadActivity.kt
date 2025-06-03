package com.example.myapp

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.base.BaseActivity
import com.example.constant.RouterPath
import com.example.download.DownloadHelper
import com.example.myapp.databinding.ActivityDownloadBinding
import com.example.utils.ktx.binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Route(path = RouterPath.App.APP_DOWNLOAD)
class DownloadActivity : BaseActivity() {

    private val mBinding by binding(ActivityDownloadBinding::inflate)
    private lateinit var downloadManager: DownloadManager
    private var downloadId: Long = 0


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//
//
//        mBinding.btnDownload.setOnClickListener {
//            startDownload()
//        }


        val downloadHelper = DownloadHelper(this)

        // 监听进度更新
        lifecycleScope.launch {
            downloadHelper.progressFlow.collectLatest { status ->
                when (status) {
                    DownloadHelper.DownloadStatus.Idle -> {
                        mBinding.btnDownload.text = "点击下载"
//                        mBinding.progressBar.visibility = ProgressBar.INVISIBLE
                    }

                    is DownloadHelper.DownloadStatus.Completed -> {
                        mBinding.tvToast.text = "下载完成"
                        mBinding.progressBar.progress = 100
                    }

                    is DownloadHelper.DownloadStatus.Connecting -> {
                        mBinding.tvToast.text = "下载中..."
                        mBinding.progressBar.isIndeterminate = true
                    }

                    is DownloadHelper.DownloadStatus.Failed -> {
                        mBinding.tvToast.text = "${status.errorCode} ${status.message}"
                        mBinding.btnDownload.text = "点击下载"
                    }

                    is DownloadHelper.DownloadStatus.Progress -> {
                        mBinding.btnDownload.text = "下载中"
                        mBinding.progressBar.progress = status.percentage
                    }

                    is DownloadHelper.DownloadStatus.UnknownSizeProgress -> {
                        mBinding.progressBar.visibility = ProgressBar.VISIBLE
                        val downloadedMB = status.downloadedBytes / (1024.0 * 1024.0)
                        val formatter = DecimalFormat("#.##")
                        mBinding.tvToast.text =
                            "已下载 ${formatter.format(downloadedMB)} MB (大小未知)"
                        mBinding.progressBar.isIndeterminate = true // 显示不定进度条
                    }
                }


            }

        }

        mBinding.btnDownload.setOnClickListener {
//            downloadHelper.download {
//                url = "https://speed.cloudflare.com/__down?during=download&bytes=104857600"
//                fileName = "file.zip"
//            }

            downloadHelper.download {
                url = "https://speed.cloudflare.com/__down?during=download&bytes=104857600"
//                url = "https://speed.cloudfl"
                title = "文件下载"
                description = "正在下载 file.zip"
                fileName = "file.zip"
                mimeType = "application/zip"
            }.onComplete { uri ->
                runOnUiThread {

                    Toast.makeText(this@DownloadActivity, "下载完成: $uri", Toast.LENGTH_SHORT)
                        .show()
                }
            }.onFailed { i, message ->
                runOnUiThread {
                    Toast.makeText(this@DownloadActivity, "下载失败: $message", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        mBinding.btnCancle.setOnClickListener {
            downloadHelper.cancel()
        }

    }

    private fun startDownload() {
        val request =
            DownloadManager.Request(Uri.parse("https://speed.cloudflare.com/__down?during=download&bytes=104857600"))
                .apply {
                    // 设置保存路径 (Android 10+ 推荐)
                    setDestinationInExternalFilesDir(
                        this@DownloadActivity,
                        Environment.DIRECTORY_DOWNLOADS,
                        "file.zip"
                    )

                    // 配置选项
                    setTitle("文件下载")
                    setDescription("正在下载重要文件")
                    setAllowedOverMetered(true)     // 允许移动网络下载
                    setAllowedOverRoaming(false)    // 禁止漫游时下载
                    setMimeType("application/zip")  // 设置文件类型
                    setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                }
        downloadId = downloadManager.enqueue(request) // 返回唯一任务ID
    }


}