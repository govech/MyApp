package com.example.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class DownloadHelper(private val context: Context) {
    private val downloadManager: DownloadManager = context.getSystemService()!!
    private var downloadId: Long = -1
    private var receiver: BroadcastReceiver? = null
    private var progressJob: Job? = null
    private val _progressFlow = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val progressFlow: StateFlow<DownloadStatus> = _progressFlow

    sealed class DownloadStatus {
        object Idle : DownloadStatus()
        object Connecting : DownloadStatus()
        data class Progress(val percentage: Int) : DownloadStatus()
        data class Completed(val uri: Uri?) : DownloadStatus()
        data class Failed(val errorCode: Int, val message: String) : DownloadStatus()
        data class UnknownSizeProgress(val downloadedBytes: Long) : DownloadStatus()

    }

    data class DownloadConfig(
        var url: String = "",
        var title: String = "文件下载",
        var description: String = "正在下载文件",
        var fileName: String = "downloaded_file",
        var mimeType: String? = null,
        var networkTypes: Int = DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE,
        var notificationVisibility: Int = DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
        var destinationDir: String = Environment.DIRECTORY_DOWNLOADS
    )

    private var onComplete: ((Uri?) -> Unit)? = null
    private var onFailed: ((Int, String) -> Unit)? = null

    fun download(config: DownloadConfig.() -> Unit): DownloadHelper {
        cancel() // 取消现有任务
        val downloadConfig = DownloadConfig().apply(config)
        val request = DownloadManager.Request(Uri.parse(downloadConfig.url)).apply {
            setTitle(downloadConfig.title)
            setDescription(downloadConfig.description)
            setNotificationVisibility(downloadConfig.notificationVisibility)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    downloadConfig.fileName
                )
            } else {
                setDestinationInExternalPublicDir(
                    downloadConfig.destinationDir,
                    downloadConfig.fileName
                )
            }
            setAllowedNetworkTypes(downloadConfig.networkTypes)
            downloadConfig.mimeType?.let { setMimeType(it) }
        }

        downloadId = downloadManager.enqueue(request)
        Log.d("DownloadHelper", "Started download with ID: $downloadId")
        registerReceiver()
        startProgressMonitoring()
        return this
    }

    fun onComplete(callback: (Uri?) -> Unit): DownloadHelper {
        onComplete = callback
        return this
    }

    fun onFailed(callback: (Int, String) -> Unit): DownloadHelper {
        onFailed = callback
        return this
    }

    private fun startProgressMonitoring() {
        progressJob?.cancel()
        _progressFlow.value = DownloadStatus.Connecting
        progressJob = CoroutineScope(Dispatchers.IO).launch {
            var isActive = true
            val startTime = System.currentTimeMillis()
            val timeoutMs = 5_000L // 10秒超时

            var lastProgressTime = System.currentTimeMillis()
            var lastDownloadedBytes = 0L
            while (isActive) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query).use { cursor ->
                    if (!cursor.moveToFirst()) {
                        Log.w(
                            "DownloadHelper",
                            "Query returned empty cursor for downloadId: $downloadId"
                        )
                        _progressFlow.value =
                            DownloadStatus.Failed(-1, "Empty cursor, download may not exist")
                        onFailed?.invoke(-1, "Empty cursor")
                        isActive = false
                        return@launch
                    }

                    val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusColumnIndex < 0) {
                        Log.e("DownloadHelper", "COLUMN_STATUS not found in cursor")
                        _progressFlow.value = DownloadStatus.Failed(-1, "COLUMN_STATUS not found")
                        onFailed?.invoke(-1, "COLUMN_STATUS not found")
                        isActive = false
                        return@launch
                    }

                    val status = cursor.getInt(statusColumnIndex)
                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            val downloadedColumnIndex =
                                cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalColumnIndex =
                                cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            Log.d(
                                "DownloadHelper",
                                "Download running  totalColumnIndex=$totalColumnIndex  --- downloadedColumnIndex=$downloadedColumnIndex"
                            )
                            if (downloadedColumnIndex < 0 || totalColumnIndex < 0) {
                                Log.e(
                                    "DownloadHelper",
                                    "Missing columns: downloaded=$downloadedColumnIndex, total=$totalColumnIndex"
                                )
                                _progressFlow.value = DownloadStatus.Connecting
                            } else {
                                val downloaded = cursor.getLong(downloadedColumnIndex)
                                val total = cursor.getLong(totalColumnIndex)
                                if (downloaded > lastDownloadedBytes) {
                                    lastProgressTime = System.currentTimeMillis() // 重置超时计时器
                                    lastDownloadedBytes = downloaded
                                    Log.d("DownloadHelper", "Progress updated, resetting timeout")
                                }
                                if (total > 0) {
                                    val progress = (downloaded * 100 / total).toInt()
                                    _progressFlow.value = DownloadStatus.Progress(progress)
                                    Log.d(
                                        "DownloadHelper",
                                        "Progress: $progress% (Downloaded: $downloaded, Total: $total)"
                                    )
                                } else {

                                    _progressFlow.value =
                                        DownloadStatus.UnknownSizeProgress(downloaded)
                                    Log.d(
                                        "DownloadHelper",
                                        "Total size unknown, downloaded: $downloaded bytes"
                                    )
                                }
                            }
                        }

                        else -> {
                            Log.d("DownloadHelper", "Status: $status")
                            _progressFlow.value = DownloadStatus.Connecting
                        }
                    }
                    // 检查超时：仅在非活跃状态（非 STATUS_RUNNING）或无进度更新时触发
                    if (status != DownloadManager.STATUS_RUNNING ||
                        (status == DownloadManager.STATUS_RUNNING && System.currentTimeMillis() - lastProgressTime > timeoutMs)) {
                        if (System.currentTimeMillis() - lastProgressTime > timeoutMs) {
                            Log.w("DownloadHelper", "Download timed out for downloadId: $downloadId, status: $status")
                            _progressFlow.value = DownloadStatus.Failed(-1, "下载超时: 无进展")
                            onFailed?.invoke(-1, "下载超时: 无进展")
                            isActive = false
                            return@launch
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    private fun registerReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    progressJob?.cancel()
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    downloadManager.query(query).use { cursor ->
                        if (!cursor.moveToFirst()) {
                            Log.w(
                                "DownloadHelper",
                                "Query returned empty cursor for downloadId: $downloadId"
                            )
                            _progressFlow.value =
                                DownloadStatus.Failed(-1, "Empty cursor in broadcast")
                            onFailed?.invoke(-1, "Empty cursor in broadcast")
                            unregisterReceiver()
                            return
                        }

                        val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusColumnIndex < 0) {
                            Log.e("DownloadHelper", "COLUMN_STATUS not found in broadcast cursor")
                            _progressFlow.value =
                                DownloadStatus.Failed(-1, "COLUMN_STATUS not found")
                            onFailed?.invoke(-1, "COLUMN_STATUS not found")
                            unregisterReceiver()
                            return
                        }

                        val status = cursor.getInt(statusColumnIndex)
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                val uri = downloadManager.getUriForDownloadedFile(downloadId)
                                _progressFlow.value = DownloadStatus.Completed(uri)
                                onComplete?.invoke(uri)
                                Log.d("DownloadHelper", "Download completed in broadcast: $uri")
                            }

                            DownloadManager.STATUS_FAILED -> {
                                val reasonColumnIndex =
                                    cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                val reason =
                                    if (reasonColumnIndex >= 0) cursor.getInt(reasonColumnIndex) else -1
                                val message = getReasonMessage(reason)
                                _progressFlow.value = DownloadStatus.Failed(reason, message)
                                onFailed?.invoke(reason, message)
                                Log.w(
                                    "DownloadHelper",
                                    "Download failed in broadcast, reason: $message ($reason)"
                                )
                            }

                            else -> {
                                Log.w("DownloadHelper", "Unexpected status in broadcast: $status")
                            }
                        }
                    }
                    unregisterReceiver()
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun getReasonMessage(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "文件已存在"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "存储空间不足"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "网络数据错误"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "未处理的 HTTP 状态码"
            DownloadManager.ERROR_UNKNOWN -> "未知错误"
            else -> "Unknown error: $reason"
        }
    }

    fun cancel() {
        progressJob?.cancel()
        downloadManager.remove(downloadId)
        _progressFlow.value = DownloadStatus.Idle
        unregisterReceiver()
        Log.d("DownloadHelper", "Download cancelled, ID: $downloadId")
    }

    private fun unregisterReceiver() {
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }
}