package com.example.newload

/**
 * 下载任务的数据类
 *
 * @param url 下载资源的URL
 * @param filePath 下载文件保存的路径
 * @param taskId 下载任务的唯一标识符
 * @param status 下载任务的状态，默认为排队中
 * @param progress 下载任务的进度，默认为0，范围是0-100
 * @param totalBytes 下载文件的总大小，默认为0
 * @param downloadedBytes 已下载的文件大小，默认为0
 * @param supportsRange 新增：是否支持Range请求
 */
data class DownloadTask(
    val url: String,
    val filePath: String,
    val taskId: String,
    var status: DownloadStatus = DownloadStatus.QUEUED,
    var progress: Int = 0, // 0-100
    var totalBytes: Long = 0,
    var downloadedBytes: Long = 0,
    var supportsRange: Boolean = false // 新增：是否支持Range请求
)