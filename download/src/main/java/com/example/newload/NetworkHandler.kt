package com.example.newload

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Handles network operations for downloading files.
 */
class NetworkHandler private constructor() {
    companion object {
        @Volatile
        private var instance: NetworkHandler? = null

        fun getInstance(): NetworkHandler {
            return instance ?: synchronized(this) {
                instance ?: NetworkHandler().also { instance = it }
            }
        }

        private const val TAG = "NetworkHandler"
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val executor: ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    )

    fun checkRangeSupport(
        task: DownloadTask,
        onResult: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            val headRequest = Request.Builder().url(task.url).head().build()
            val call = client.newCall(headRequest)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Task ${task.taskId}: Failed to check Range support", e)
                    onError("Failed to check Range support: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val acceptRanges = response.header("Accept-Ranges")
                        val supportRange = acceptRanges == "bytes"
                        val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
                        if (supportRange && contentLength <= 0) {
                            throw IOException("Invalid Content-Length: $contentLength")
                        }
                        onResult(supportRange)
                        Log.d(TAG, "Task ${task.taskId}: Range support = $supportRange")
                    } finally {
                        response.close()
                        call.cancel()
                    }
                }
            })
        }

    }

    fun executeDownload(
        task: DownloadTask,
        onStart: () -> Unit,
        onProgress: (Long, Long, java.io.InputStream) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
        addCall: (Call) -> Unit
    ) {
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
                addCall(call)

                onStart()
                Log.d(TAG, "Task ${task.taskId}: Downloading")

                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (call.isCanceled() && task.status == DownloadStatus.PAUSED) {
                            Log.d(TAG, "Task ${task.taskId}: Paused")
                            return
                        }
                        Log.e(TAG, "Task ${task.taskId}: Download failed", e)
                        onError(e.message ?: "Download failed")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            Log.e(TAG, "Task ${task.taskId}: HTTP error ${response.code}")
                            onError("HTTP error: ${response.code}")
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
                                onProgress(task.downloadedBytes, totalBytes, body.byteStream())
                            } catch (e: Exception) {
                                Log.e(TAG, "Task ${task.taskId}: Download failed", e)
                                onError(e.message ?: "Download failed")
                            } finally {
                                response.close()
                            }
                        } ?: run {
                            if (response.code == 206 && task.supportsRange) {
                                Log.d(
                                    TAG,
                                    "Task ${task.taskId}: Empty body with 206, checking range"
                                )
                                onComplete()
                            } else {
                                Log.e(TAG, "Task ${task.taskId}: Empty response body")
                                onError("Empty response body")
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Task ${task.taskId}: Download failed", e)
                onError(e.message ?: "Download failed")
            }
        }
    }

    fun shutdown() {
        executor.shutdown()
        Log.d(TAG, "NetworkHandler shutdown")
    }
}