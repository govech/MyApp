package com.example.newload

import android.util.Log
import com.google.android.material.color.utilities.MaterialDynamicColors.onError


class DownloadManager private constructor() {
    companion object {
        @Volatile
        private var instance: DownloadManager? = null

        fun getInstance(): DownloadManager {
            return instance ?: synchronized(this) {
                instance ?: DownloadManager().also { instance = it }
            }
        }

        private const val TAG = "DownloadManager"
    }

    private val taskManager = TaskManager.getInstance()
    private val networkHandler = NetworkHandler.getInstance()
    private val fileHandler = FileHandler()
    private val callbackHandler = CallbackHandler.getInstance()

    fun addTask(url: String, filePath: String, taskId: String, callback: DownloadCallback) {
        if (taskManager.addTask(url, filePath, taskId, callback)) {
            val task = taskManager.getTask(taskId)!!
            checkRangeSupportAndExecute(task)
        }
    }

    fun pauseTask(taskId: String) {
        if (taskManager.pauseTask(taskId)) {
            callbackHandler.notifyStatusChanged(
                taskId,
                DownloadStatus.PAUSED,
                taskManager.getTask(taskId)?.let { taskManager.callbacks[taskId] })
        }
    }

    fun resumeTask(taskId: String) {
        taskManager.resumeTask(taskId)?.let { task ->
            if (!task.supportsRange && task.downloadedBytes > 0) {
                if (!fileHandler.deletePartialFile(task)) {
                    callbackHandler.notifyError(
                        taskId,
                        "Failed to delete partial file",
                        taskManager.callbacks[taskId]
                    )
                    return
                }
                callbackHandler.notifyError(
                    taskId,
                    "Server does not support Range requests, restarting download",
                    taskManager.callbacks[taskId]
                )
            }
            executeDownload(task)
        }
    }

    fun cancelTask(taskId: String) {
        taskManager.cancelTask(taskId)
        callbackHandler.notifyStatusChanged(
            taskId,
            DownloadStatus.CANCELLED,
            taskManager.callbacks[taskId]
        )
    }

    fun cancelAllTasks() {
        taskManager.cancelAllTasks()
        taskManager.tasks.keys().toList().forEach {
            callbackHandler.notifyStatusChanged(
                it,
                DownloadStatus.CANCELLED,
                taskManager.callbacks[it]
            )
        }
    }

    fun getTaskStatus(taskId: String): DownloadStatus? {
        return taskManager.getTaskStatus(taskId)
    }

    private fun checkRangeSupportAndExecute(task: DownloadTask) {
        networkHandler.checkRangeSupport(
            task,
            onResult = { supportsRange ->
                task.supportsRange = supportsRange
                executeDownload(task)
            },
            onError = { error ->
                task.status = DownloadStatus.FAILED
                callbackHandler.notifyError(task.taskId, error, taskManager.callbacks[task.taskId])
                taskManager.cleanupTask(task.taskId)
            }
        )
    }

    private fun executeDownload(task: DownloadTask) {
        task.status = DownloadStatus.QUEUED
        callbackHandler.notifyStatusChanged(
            task.taskId,
            DownloadStatus.QUEUED,
            taskManager.callbacks[task.taskId]
        )

        networkHandler.executeDownload(
            task,
            onStart = {
                task.status = DownloadStatus.DOWNLOADING
                callbackHandler.notifyStatusChanged(
                    task.taskId,
                    DownloadStatus.DOWNLOADING,
                    taskManager.callbacks[task.taskId]
                )
            },
            onProgress = label@{ downloadedBytes, totalBytes, inputStream ->
                // 检查任务状态，暂停或取消时不执行文件写入
                if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.CANCELLED) {
                    Log.d(TAG, "Task ${task.taskId}: Skipped file write due to ${task.status}")
                    return@label
                }
                try {
                    fileHandler.prepareFile(task)
                    fileHandler.writeToFile(
                        task, inputStream,
                        onProgress = { newDownloadedBytes, newProgress ->
                            callbackHandler.notifyProgress(
                                task.taskId,
                                newProgress,
                                newDownloadedBytes,
                                totalBytes,
                                taskManager.callbacks[task.taskId]
                            )
                        },
                        onComplete = {
                            callbackHandler.notifyStatusChanged(
                                task.taskId,
                                DownloadStatus.COMPLETED,
                                taskManager.callbacks[task.taskId]
                            )
                        })

                } catch (e: Exception) {
                    // 仅在任务未暂停或取消时标记为失败
                    if (task.status != DownloadStatus.PAUSED && task.status != DownloadStatus.CANCELLED) {
                        task.status = DownloadStatus.FAILED
                        callbackHandler.notifyError(
                            task.taskId,
                            e.message ?: "File operation failed",
                            taskManager.callbacks[task.taskId]
                        )
                        taskManager.cleanupTask(task.taskId)
                    }


                }
            },
            onComplete = {
                task.status = DownloadStatus.COMPLETED
                callbackHandler.notifyStatusChanged(
                    task.taskId,
                    DownloadStatus.COMPLETED,
                    taskManager.callbacks[task.taskId]
                )
                taskManager.cleanupTask(task.taskId)
            },
            onError = { error ->
                task.status = DownloadStatus.FAILED
                callbackHandler.notifyError(task.taskId, error, taskManager.callbacks[task.taskId])
                taskManager.cleanupTask(task.taskId)
            },
            addCall = { call ->
                taskManager.addCall(task.taskId, call)
            }
        )
    }

    fun shutdown() {
        taskManager.cancelAllTasks()
        networkHandler.shutdown()
        Log.d(TAG, "DownloadManager shutdown")
    }
}