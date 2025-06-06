package com.example.newload

import android.util.Log
import okhttp3.Call
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages download tasks, including adding, pausing, resuming, and canceling tasks.
 */
class TaskManager private constructor() {
    companion object {
        @Volatile
        private var instance: TaskManager? = null

        fun getInstance(): TaskManager {
            return instance ?: synchronized(this) {
                instance ?: TaskManager().also { instance = it }
            }
        }

        private const val TAG = "TaskManager"
    }

    val tasks = ConcurrentHashMap<String, DownloadTask>()
    val callbacks = ConcurrentHashMap<String, DownloadCallback>()
    private val calls = ConcurrentHashMap<String, Call>()

    fun addTask(
        url: String,
        filePath: String,
        taskId: String,
        callback: DownloadCallback
    ): Boolean {
        if (tasks.containsKey(taskId)) {
            callback.onError(taskId, "Task ID already exists")
            Log.e(TAG, "Task ID $taskId already exists")
            return false
        }

        val task = DownloadTask(url, filePath, taskId)
        tasks[taskId] = task
        callbacks[taskId] = callback
        Log.d(TAG, "Adding task $taskId with URL: ${task.url}")
        return true
    }

    fun pauseTask(taskId: String): Boolean {
        tasks[taskId]?.let { task ->
            if (task.status == DownloadStatus.DOWNLOADING) {
                calls[taskId]?.cancel()
                task.status = DownloadStatus.PAUSED
                Log.d(TAG, "Paused task $taskId, downloadedBytes: ${task.downloadedBytes}")
                return true
            } else {
                Log.w(TAG, "Cannot pause task $taskId, current status: ${task.status}")
                return false
            }
        } ?: run {
            Log.w(TAG, "Task $taskId not found for pause")
            return false
        }
    }

    fun resumeTask(taskId: String): DownloadTask? {
        tasks[taskId]?.let { task ->
            if (task.status == DownloadStatus.PAUSED) {
                Log.d(
                    TAG,
                    "Resuming task $taskId, supportsRange: ${task.supportsRange}, downloadedBytes: ${task.downloadedBytes}"
                )
                return task
            } else {
                Log.w(TAG, "Cannot resume task $taskId, current status: ${task.status}")
                return null
            }
        } ?: run {
            Log.w(TAG, "Task $taskId not found for resume")
            return null
        }
    }

    fun cancelTask(taskId: String) {
        calls[taskId]?.cancel()
        tasks[taskId]?.let {
            it.status = DownloadStatus.CANCELLED
            Log.d(TAG, "Cancelled task $taskId")
        }
        cleanupTask(taskId)
    }

    fun cancelAllTasks() {
        calls.forEach { it.value.cancel() }
        tasks.forEach { it.value.status = DownloadStatus.CANCELLED }
        tasks.clear()
        calls.clear()
        Log.d(TAG, "Cancelled all tasks")
    }

    fun getTaskStatus(taskId: String): DownloadStatus? {
        return tasks[taskId]?.status
    }

    fun getTask(taskId: String): DownloadTask? {
        return tasks[taskId]
    }

    fun addCall(taskId: String, call: Call) {
        calls[taskId] = call
    }

    fun cleanupTask(taskId: String) {
        calls.remove(taskId)
        tasks.remove(taskId)
        Log.d(TAG, "Task $taskId: Cleaned up")
    }
}