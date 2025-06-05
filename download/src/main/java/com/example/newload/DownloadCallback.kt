package com.example.newload

// 下载回调接口
interface DownloadCallback {
    fun onProgress(taskId: String, progress: Int, downloadedBytes: Long, totalBytes: Long)
    fun onStatusChanged(taskId: String, status: DownloadStatus)
    fun onError(taskId: String, error: String)
}