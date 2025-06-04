package com.example.download
// 下载状态枚举
enum class DownloadStatus {
    PENDING,     // 等待中
    DOWNLOADING, // 下载中
    PAUSED,      // 暂停
    COMPLETED,   // 完成
    FAILED,      // 失败
    CANCELLED    // 取消
}