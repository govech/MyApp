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
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Task ${task.taskId}: File preparation failed", e)
            throw e
        }
    }

    fun writeToFile(
        task: DownloadTask,
        inputStream: java.io.InputStream,
        onProgress: (Long, Int) -> Unit
    ) {
        try {
            val bufferSize = if (task.totalBytes > 1024 * 1024) 16 * 1024 else 8 * 1024
            val buffer = ByteArray(bufferSize)
            var bytesRead: Int
            var downloadedBytes = task.downloadedBytes
            var lastUpdateTimestamp = System.currentTimeMillis()
            FileOutputStream(
                task.filePath,
                task.supportsRange && task.downloadedBytes > 0
            ).use { output ->
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    task.downloadedBytes = downloadedBytes
                    if (task.totalBytes > 0) {
                        val newProgress = ((downloadedBytes.toLong() * 100) / task.totalBytes).toInt()
                        /**
                         * 当进度变化大于1%或者当前时间间隔超过1秒时，更新进度
                         */
                        val currentTimestamp = System.currentTimeMillis()
                        if (newProgress > task.progress + 1 || currentTimestamp - lastUpdateTimestamp >= 1000) {
                            task.progress = newProgress
                            onProgress(downloadedBytes, newProgress)
                            Log.d(TAG, "Task ${task.taskId}: Progress ${task.progress}% ($downloadedBytes/${task.totalBytes})")
                            lastUpdateTimestamp = currentTimestamp // 更新最后更新时间
                        }

                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Task ${task.taskId}: File write failed", e)
            throw e
        }
    }

    fun deletePartialFile(task: DownloadTask): Boolean {
        try {
            File(task.filePath).delete()
            task.downloadedBytes = 0
            task.progress = 0
            Log.d(TAG, "Task ${task.taskId}: Deleted partial file")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Task ${task.taskId}: Failed to delete partial file", e)
            return false
        }
    }
}