package com.example.newload

/**
 * 下载状态枚举类
 *
 * 用于表示文件下载的各个状态，包括：
 * - QUEUED：表示下载任务已排队等待下载
 * - DOWNLOADING：表示下载任务正在进行中
 * - COMPLETED：表示下载任务已完成
 * - FAILED：表示下载任务失败
 * - CANCELLED：表示下载任务已被取消
 * - PAUSED：表示下载任务已暂停
 */
enum class DownloadStatus {
    QUEUED, DOWNLOADING, COMPLETED, FAILED, CANCELLED,PAUSED
}