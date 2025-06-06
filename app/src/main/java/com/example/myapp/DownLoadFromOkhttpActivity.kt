package com.example.myapp

import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.example.base.BaseActivity
import com.example.myapp.databinding.ActivityDownLoadFromOkhttpBinding
import com.example.newload.DownloadCallback
import com.example.newload.DownloadManager
import com.example.newload.DownloadStatus
import com.example.utils.ktx.binding

class DownLoadFromOkhttpActivity : BaseActivity() {

    private val mBinding by binding(ActivityDownLoadFromOkhttpBinding::inflate)

    private val url = "https://cdn2.eso.org/images/original/eso1242a.psb"  //24g大图
    private val url1 = "https://media.w3.org/2010/05/sintel/trailer.mp4"
    private val url2 = "https://t-cdn.kaiyanapp.com/7c09fffc63c0122dede7af7dae46dc1b_720P.mp4"
    private val url3 =
        "https://github.com/dhewm/dhewm3/releases/download/1.5.4/dhewm3-mods-1.5.4_win32.zip"
    private var downloadId = ""

    private var download_status = DownloadStatus.COMPLETED

    //    private val url = "https://github.com/owncloud/android/archive/refs/heads/master.zip"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


//
//        val downloadManager = DownloadManager.getInstance(
//            DownloadConfig(
//                maxConcurrentDownloads = 3,
//                threadCount = 4,
//                retryCount = 3
//            )
//        )
//
//
//        val listener = object : DownloadListener {
//            override fun onStart(downloadInfo: DownloadInfo) {
//                super.onStart(downloadInfo)
//                Log.d("Test", "开始下载: ${downloadInfo.fileName}")
//            }
//
//            override fun onProgress(downloadInfo: DownloadInfo, progress: DownloadProgress) {
//                super.onProgress(downloadInfo, progress)
//                mBinding.progressBar.progress = (progress.progress * 100).toInt()
//                Log.d("Test", "进度: ${progress.progress}")
//            }
//
//            override fun onPause(downloadInfo: DownloadInfo) {
//                super.onPause(downloadInfo)
//                Log.d("Test", "暂停下载: ${downloadInfo.fileName}")
//            }
//
//            override fun onResume(downloadInfo: DownloadInfo) {
//                super.onResume(downloadInfo)
//                Log.d("Test", "恢复下载: ${downloadInfo.fileName}")
//            }
//
//            override fun onComplete(downloadInfo: DownloadInfo) {
//                super.onComplete(downloadInfo)
//                Log.d("Test", "下载完成: ${downloadInfo.fileName}")
//            }
//
//            override fun onError(downloadInfo: DownloadInfo, error: Throwable) {
//                super.onError(downloadInfo, error)
//                Log.d("Test", "下载失败: ${error.message}")
//            }
//
//            override fun onCancel(downloadInfo: DownloadInfo) {
//                super.onCancel(downloadInfo)
//                Log.d("Test", "取消下载: ${downloadInfo.fileName}")
//            }
//        }
//
//
//        val videoDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
//
//        mBinding.btnDownloadOkhttp.setOnClickListener {
//            // 开始下载
//            downloadId = downloadManager.download(
//                url = url,
//                dir = videoDir?.absolutePath ?: "",
//                fileName = "myfile.zip",
//                listener = listener
//            )
//        }
//
//        mBinding.btnPause.setOnClickListener {
//            // 暂停下载
//            downloadManager.pause(downloadId)
//        }
//        mBinding.btnCancle.setOnClickListener {
//            // 取消下载
//            downloadManager.cancel(downloadId)
//        }
    }


    override fun initData() {
        super.initData()
        val videoDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val downloadManager = DownloadManager.getInstance()

        val callback = object : DownloadCallback {
            override fun onProgress(
                taskId: String,
                progress: Int,
                downloadedBytes: Long,
                totalBytes: Long
            ) {
                Log.d(
                    "Download",
                    "Task $taskId progress: $progress% ($downloadedBytes/$totalBytes)"
                )
                mBinding.btnDownloadOkhttp.text = "$progress%"
                mBinding.progressBar.progress = progress
            }

            override fun onStatusChanged(taskId: String, status: DownloadStatus) {
                Log.d("Download", "onStatusChanged---Task $taskId status: $status")
                download_status = status
                when (status) {
                    DownloadStatus.QUEUED -> {
                        mBinding.btnDownloadOkhttp.text = "等待中..."
                        mBinding.btnDownloadOkhttp.isEnabled = false
                    }

                    DownloadStatus.DOWNLOADING -> {
                        mBinding.btnDownloadOkhttp.text = "下载中..."
                        mBinding.btnDownloadOkhttp.isEnabled = true
                    }

                    DownloadStatus.COMPLETED -> {
                        mBinding.btnDownloadOkhttp.text = "下载完成"
                        mBinding.btnDownloadOkhttp.isEnabled = true
                    }

                    DownloadStatus.FAILED -> {
                        mBinding.btnDownloadOkhttp.text = "下载失败"
                        mBinding.btnDownloadOkhttp.isEnabled = true
                    }

                    DownloadStatus.CANCELLED -> {
                        mBinding.btnDownloadOkhttp.text = "已取消"
                        mBinding.btnDownloadOkhttp.isEnabled = true
                    }

                    DownloadStatus.PAUSED -> {
                        mBinding.btnDownloadOkhttp.text = "已暂停"
                        mBinding.btnDownloadOkhttp.isEnabled = true
                    }

                    else -> {
                        mBinding.btnDownloadOkhttp.text = "未知状态"
                        mBinding.btnDownloadOkhttp.isEnabled = true
                    }
                }
            }

            override fun onError(taskId: String, error: String) {
                Log.e("Download", "Task $taskId error: $error")
            }
        }

        val callback2 = object : DownloadCallback {
            override fun onProgress(
                taskId: String,
                progress: Int,
                downloadedBytes: Long,
                totalBytes: Long
            ) {
                Log.d(
                    "Download",
                    "Task $taskId progress: $progress% ($downloadedBytes/$totalBytes)"
                )
                mBinding.progressBar2.progress = progress
            }

            override fun onStatusChanged(taskId: String, status: DownloadStatus) {
                Log.d("Download", "Task $taskId status: $status")
            }

            override fun onError(taskId: String, error: String) {
                Log.e("Download", "Task $taskId error: $error")
            }
        }


        // 添加多个下载任务
//        downloadManager.addTask(
//            url = url,
//            filePath = videoDir?.absolutePath + "file1.zip",
//            taskId = "task1",
//            callback = callback
//        )

//        downloadManager.addTask(
//            url = url2,
//            filePath = videoDir?.absolutePath + "file2.zip",
//            taskId = "task2",
//            callback = callback2
//        )


        // 获取任务状态示例
        val status = downloadManager.getTaskStatus("task1")
        Log.d("Download", "Task1 status: $status")


        mBinding.btnCancle.setOnClickListener {
            downloadManager.cancelTask("task1")
        }
        mBinding.btnPause.setOnClickListener {
            downloadManager.resumeTask("task1")
        }

        mBinding.btnDownloadOkhttp.setOnClickListener {

            when (download_status) {
                DownloadStatus.QUEUED -> {
                }

                DownloadStatus.DOWNLOADING -> {
                    downloadManager.pauseTask("task1")
                }

                DownloadStatus.COMPLETED -> {
                    downloadManager.addTask(
                        url = url,
                        filePath = videoDir?.absolutePath + "file1.zip",
                        taskId = "task1",
                        callback = callback
                    )
                }

                DownloadStatus.FAILED -> {
                    downloadManager.addTask(
                        url = url,
                        filePath = videoDir?.absolutePath + "file1.zip",
                        taskId = "task1",
                        callback = callback
                    )
                }

                DownloadStatus.CANCELLED -> {
                    downloadManager.addTask(
                        url = url,
                        filePath = videoDir?.absolutePath + "file1.zip",
                        taskId = "task1",
                        callback = callback
                    )
                }
                DownloadStatus.PAUSED -> {
                    downloadManager.resumeTask("task1")
                }
            }
        }
    }
}