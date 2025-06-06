package com.example.newload

import android.os.Handler
import android.os.Looper
import android.util.Log
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
import java.util.concurrent.TimeUnit

/**
 * 下载管理器类，用于管理多个下载任务
 */
class DownloadManager private constructor() {
    companion object {
        @Volatile
        private var instance: DownloadManager? = null

        /**
         * 获取下载管理器的单例实例
         * @return 下载管理器实例
         */
        fun getInstance(): DownloadManager {
            return instance ?: synchronized(this) {
                instance ?: DownloadManager().also { instance = it }
            }
        }

        private const val TAG = "DownloadManager"
    }

    // 创建一个OkHttpClient实例，用于执行网络请求
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // 保存所有下载任务，键为任务标识，值为具体的下载任务对象
    private val tasks = ConcurrentHashMap<String, DownloadTask>()

    // 保存所有下载回调，键为任务标识，值为具体的下载回调对象
    private val callbacks = ConcurrentHashMap<String, DownloadCallback>()

    // 保存所有网络请求的Call对象，键为任务标识，值为具体的Call对象
    private val calls = ConcurrentHashMap<String, Call>()

    /**
     * 创建了一个固定大小的线程池，用于并发执行任务。具体功能如下：
     * Executors.newFixedThreadPool(...)：创建一个固定线程数量的线程池。
     * Runtime.getRuntime().availableProcessors()：获取当前设备可用的 CPU 核心数，作为线程池的大小。
     */
    private val executor: ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    )

    // 创建一个Handler对象，用于在主线程中执行操作
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 添加下载任务
     * @param url 下载链接
     * @param filePath 保存文件的路径
     * @param taskId 任务ID
     * @param callback 下载回调接口
     */
    fun addTask(url: String, filePath: String, taskId: String, callback: DownloadCallback) {
        if (tasks.containsKey(taskId)) {
            callback.onError(taskId, "Task ID already exists")
            Log.e(TAG, "Task ID $taskId already exists")
            return
        }

        val task = DownloadTask(url, filePath, taskId)
        tasks[taskId] = task
        callbacks[taskId] = callback
        Log.d(TAG, "Adding task $taskId with URL: ${task.url}")
        checkRangeSupportAndExecute(task)
    }

    /**
     * 暂停下载任务
     * @param taskId 任务ID
     */
    fun pauseTask(taskId: String) {
        tasks[taskId]?.let { task ->
            if (task.status == DownloadStatus.DOWNLOADING) {
                calls[taskId]?.cancel()
                task.status = DownloadStatus.PAUSED
                notifyStatusChanged(taskId, DownloadStatus.PAUSED)
                Log.d(TAG, "Paused task $taskId, downloadedBytes: ${task.downloadedBytes}")
            } else {
                Log.w(TAG, "Cannot pause task $taskId, current status: ${task.status}")
            }
        } ?: Log.w(TAG, "Task $taskId not found for pause")
    }

    // 恢复下载任务
    fun resumeTask(taskId: String) {
        tasks[taskId]?.let { task ->
            if (task.status == DownloadStatus.PAUSED) {
                Log.d(
                    TAG,
                    "Resuming task $taskId, supportsRange: ${task.supportsRange}, downloadedBytes: ${task.downloadedBytes}"
                )
                if (!task.supportsRange && task.downloadedBytes > 0) {
                    try {
                        File(task.filePath).delete()
                        task.downloadedBytes = 0
                        task.progress = 0
                        notifyError(
                            taskId,
                            "Server does not support Range requests, restarting download"
                        )
                        Log.w(
                            TAG,
                            "Task $taskId: Server does not support Range, restarting download"
                        )
                    } catch (e: Exception) {
                        notifyError(taskId, "Failed to delete partial file: ${e.message}")
                        Log.e(TAG, "Task $taskId: Failed to delete partial file", e)
                        return
                    }
                }
                executeDownload(task)
            } else {
                Log.w(TAG, "Cannot resume task $taskId, current status: ${task.status}")
            }
        } ?: Log.w(TAG, "Task $taskId not found for resume")
    }

    // 取消下载任务
    fun cancelTask(taskId: String) {
        calls[taskId]?.cancel()
        tasks[taskId]?.let {
            it.status = DownloadStatus.CANCELLED
            notifyStatusChanged(it.taskId, DownloadStatus.CANCELLED)
            Log.d(TAG, "Cancelled task $taskId")
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
        Log.d(TAG, "Cancelled all tasks")
    }

    // 获取任务状态
    fun getTaskStatus(taskId: String): DownloadStatus? {
        return tasks[taskId]?.status
    }

    // 检查服务器是否支持Range请求
    private fun checkRangeSupportAndExecute(task: DownloadTask) {
        executor.execute {
            try {
                val headRequest = Request.Builder().url(task.url).head().build()
                val call = client.newCall(headRequest)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        task.status = DownloadStatus.FAILED
                        notifyError(task.taskId, "Failed to check Range support: ${e.message}")
                        Log.e(TAG, "Task ${task.taskId}: Failed to check Range support", e)
                        cleanupTask(task.taskId)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        task.supportsRange = response.header("Accept-Ranges") == "bytes"
                        Log.d(TAG, "Task ${task.taskId}: Range support = ${task.supportsRange}")
                        response.close()
                        executeDownload(task)
                    }
                })
            } catch (e: Exception) {
                task.status = DownloadStatus.FAILED
                notifyError(task.taskId, "Failed to check Range support: ${e.message}")
                Log.e(TAG, "Task ${task.taskId}: Failed to check Range support", e)
                cleanupTask(task.taskId)
            }
        }
    }

    /**
     * 执行下载任务
     * @param task 下载任务
     */
    private fun executeDownload(task: DownloadTask) {
        task.status = DownloadStatus.QUEUED
        notifyStatusChanged(task.taskId, DownloadStatus.QUEUED)
        Log.d(TAG, "Task ${task.taskId}: Queued")

        executor.execute {
            try {
                val requestBuilder = Request.Builder().url(task.url)
                if (task.supportsRange && task.downloadedBytes > 0) {
                    requestBuilder.addHeader("Range", "bytes=${task.downloadedBytes}-")
                    Log.d(
                        TAG,
                        "Task ${task.taskId}: Resuming with Range: bytes=${task.downloadedBytes}-"
                    )
                }
                val request = requestBuilder.build()
                val call = client.newCall(request)
                calls[task.taskId] = call

                task.status = DownloadStatus.DOWNLOADING
                notifyStatusChanged(task.taskId, DownloadStatus.DOWNLOADING)
                Log.d(TAG, "Task ${task.taskId}: Downloading")

                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (call.isCanceled() && task.status == DownloadStatus.PAUSED) {
                            Log.d(TAG, "Task ${task.taskId}: Paused")
                            return
                        }
                        task.status = DownloadStatus.FAILED
                        notifyError(task.taskId, e.message ?: "Download failed")
                        Log.e(TAG, "Task ${task.taskId}: Download failed", e)
                        cleanupTask(task.taskId)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            task.status = DownloadStatus.FAILED
                            notifyError(task.taskId, "HTTP error: ${response.code}")
                            Log.e(TAG, "Task ${task.taskId}: HTTP error ${response.code}")
                            cleanupTask(task.taskId)
                            response.close()
                            return
                        }

                        response.body?.let { body ->
                            try {
                                val totalBytes =
                                    if (task.supportsRange && task.downloadedBytes > 0) {
                                        body.contentLength() + task.downloadedBytes
                                    } else {
                                        body.contentLength()
                                    }
                                task.totalBytes = totalBytes
                                var downloadedBytes = task.downloadedBytes
                                val bufferSize =
                                    if (task.totalBytes > 1024 * 1024) 16 * 1024 else 8 * 1024
                                val buffer = ByteArray(bufferSize)
                                var bytesRead: Int


                                val file = File(task.filePath)
                                if (file.freeSpace < task.totalBytes - task.downloadedBytes) {
                                    throw IOException("Insufficient disk space for ${task.filePath}")
                                }
                                val parentDir = file.parentFile
                                if (parentDir != null) {
                                    if (!parentDir.exists() && !parentDir.mkdirs()) {
                                        throw IOException("Failed to create parent directories for ${task.filePath}")
                                    }
                                } else {
                                    throw IOException("File has no parent directory: ${task.filePath}")
                                }
                                if (!file.canWrite() && !file.createNewFile()) {
                                    throw IOException("Cannot write to file ${task.filePath}")
                                }

                                FileOutputStream(
                                    task.filePath,
                                    task.supportsRange && task.downloadedBytes > 0
                                ).use { output ->
                                    body.byteStream().use { input ->
                                        while (input.read(buffer).also { bytesRead = it } != -1) {
                                            if (call.isCanceled() && task.status == DownloadStatus.PAUSED) {
                                                Log.d(
                                                    TAG,
                                                    "Task ${task.taskId}: Paused during download"
                                                )
                                                return
                                            }
                                            if (call.isCanceled()) {
                                                task.status = DownloadStatus.CANCELLED
                                                notifyStatusChanged(
                                                    task.taskId,
                                                    DownloadStatus.CANCELLED
                                                )
                                                Log.d(
                                                    TAG,
                                                    "Task ${task.taskId}: Cancelled during download"
                                                )
                                                return
                                            }
                                            output.write(buffer, 0, bytesRead)
                                            downloadedBytes += bytesRead
                                            task.downloadedBytes = downloadedBytes
                                            if (totalBytes > 0) {
                                                val newProgress =
                                                    ((downloadedBytes.toLong() * 100) / totalBytes).toInt()
                                                if (newProgress > task.progress + 1) { // 仅当进度变化超过 1% 时通知
                                                    task.progress = newProgress
                                                    notifyProgress(
                                                        task.taskId,
                                                        task.progress,
                                                        downloadedBytes,
                                                        totalBytes
                                                    )
                                                    Log.d(
                                                        TAG,
                                                        "Task ${task.taskId}: Progress ${task.progress}% ($downloadedBytes/$totalBytes)"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                task.status = DownloadStatus.COMPLETED
                                notifyStatusChanged(task.taskId, DownloadStatus.COMPLETED)
                                Log.d(TAG, "Task ${task.taskId}: Completed")
                                cleanupTask(task.taskId)
                            } catch (e: Exception) {
                                task.status = DownloadStatus.FAILED
                                notifyError(task.taskId, e.message ?: "Download failed")
                                Log.e(TAG, "Task ${task.taskId}: Download failed", e)
                                cleanupTask(task.taskId)
                            } finally {
                                response.close()
                            }
                        } ?: run {
                            /**
                             * 某些服务器可能在 Range 请求时返回 206 且 body 为空，这不一定是错误
                             */
                            if (response.code == 206 && task.supportsRange) {
                                // 可能是合法的空范围响应，检查是否需要特殊处理
                                Log.d(
                                    TAG,
                                    "Task ${task.taskId}: Empty body with 206, checking range"
                                )
                                task.status = DownloadStatus.COMPLETED
                                notifyStatusChanged(task.taskId, DownloadStatus.COMPLETED)
                            } else {
                                task.status = DownloadStatus.FAILED
                                notifyError(task.taskId, "Empty response body")
                                Log.e(TAG, "Task ${task.taskId}: Empty response body")
                            }
                            cleanupTask(task.taskId)
                        }
                    }
                })
            } catch (e: Exception) {
                task.status = DownloadStatus.FAILED
                notifyError(task.taskId, e.message ?: "Download failed")
                Log.e(TAG, "Task ${task.taskId}: Download failed", e)
                cleanupTask(task.taskId)
            }
        }
    }

    /**
     * 清理任务相关的资源
     * @param taskId 任务ID
     */
    private fun cleanupTask(taskId: String) {
        calls.remove(taskId)
        tasks.remove(taskId)
        Log.d(TAG, "Task $taskId: Cleaned up")
    }

    /**
     * 通知下载进度
     * @param taskId 任务ID
     * @param progress 下载进度
     * @param downloadedBytes 已下载字节数
     * @param totalBytes 总字节数
     */
    private fun notifyProgress(
        taskId: String,
        progress: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
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
        Log.d(TAG, "DownloadManager shutdown")
    }
}

