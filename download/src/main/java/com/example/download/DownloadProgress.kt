package com.example.download

// 下载进度信息
data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val progress: Float = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f,
    val speed: Long = 0L, // 字节/秒
    val remainingTime: Long = 0L // 剩余时间(秒)
)