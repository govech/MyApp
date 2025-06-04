package com.example.download

import okhttp3.OkHttpClient
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 下载管理器类，负责管理下载任务
 */
class DownloadManager private constructor(
    val config: DownloadConfig = DownloadConfig()
) {

    companion object {
        @Volatile
        private var INSTANCE: DownloadManager? = null

        /**
         * 获取DownloadManager单例
         */
        fun getInstance(config: DownloadConfig = DownloadConfig()): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManager(config).also { INSTANCE = it }
            }
        }
    }

    // OkHttp客户端，用于网络请求
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(config.connectTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(config.readTimeout, TimeUnit.MILLISECONDS)
        .writeTimeout(config.writeTimeout, TimeUnit.MILLISECONDS)
        .build()

    // 下载任务执行器，用于并发执行下载任务
    private val downloadExecutor = ThreadPoolExecutor(
        config.maxConcurrentDownloads,
        config.maxConcurrentDownloads,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(),
        ThreadFactory { r -> Thread(r, "DownloadManager-${System.currentTimeMillis()}") }
    )

    // 存储下载信息的映射表
    private val downloadInfoMap = ConcurrentHashMap<String, DownloadInfo>()
    // 存储下载任务的映射表
    private val downloadTaskMap = ConcurrentHashMap<String, DownloadTask>()
    // 存储监听器的映射表
    private val listenerMap = ConcurrentHashMap<String, MutableList<DownloadListener>>()

    /**
     * 开始下载任务
     */
    fun download(
        url: String,
        filePath: String,
        fileName: String? = null,
        listener: DownloadListener? = null
    ): String {
        // 生成下载ID
        val downloadId = generateDownloadId(url, filePath)
        // 实际的文件名
        val actualFileName = fileName ?: extractFileNameFromUrl(url)
        // 文件的完整路径
        val fullPath = File(filePath, actualFileName).absolutePath

        // 创建下载信息对象
        val downloadInfo = DownloadInfo(
            id = downloadId,
            url = url,
            fileName = actualFileName,
            filePath = fullPath
        )

        // 将下载信息存入映射表
        downloadInfoMap[downloadId] = downloadInfo
        // 添加监听器
        listener?.let { addListener(downloadId, it) }

        // 创建并提交下载任务
        val task = DownloadTask(downloadInfo, this)
        downloadTaskMap[downloadId] = task
        downloadExecutor.execute(task)

        // 返回下载ID
        return downloadId
    }

    /**
     * 暂停下载任务
     */
    fun pause(downloadId: String): Boolean {
        // 调用下载任务的暂停方法
        return downloadTaskMap[downloadId]?.pause() ?: false
    }

    /**
     * 恢复下载任务
     */
    fun resume(downloadId: String): Boolean {
        // 获取下载信息
        val downloadInfo = downloadInfoMap[downloadId] ?: return false
        // 检查任务状态
        if (downloadInfo.status != DownloadStatus.PAUSED) return false

        // 创建并提交下载任务
        val task = DownloadTask(downloadInfo, this)
        downloadTaskMap[downloadId] = task
        downloadExecutor.execute(task)
        return true
    }

    /**
     * 取消下载任务
     */
    fun cancel(downloadId: String): Boolean {
        // 调用下载任务的取消方法
        val task = downloadTaskMap[downloadId]
        val result = task?.cancel() ?: false
        downloadTaskMap.remove(downloadId)
        return result
    }

    /**
     * 删除下载任务(包括文件)
     */
    fun delete(downloadId: String): Boolean {
        // 取消下载任务
        cancel(downloadId)
        // 获取下载信息
        val downloadInfo = downloadInfoMap[downloadId] ?: return false

        // 删除文件
        try {
            File(downloadInfo.filePath).delete()
            File("${downloadInfo.filePath}.tmp").delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 移除映射表中的信息
        downloadInfoMap.remove(downloadId)
        listenerMap.remove(downloadId)
        return true
    }

    /**
     * 获取下载信息
     */
    fun getDownloadInfo(downloadId: String): DownloadInfo? {
        return downloadInfoMap[downloadId]
    }

    /**
     * 获取所有下载信息
     */
    fun getAllDownloads(): List<DownloadInfo> {
        return downloadInfoMap.values.toList()
    }

    /**
     * 添加监听器
     */
    fun addListener(downloadId: String, listener: DownloadListener) {
        listenerMap.getOrPut(downloadId) { mutableListOf() }.add(listener)
    }

    /**
     * 移除监听器
     */
    fun removeListener(downloadId: String, listener: DownloadListener) {
        listenerMap[downloadId]?.remove(listener)
    }

    /**
     * 清理已完成的下载记录
     */
    fun clearCompleted() {
        // 获取已完成的下载ID
        val completedIds = downloadInfoMap.filter {
            it.value.status == DownloadStatus.COMPLETED
        }.keys

        // 移除已完成的记录
        completedIds.forEach { id ->
            downloadInfoMap.remove(id)
            listenerMap.remove(id)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        // 关闭下载任务执行器
        downloadExecutor.shutdown()
        try {
            if (!downloadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                downloadExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            downloadExecutor.shutdownNow()
        }

        // 清空映射表
        downloadTaskMap.clear()
        downloadInfoMap.clear()
        listenerMap.clear()
    }

    /**
     * 通知监听器
     */
    internal fun notifyListeners(downloadId: String, action: (DownloadListener) -> Unit) {
        listenerMap[downloadId]?.forEach { listener ->
            try {
                action(listener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 更新下载信息
     */
    internal fun updateDownloadInfo(downloadInfo: DownloadInfo) {
        downloadInfoMap[downloadInfo.id] = downloadInfo
    }

    /**
     * 生成下载ID
     */
    private fun generateDownloadId(url: String, filePath: String): String {
        return MessageDigest.getInstance("MD5")
            .digest("$url$filePath".toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * 从URL中提取文件名
     */
    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val fileName = url.substringAfterLast("/").substringBefore("?")
            if (fileName.isNotEmpty()) fileName else "download_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }
}
