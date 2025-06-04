package com.example.download

/**
 * 表示下载任务的信息类
 *
 * 该数据类封装了下载任务的相关信息，包括下载的唯一标识符、下载链接、文件名、文件路径、
 * 下载状态、创建时间、完成时间以及错误信息等属性
 */
data class DownloadInfo(
    /**
     * 下载任务的唯一标识符
     */
    val id: String,
    /**
     * 下载链接
     */
    val url: String,
    /**
     * 被下载文件的名称
     */
    val fileName: String,
    /**
     * 文件保存的路径
     */
    val filePath: String,
    /**
     * 文件总大小（字节）
     *
     * 默认值为0，表示未知或未初始化
     */
    val totalBytes: Long = 0L,
    /**
     * 已下载的文件大小（字节）
     *
     * 默认值为0，表示尚未开始下载
     */
    val downloadedBytes: Long = 0L,
    /**
     * 下载任务的状态
     *
     * 默认状态为待处理（PENDING）
     */
    val status: DownloadStatus = DownloadStatus.PENDING,
    /**
     * 下载任务创建的时间戳（毫秒）
     *
     * 默认值为实例创建时的当前系统时间
     */
    val createTime: Long = System.currentTimeMillis(),
    /**
     * 下载任务完成的时间戳（毫秒）
     *
     * 默认为null，表示任务尚未完成
     */
    val completeTime: Long? = null,
    /**
     * 错误信息
     *
     * 当下载任务遇到错误时，提供错误描述信息，默认为null
     */
    val errorMessage: String? = null
)
