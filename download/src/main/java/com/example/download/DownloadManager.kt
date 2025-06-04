package com.example.download

import okhttp3.OkHttpClient
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class DownloadManager private constructor(
    val config: DownloadConfig = DownloadConfig()
) {

    companion object {
        @Volatile
        private var INSTANCE: DownloadManager? = null

        fun getInstance(config: DownloadConfig = DownloadConfig()): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManager(config).also { INSTANCE = it }
            }
        }
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(config.connectTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(config.readTimeout, TimeUnit.MILLISECONDS)
        .writeTimeout(config.writeTimeout, TimeUnit.MILLISECONDS)
        .build()

    private val downloadExecutor = ThreadPoolExecutor(
        config.maxConcurrentDownloads,
        config.maxConcurrentDownloads,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(),
        ThreadFactory { r -> Thread(r, "DownloadManager-${System.currentTimeMillis()}") }
    )

    private val downloadInfoMap = ConcurrentHashMap<String, DownloadInfo>()
    private val downloadTaskMap = ConcurrentHashMap<String, DownloadTask>()
    private val listenerMap = ConcurrentHashMap<String, MutableList<DownloadListener>>()

    /**
     * 开始下载
     */
    fun download(
        url: String,
        filePath: String,
        fileName: String? = null,
        listener: DownloadListener? = null
    ): String {
        val downloadId = generateDownloadId(url, filePath)
        val actualFileName = fileName ?: extractFileNameFromUrl(url)
        val fullPath = File(filePath, actualFileName).absolutePath

        val downloadInfo = DownloadInfo(
            id = downloadId,
            url = url,
            fileName = actualFileName,
            filePath = fullPath
        )

        downloadInfoMap[downloadId] = downloadInfo
        listener?.let { addListener(downloadId, it) }

        val task = DownloadTask(downloadInfo, this)
        downloadTaskMap[downloadId] = task
        downloadExecutor.execute(task)

        return downloadId
    }

    /**
     * 暂停下载
     */
    fun pause(downloadId: String): Boolean {
        return downloadTaskMap[downloadId]?.pause() ?: false
    }

    /**
     * 恢复下载
     */
    fun resume(downloadId: String): Boolean {
        val downloadInfo = downloadInfoMap[downloadId] ?: return false
        if (downloadInfo.status != DownloadStatus.PAUSED) return false

        val task = DownloadTask(downloadInfo, this)
        downloadTaskMap[downloadId] = task
        downloadExecutor.execute(task)
        return true
    }

    /**
     * 取消下载
     */
    fun cancel(downloadId: String): Boolean {
        val task = downloadTaskMap[downloadId]
        val result = task?.cancel() ?: false
        downloadTaskMap.remove(downloadId)
        return result
    }

    /**
     * 删除下载(包括文件)
     */
    fun delete(downloadId: String): Boolean {
        cancel(downloadId)
        val downloadInfo = downloadInfoMap[downloadId] ?: return false

        try {
            File(downloadInfo.filePath).delete()
            File("${downloadInfo.filePath}.tmp").delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
        val completedIds = downloadInfoMap.filter {
            it.value.status == DownloadStatus.COMPLETED
        }.keys

        completedIds.forEach { id ->
            downloadInfoMap.remove(id)
            listenerMap.remove(id)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        downloadExecutor.shutdown()
        try {
            if (!downloadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                downloadExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            downloadExecutor.shutdownNow()
        }

        downloadTaskMap.clear()
        downloadInfoMap.clear()
        listenerMap.clear()
    }

    internal fun notifyListeners(downloadId: String, action: (DownloadListener) -> Unit) {
        listenerMap[downloadId]?.forEach { listener ->
            try {
                action(listener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal fun updateDownloadInfo(downloadInfo: DownloadInfo) {
        downloadInfoMap[downloadInfo.id] = downloadInfo
    }

    private fun generateDownloadId(url: String, filePath: String): String {
        return MessageDigest.getInstance("MD5")
            .digest("$url$filePath".toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val fileName = url.substringAfterLast("/").substringBefore("?")
            if (fileName.isNotEmpty()) fileName else "download_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }
}