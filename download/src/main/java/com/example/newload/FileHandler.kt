package com.example.newload

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import okio.IOException

/**
 * Handles file operations for downloading files.
 */
class FileHandler {
    companion object {
        private const val TAG = "FileHandler"
    }

    fun prepareFile(task: DownloadTask): Boolean {
        try {
            val file = File(task.filePath)
            if (StorageUtils.getAvailableInternalStorageSize() < task.totalBytes - task.downloadedBytes) {
                throw IOException("Insufficient disk space for ${task.filePath}   AvailableInternalStorageSize=${StorageUtils.getAvailableInternalStorageSize()}")
            }
            Log.d(TAG, "Task ${task.taskId}: getAvailableInternalStorageSize=${StorageUtils.getAvailableInternalStorageSize()}")

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
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Task ${task.taskId}: File preparation failed", e)
            throw e
        }
    }

    fun writeToFile(
        task: DownloadTask,
        inputStream: java.io.InputStream,
        onProgress: (Long, Double) -> Unit,
        onComplete: () -> Unit
    ) {
        try {
            val bufferSize =
                if (task.totalBytes > 1024 * 1024) 16 * 1024 else 8 * 1024 // 根据文件大小选择合适的缓冲区大小
            val buffer = ByteArray(bufferSize)
            var bytesRead: Int
            var downloadedBytes = task.downloadedBytes
            var lastUpdateTimestamp = System.currentTimeMillis()
            FileOutputStream(
                task.filePath,
                task.supportsRange && task.downloadedBytes > 0
            ).use { output ->
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    // 检查任务是否暂停或取消
                    if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.CANCELLED) {
                        Log.d(TAG, "Task ${task.taskId}: Stopped writing due to ${task.status}")
                        return
                    }
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    task.downloadedBytes = downloadedBytes
                    if (task.totalBytes > 0) {

                        val newProgress = String.format("%.2f", (downloadedBytes.toLong() * 100.0) / task.totalBytes).toDouble()

                        /**
                         * 当进度变化大于1%或者当前时间间隔超过1秒时，更新进度
                         */
                        val currentTimestamp = System.currentTimeMillis()
                        if (newProgress > task.progress + 1 || currentTimestamp - lastUpdateTimestamp >= 1000) {
                            task.progress = newProgress
                            onProgress(downloadedBytes, newProgress)
                            Log.d(
                                TAG,
                                "writeToFile---Task ${task.taskId}: Progress ${task.progress}% ($downloadedBytes/${task.totalBytes})"
                            )
                            lastUpdateTimestamp = currentTimestamp // 更新最后更新时间
                        }

                    }
                }
                // 文件写入完成，调用 onComplete
                if (task.status != DownloadStatus.PAUSED && task.status != DownloadStatus.CANCELLED) {
                    Log.d(TAG, "Task ${task.taskId}: File write completed")
                    onComplete()
                }
            }
        } catch (e: Exception) {
            if (e is okhttp3.internal.http2.StreamResetException && (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.CANCELLED)) {
                Log.e(TAG, "Task ${task.taskId}: Ignored StreamResetException due to PAUSED or CANCELLED state")
                return
            }
            Log.e(TAG, "Task ${task.taskId}: File write failed", e)
            throw e
        }
    }

    fun deletePartialFile(task: DownloadTask): Boolean {
        try {
            File(task.filePath).delete()
            task.downloadedBytes = 0
            task.progress = 0.0
            Log.d(TAG, "Task ${task.taskId}: Deleted partial file")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Task ${task.taskId}: Failed to delete partial file", e)
            return false
        }
    }
}