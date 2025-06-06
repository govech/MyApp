package com.example.myapp

import com.example.newload.DownloadCallback
import com.example.newload.DownloadManager
import com.example.newload.DownloadStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.UUID

data class DownloadEvent(
    val taskId: String,
    val state: DownloadState
)

sealed class DownloadState {
    data class Progress(val progress: Double, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    data class Status(val status: DownloadStatus) : DownloadState()
    data class Error(val error: String) : DownloadState()
}

class DownloadRepository {
    private val downloadManager = DownloadManager.getInstance()
    private val eventChannel = Channel<DownloadEvent>(Channel.BUFFERED)
    private val eventFlow: Flow<DownloadEvent> = eventChannel.receiveAsFlow()

    fun getDownloadEvents(): Flow<DownloadEvent> = eventFlow

    fun addTask(url: String, filePath: String): String {
        val taskId = UUID.randomUUID().toString()
        downloadManager.addTask(url, filePath, taskId, object : DownloadCallback {
            override fun onProgress(taskId: String, progress: Double, downloadedBytes: Long, totalBytes: Long) {
                eventChannel.trySend(DownloadEvent(taskId, DownloadState.Progress(progress, downloadedBytes, totalBytes)))
            }

            override fun onStatusChanged(taskId: String, status: DownloadStatus) {
                eventChannel.trySend(DownloadEvent(taskId, DownloadState.Status(status)))
            }

            override fun onError(taskId: String, error: String) {
                eventChannel.trySend(DownloadEvent(taskId, DownloadState.Error(error)))
            }
        })
        return taskId
    }

    fun pauseTask(taskId: String) {
        downloadManager.pauseTask(taskId)
    }

    fun resumeTask(taskId: String) {
        downloadManager.resumeTask(taskId)
    }

    fun cancelTask(taskId: String) {
        downloadManager.cancelTask(taskId)
    }

    fun shutdown() {
        downloadManager.shutdown()
    }
}