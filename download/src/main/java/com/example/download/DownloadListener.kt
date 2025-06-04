package com.example.download

// 下载监听器
interface DownloadListener {
    fun onStart(downloadInfo: DownloadInfo) {}
    fun onProgress(downloadInfo: DownloadInfo, progress: DownloadProgress) {}
    fun onPause(downloadInfo: DownloadInfo) {}
    fun onResume(downloadInfo: DownloadInfo) {}
    fun onComplete(downloadInfo: DownloadInfo) {}
    fun onError(downloadInfo: DownloadInfo, error: Throwable) {}
    fun onCancel(downloadInfo: DownloadInfo) {}
}