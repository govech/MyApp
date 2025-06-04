package com.example.download.downloadmanage

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

class DownloadHelper(private val context: Context) {
    private val downloadManager: DownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private var downloadId: Long = -1
    private var receiver: BroadcastReceiver? = null
    private var isReceiverRegistered = false

    private var onComplete: ((Uri?) -> Unit)? = null
    private var onFailed: ((Int, String) -> Unit)? = null

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

    fun download(config: DownloadConfig.() -> Unit): DownloadHelper {
        cancel() // 取消之前的下载
        val downloadConfig = DownloadConfig().apply(config)
        val request = DownloadManager.Request(Uri.parse(downloadConfig.url)).apply {
            setTitle(downloadConfig.title)
            setDescription(downloadConfig.description)
            setNotificationVisibility(downloadConfig.notificationVisibility)
            setAllowedNetworkTypes(downloadConfig.networkTypes)
            downloadConfig.mimeType?.let { setMimeType(it) }
            setDestinationInExternalPublicDir(
                downloadConfig.destinationDir,
                downloadConfig.fileName
            )
        }

        downloadId = downloadManager.enqueue(request)
        registerReceiver()
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

    private fun registerReceiver() {
        if (isReceiverRegistered) return
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    downloadManager.query(query).use { cursor ->
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (statusIndex >= 0) {
                                when (cursor.getInt(statusIndex)) {
                                    DownloadManager.STATUS_SUCCESSFUL -> {
                                        val uri =
                                            downloadManager.getUriForDownloadedFile(downloadId)
                                        onComplete?.invoke(uri)
                                    }

                                    DownloadManager.STATUS_FAILED -> {
                                        val reasonIndex =
                                            cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                        val reason =
                                            if (reasonIndex >= 0) cursor.getInt(reasonIndex) else -1
                                        onFailed?.invoke(reason, getReasonMessage(reason))
                                    }

                                    DownloadManager.STATUS_RUNNING -> {
                                        // 可选：日志输出或忽略
                                        Log.d("Download", "正在下载...")
                                    }

                                    DownloadManager.STATUS_PAUSED -> {
                                        Log.d("Download", "下载已暂停")
                                    }

                                    DownloadManager.STATUS_PENDING -> {
                                        Log.d("Download", "下载排队中")
                                    }

                                    else -> {
                                        onFailed?.invoke(-1, "未知下载状态")
                                    }
                                }
                            } else {
                                onFailed?.invoke(-1, "下载状态未知：找不到 COLUMN_STATUS 列")
                            }


                        } else {
                            onFailed?.invoke(-1, "下载失败，未找到记录")
                        }
                    }
                    unregisterReceiver()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )

        }
        isReceiverRegistered = true
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered && receiver != null) {
            context.unregisterReceiver(receiver)
            receiver = null
            isReceiverRegistered = false
        }
    }

    fun cancel() {
        if (downloadId != -1L) {
            downloadManager.remove(downloadId)
            downloadId = -1L
        }
        unregisterReceiver()
    }

    private fun getReasonMessage(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "文件已存在"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "存储空间不足"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "网络数据错误"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "未处理的 HTTP 状态码"
            DownloadManager.ERROR_UNKNOWN -> "未知错误"
            else -> "未知错误码: $reason"
        }
    }
}
