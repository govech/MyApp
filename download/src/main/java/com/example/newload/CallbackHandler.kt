package com.example.newload

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Handles callback notifications for download tasks.
 */
class CallbackHandler private constructor() {
    companion object {
        @Volatile
        private var instance: CallbackHandler? = null

        fun getInstance(): CallbackHandler {
            return instance ?: synchronized(this) {
                instance ?: CallbackHandler().also { instance = it }
            }
        }

        private const val TAG = "CallbackHandler"
    }

    private val handler = Handler(Looper.getMainLooper())

    fun notifyProgress(
        taskId: String,
        progress: Double,
        downloadedBytes: Long,
        totalBytes: Long,
        callback: DownloadCallback?
    ) {
        handler.post {
            callback?.onProgress(taskId, progress, downloadedBytes, totalBytes)
            Log.d(TAG, "Notified progress for $taskId: $progress%  ----$downloadedBytes/$totalBytes")
        }
    }

    fun notifyStatusChanged(taskId: String, status: DownloadStatus, callback: DownloadCallback?) {
        handler.post {
            callback?.onStatusChanged(taskId, status)
            Log.d(TAG, "Notified status change for $taskId: $status")
        }
    }

    fun notifyError(taskId: String, error: String, callback: DownloadCallback?) {
        handler.post {
            callback?.onError(taskId, error)
            Log.e(TAG, "Notified error for $taskId: $error")
        }
    }
}