package com.example.download

// 下载配置
data class DownloadConfig(
    val maxConcurrentDownloads: Int = 3,
    val connectTimeout: Long = 30_000L,
    val readTimeout: Long = 30_000L,
    val writeTimeout: Long = 30_000L,
    val retryCount: Int = 3,
    val retryDelay: Long = 1000L,
    val threadCount: Int = 3, // 多线程下载的线程数
    val bufferSize: Int = 8192,
    val progressUpdateInterval: Long = 500L // 进度更新间隔(毫秒)
)