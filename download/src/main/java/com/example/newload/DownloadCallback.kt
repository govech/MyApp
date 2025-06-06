package com.example.newload

// 下载回调接口
interface DownloadCallback {
    fun onProgress(taskId: String, progress: Double, downloadedBytes: Long, totalBytes: Long)
    fun onStatusChanged(taskId: String, status: DownloadStatus)
    fun onError(taskId: String, error: String)
}