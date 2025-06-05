package com.example.newload

import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// 下载管理器
class DownloadManager private constructor() {
    companion object {
        @Volatile
        private var instance: DownloadManager? = null

        fun getInstance(): DownloadManager {
            return instance ?: synchronized(this) {
                instance ?: DownloadManager().also { instance = it }
            }
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .build()
    
    private val tasks = ConcurrentHashMap<String, DownloadTask>()
    private val callbacks = ConcurrentHashMap<String, DownloadCallback>()
    private val calls = ConcurrentHashMap<String, Call>()
    private val executor: ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    )
    private val handler = Handler(Looper.getMainLooper())

    // 添加下载任务
    fun addTask(url: String, filePath: String, taskId: String, callback: DownloadCallback) {
        if (tasks.containsKey(taskId)) {
            callback.onError(taskId, "Task ID already exists")
            return
        }

        val task = DownloadTask(url, filePath, taskId)
        tasks[taskId] = task
        callbacks[taskId] = callback
        executeDownload(task)
    }

    // 取消下载任务
    fun cancelTask(taskId: String) {
        calls[taskId]?.cancel()
        tasks[taskId]?.let {
            it.status = DownloadStatus.CANCELLED
            notifyStatusChanged(it.taskId, DownloadStatus.CANCELLED)
        }
        cleanupTask(taskId)
    }

    // 取消所有任务
    fun cancelAllTasks() {
        calls.forEach { it.value.cancel() }
        tasks.forEach { it.value.status = DownloadStatus.CANCELLED }
        tasks.keys().toList().forEach { notifyStatusChanged(it, DownloadStatus.CANCELLED) }
        tasks.clear()
        calls.clear()
    }

    // 获取任务状态
    fun getTaskStatus(taskId: String): DownloadStatus? {
        return tasks[taskId]?.status
    }

    private fun executeDownload(task: DownloadTask) {
        task.status = DownloadStatus.QUEUED
        notifyStatusChanged(task.taskId, DownloadStatus.QUEUED)

        executor.execute {
            try {
                val request = Request.Builder().url(task.url).build()
                val call = client.newCall(request)
                calls[task.taskId] = call

                task.status = DownloadStatus.DOWNLOADING
                notifyStatusChanged(task.taskId, DownloadStatus.DOWNLOADING)

                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (call.isCanceled()) {
                            task.status = DownloadStatus.CANCELLED
                            notifyStatusChanged(task.taskId, DownloadStatus.CANCELLED)
                        } else {
                            task.status = DownloadStatus.FAILED
                            notifyError(task.taskId, e.message ?: "Download failed")
                        }
                        cleanupTask(task.taskId)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.body?.let { body ->
                            try {
                                val totalBytes = body.contentLength()
                                task.totalBytes = totalBytes
                                var downloadedBytes: Long = 0
                                val buffer = ByteArray(8 * 1024) // 8KB buffer
                                var bytesRead: Int

                                File(task.filePath).parentFile?.mkdirs()
                                FileOutputStream(task.filePath).use { output ->
                                    body.byteStream().use { input ->
                                        while (input.read(buffer).also { bytesRead = it } != -1) {
                                            if (call.isCanceled()) {
                                                task.status = DownloadStatus.CANCELLED
                                                notifyStatusChanged(task.taskId, DownloadStatus.CANCELLED)
                                                return
                                            }
                                            output.write(buffer, 0, bytesRead)
                                            downloadedBytes += bytesRead
                                            task.downloadedBytes = downloadedBytes
                                            if (totalBytes > 0) {
                                                task.progress = ((downloadedBytes * 100) / totalBytes).toInt()
                                                notifyProgress(task.taskId, task.progress, downloadedBytes, totalBytes)
                                            }
                                        }
                                    }
                                }

                                task.status = DownloadStatus.COMPLETED
                                notifyStatusChanged(task.taskId, DownloadStatus.COMPLETED)
                            } catch (e: Exception) {
                                task.status = DownloadStatus.FAILED
                                notifyError(task.taskId, e.message ?: "Download failed")
                            } finally {
                                cleanupTask(task.taskId)
                            }
                        } ?: run {
                            task.status = DownloadStatus.FAILED
                            notifyError(task.taskId, "Empty response body")
                            cleanupTask(task.taskId)
                        }
                    }
                })
            } catch (e: Exception) {
                task.status = DownloadStatus.FAILED
                notifyError(task.taskId, e.message ?: "Download failed")
                cleanupTask(task.taskId)
            }
        }
    }

    private fun cleanupTask(taskId: String) {
        calls.remove(taskId)
        tasks.remove(taskId)
    }

    private fun notifyProgress(taskId: String, progress: Int, downloadedBytes: Long, totalBytes: Long) {
        handler.post {
            callbacks[taskId]?.onProgress(taskId, progress, downloadedBytes, totalBytes)
        }
    }

    private fun notifyStatusChanged(taskId: String, status: DownloadStatus) {
        handler.post {
            callbacks[taskId]?.onStatusChanged(taskId, status)
        }
    }

    private fun notifyError(taskId: String, error: String) {
        handler.post {
            callbacks[taskId]?.onError(taskId, error)
        }
    }

    // 清理资源
    fun shutdown() {
        cancelAllTasks()
        executor.shutdown()
    }
}