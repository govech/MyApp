package com.example.download

// 下载信息
data class DownloadInfo(
    val id: String,
    val url: String,
    val fileName: String,
    val filePath: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val createTime: Long = System.currentTimeMillis(),
    val completeTime: Long? = null,
    val errorMessage: String? = null

)