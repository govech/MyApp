package com.example.myapp

import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.example.base.BaseActivity
import com.example.download.DownloadConfig
import com.example.download.DownloadInfo
import com.example.download.DownloadListener
import com.example.download.DownloadManager
import com.example.download.DownloadProgress
import com.example.myapp.databinding.ActivityDownLoadFromOkhttpBinding
import com.example.utils.ktx.binding

class DownLoadFromOkhttpActivity : BaseActivity() {

    private val mBinding by binding(ActivityDownLoadFromOkhttpBinding::inflate)

    //    private val url = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"
//    private val url = "https://media.w3.org/2010/05/sintel/trailer.mp4"
    private val url = "https://github.com/dhewm/dhewm3/releases/download/1.5.4/dhewm3-mods-1.5.4_win32.zip"
    private var downloadId = ""


    //    private val url = "https://github.com/owncloud/android/archive/refs/heads/master.zip"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


//        lifecycleScope.launch {
//            DownloadHelper.registerCallback(url, object : DownloadCallback {
//                override fun onStatusChanged(status: DownloadStatus) {
//                    when (status) {
//                        is DownloadStatus.Connecting -> Log.d("Test", "连接中...")
//                        is DownloadStatus.Progress -> Log.d("Test", "进度: ${status.percentage}")
//                        is DownloadStatus.Completed -> Log.d("Test", "完成: ${status.filePath}")
//                        is DownloadStatus.Failed -> Log.d("Test", "失败: ${status.message}")
//                        is DownloadStatus.Cancelled -> Log.d("Test", "取消")
//                        else -> Unit
//                    }
//                }
//
//            })
//        }
//
//        mBinding.btnDownloadOkhttp.setOnClickListener {
//            lifecycleScope.launch {
//                DownloadHelper.enqueue(this@DownLoadFromOkhttpActivity, url, "fileName")
//            }
//        }


        val downloadManager = DownloadManager.getInstance(
            DownloadConfig(
                maxConcurrentDownloads = 3,
                threadCount = 4,
                retryCount = 3
            )
        )


        val listener = object : DownloadListener {
            override fun onStart(downloadInfo: DownloadInfo) {
                super.onStart(downloadInfo)
                Log.d("Test", "开始下载: ${downloadInfo.fileName}")
            }

            override fun onProgress(downloadInfo: DownloadInfo, progress: DownloadProgress) {
                super.onProgress(downloadInfo, progress)
                mBinding.progressBar.progress = (progress.progress * 100).toInt()
                Log.d("Test", "进度: ${progress.progress}")
            }

            override fun onPause(downloadInfo: DownloadInfo) {
                super.onPause(downloadInfo)
                Log.d("Test", "暂停下载: ${downloadInfo.fileName}")
            }

            override fun onResume(downloadInfo: DownloadInfo) {
                super.onResume(downloadInfo)
                Log.d("Test", "恢复下载: ${downloadInfo.fileName}")
            }

            override fun onComplete(downloadInfo: DownloadInfo) {
                super.onComplete(downloadInfo)
                Log.d("Test", "下载完成: ${downloadInfo.fileName}")
            }

            override fun onError(downloadInfo: DownloadInfo, error: Throwable) {
                super.onError(downloadInfo, error)
                Log.d("Test", "下载失败: ${error.message}")
            }

            override fun onCancel(downloadInfo: DownloadInfo) {
                super.onCancel(downloadInfo)
                Log.d("Test", "取消下载: ${downloadInfo.fileName}")
            }
        }


        val videoDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)

        mBinding.btnDownloadOkhttp.setOnClickListener {
            // 开始下载
            downloadId = downloadManager.download(
                url = url,
                filePath = videoDir?.absolutePath ?: "",
                fileName = "myfile.zip",
                listener = listener
            )
        }

        mBinding.btnPause.setOnClickListener {
            // 暂停下载
            downloadManager.pause(downloadId)
        }
        mBinding.btnCancle.setOnClickListener {
            // 取消下载
            downloadManager.cancel(downloadId)
        }
    }


}