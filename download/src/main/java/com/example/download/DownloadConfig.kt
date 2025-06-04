package com.example.download

/**
 * 下载配置类，用于配置下载任务的各种参数
 *
 * @param maxConcurrentDownloads 最大并发下载数，默认为3
 * @param connectTimeout 连接超时时间，默认为30000毫秒
 * @param readTimeout 读取超时时间，默认为30000毫秒
 * @param writeTimeout 写入超时时间，默认为30000毫秒
 * @param retryCount 重试次数，默认为3次
 * @param retryDelay 重试间隔时间，默认为1000毫秒
 * @param threadCount 多线程下载的线程数，默认为3
 * @param bufferSize 缓冲区大小，默认为8192字节
 * @param progressUpdateInterval 进度更新间隔，默认为500毫秒
 */
data class DownloadConfig(
    val maxConcurrentDownloads: Int = 3,
    val connectTimeout: Long = 30_000L,
    val readTimeout: Long = 30_000L,
    val writeTimeout: Long = 30_000L,
    val retryCount: Int = 3,
    val retryDelay: Long = 1000L,
    val threadCount: Int = 3,
    val bufferSize: Int = 8192,
    val progressUpdateInterval: Long = 500L
)
