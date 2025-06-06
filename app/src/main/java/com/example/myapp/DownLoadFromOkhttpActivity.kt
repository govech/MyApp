package com.example.myapp

import android.os.Environment
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.base.BaseActivity
import com.example.myapp.databinding.ActivityDownLoadFromOkhttpBinding
import com.example.newload.DownloadCallback
import com.example.newload.DownloadManager
import com.example.newload.DownloadStatus
import com.example.utils.ktx.binding
import kotlinx.coroutines.launch

class DownLoadFromOkhttpActivity : BaseActivity() {

    private val mBinding by binding(ActivityDownLoadFromOkhttpBinding::inflate)

    private val url = "https://cdn2.eso.org/images/original/eso1242a.psb"  //24g大图
    private val url1 = "https://media.w3.org/2010/05/sintel/trailer.mp4"
    private val url2 = "https://t-cdn.kaiyanapp.com/7c09fffc63c0122dede7af7dae46dc1b_720P.mp4"
    private val url3 =
        "https://github.com/dhewm/dhewm3/releases/download/1.5.4/dhewm3-mods-1.5.4_win32.zip"
    private var downloadId = ""

    private var download_status = DownloadStatus.COMPLETED

    private val viewModel: DownloadViewModel by viewModels()
    override fun initView() {
        super.initView()
        initRv()
    }


    override fun initData() {
        super.initData()
        val videoDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val downloadManager = DownloadManager.getInstance()

        val callback = object : DownloadCallback {
            override fun onProgress(
                taskId: String,
                progress: Double,
                downloadedBytes: Long,
                totalBytes: Long
            ) {
                Log.d(
                    "Download",
                    "Task $taskId progress: $progress% ($downloadedBytes/$totalBytes)"
                )
                mBinding.btnDownloadOkhttp.text = "$progress%"
                mBinding.progressBar.progress = progress.toInt()
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





        mBinding.btnDownloadOkhttp.setOnClickListener {

            when (download_status) {
                DownloadStatus.QUEUED -> {
                }

                DownloadStatus.DOWNLOADING -> {
                    downloadManager.pauseTask("task1")
                }

                DownloadStatus.COMPLETED -> {
                    downloadManager.addTask(
                        url = url1,
                        filePath = videoDir?.absolutePath + "file1.zip",
                        taskId = "task1",
                        callback = callback
                    )
                }

                DownloadStatus.FAILED -> {
                    downloadManager.addTask(
                        url = url1,
                        filePath = videoDir?.absolutePath + "file1.zip",
                        taskId = "task1",
                        callback = callback
                    )
                }

                DownloadStatus.CANCELLED -> {
                    downloadManager.addTask(
                        url = url1,
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


    private fun initRv() {
        val adapter = DownloadAdapter(viewModel)
        mBinding.rvDownload.adapter = adapter

        lifecycleScope.launch {
            viewModel.tasks.collect { tasks ->
                adapter.submitList(tasks)
            }
        }
        val videoDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (videoDir != null) {
            for (i in 1..6) {
                val fileName = "file$i.zip"
                viewModel.addTask(url1, "$videoDir/$fileName")
            }
        }
    }
}