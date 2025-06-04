package com.example.download

/**
 * 表示下载进度的信息类
 *
 * 该类包含了下载过程中的关键信息，如已下载字节数、总字节数、下载进度、下载速度和剩余下载时间
 * 主要用于更新用户界面，以便用户可以实时了解下载任务的进展情况
 *
 * @param downloadedBytes 已下载的字节数
 * @param totalBytes 总字节数，用于计算下载进度
 * @param progress 下载进度，范围为0.0到1.0，表示完成的比例
 * @param speed 当前的下载速度，单位为字节/秒
 * @param remainingTime 剩余的下载时间，单位为秒
 */
data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val progress: Float = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f,
    val speed: Long = 0L, // 字节/秒
    val remainingTime: Long = 0L // 剩余时间(秒)
)
