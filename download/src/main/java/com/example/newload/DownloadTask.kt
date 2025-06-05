package com.example.newload

// 下载任务信息
data class DownloadTask(
    val url: String,
    val filePath: String,
    val taskId: String,
    var status: DownloadStatus = DownloadStatus.QUEUED,
    var progress: Int = 0, // 0-100
    var totalBytes: Long = 0,
    var downloadedBytes: Long = 0
)