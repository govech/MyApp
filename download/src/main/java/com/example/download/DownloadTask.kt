package com.example.download

import okhttp3.Request
import okio.IOException
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * 下载任务类，负责执行具体的文件下载任务
 * @param downloadInfo 下载任务的信息
 * @param downloadManager 下载管理器，用于管理下载任务
 */
class DownloadTask(
    private var downloadInfo: DownloadInfo,
    private val downloadManager: DownloadManager
) : Runnable {

    // 暂停状态标志，volatile保证多线程可见性
    @Volatile
    private var isPaused = false

    // 取消状态标志，volatile保证多线程可见性
    @Volatile
    private var isCancelled = false

    // 下载配置信息
    private val config = downloadManager.config

    // OkHttpClient实例，用于网络请求
    private val okHttpClient = downloadManager.okHttpClient

    /**
     * 暂停下载任务
     * @return 如果任务被成功暂停，则返回true；否则返回false
     */
    fun pause(): Boolean {
        if (downloadInfo.status == DownloadStatus.DOWNLOADING) {
            isPaused = true
            updateStatus(DownloadStatus.PAUSED)
            downloadManager.notifyListeners(downloadInfo.id) { it.onPause(downloadInfo) }
            return true
        }
        return false
    }

    /**
     * 取消下载任务
     * @return 如果任务被成功取消，则返回true；否则返回false
     */
    fun cancel(): Boolean {
        isCancelled = true
        updateStatus(DownloadStatus.CANCELLED)
        downloadManager.notifyListeners(downloadInfo.id) { it.onCancel(downloadInfo) }
        return true
    }

    /**
     * 执行下载任务
     */
    override fun run() {
        if (isCancelled) return

        try {
            if (downloadInfo.status == DownloadStatus.PAUSED) {
                updateStatus(DownloadStatus.DOWNLOADING)
                downloadManager.notifyListeners(downloadInfo.id) { it.onResume(downloadInfo) }
            } else {
                updateStatus(DownloadStatus.DOWNLOADING)
                downloadManager.notifyListeners(downloadInfo.id) { it.onStart(downloadInfo) }
            }

            startDownload()

        } catch (e: Exception) {
            if (!isCancelled && !isPaused) {
                updateStatus(DownloadStatus.FAILED, e.message)
                downloadManager.notifyListeners(downloadInfo.id) { it.onError(downloadInfo, e) }
            }
        }
    }

    /**
     * 开始下载文件
     */
    private fun startDownload() {
        val file = File(downloadInfo.filePath)
        val tempFile = File("${downloadInfo.filePath}.tmp")

        // 确保目录存在
        file.parentFile?.mkdirs()

        var retryCount = 0
        while (retryCount <= config.retryCount && !isCancelled && !isPaused) {
            try {
                // 检查服务器是否支持断点续传
                val serverInfo = getServerInfo()
                val supportRange = serverInfo.first
                val totalSize = serverInfo.second

                downloadInfo = downloadInfo.copy(totalBytes = totalSize)
                downloadManager.updateDownloadInfo(downloadInfo)

                if (supportRange && totalSize > config.bufferSize * config.threadCount) {
                    // 多线程下载
                    multiThreadDownload(tempFile, totalSize)
                } else {
                    // 单线程下载
                    singleThreadDownload(tempFile)
                }

                // 下载完成，重命名临时文件
                if (!isCancelled && !isPaused) {
                    if (tempFile.exists()) {
                        tempFile.renameTo(file)
                    }
                    updateStatus(DownloadStatus.COMPLETED)
                    downloadInfo = downloadInfo.copy(completeTime = System.currentTimeMillis())
                    downloadManager.updateDownloadInfo(downloadInfo)
                    downloadManager.notifyListeners(downloadInfo.id) { it.onComplete(downloadInfo) }
                }
                return

            } catch (e: IOException) {
                retryCount++
                if (retryCount <= config.retryCount && !isCancelled && !isPaused) {
                    Thread.sleep(config.retryDelay)
                } else {
                    throw e
                }
            }
        }
    }

    /**
     * 获取服务器信息，包括是否支持断点续传和文件总大小
     * @return Pair<Boolean, Long>，第一个元素表示是否支持断点续传，第二个元素表示文件总大小
     */
    private fun getServerInfo(): Pair<Boolean, Long> {
        val request = Request.Builder()
            .url(downloadInfo.url)
            .head()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Server error: ${response.code}")
            }

            val acceptRanges = response.header("Accept-Ranges")
            val supportRange = acceptRanges == "bytes"
            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L

            return Pair(supportRange, contentLength)
        }
    }


    /**
     * 单线程下载文件
     * @param tempFile 临时文件
     */
    private fun singleThreadDownload(tempFile: File) {
        val existingSize = if (tempFile.exists()) tempFile.length() else 0L

        val requestBuilder = Request.Builder().url(downloadInfo.url)
        if (existingSize > 0) {
            requestBuilder.header("Range", "bytes=$existingSize-")
        }

        val request = requestBuilder.build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Server error: ${response.code}")
            }

            val body = response.body ?: throw IOException("Response body is null")
            val totalSize = downloadInfo.totalBytes

            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.seek(existingSize)

                body.source().use { source ->
                    val buffer = ByteArray(config.bufferSize)
                    var downloadedBytes = existingSize
                    var lastUpdateTime = System.currentTimeMillis()
                    var lastDownloadedBytes = downloadedBytes

                    while (!isCancelled && !isPaused) {
                        val bytesRead = source.read(buffer)
                        if (bytesRead == -1) break

                        raf.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime >= config.progressUpdateInterval) {
                            val speed =
                                ((downloadedBytes - lastDownloadedBytes) * 1000L) / (currentTime - lastUpdateTime)
                            val remainingTime = if (speed > 0 && totalSize > 0) {
                                (totalSize - downloadedBytes) / speed
                            } else 0L

                            val progress = DownloadProgress(
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalSize,
                                speed = speed,
                                remainingTime = remainingTime
                            )

                            downloadInfo = downloadInfo.copy(downloadedBytes = downloadedBytes)
                            downloadManager.updateDownloadInfo(downloadInfo)
                            downloadManager.notifyListeners(downloadInfo.id) {
                                it.onProgress(downloadInfo, progress)
                            }

                            lastUpdateTime = currentTime
                            lastDownloadedBytes = downloadedBytes
                        }
                    }
                }
            }
        }
    }


    /**
     * 多线程下载文件
     * @param tempFile 临时文件
     * @param totalSize 文件总大小
     */
    private fun multiThreadDownload(tempFile: File, totalSize: Long) {
        val existingSize = if (tempFile.exists()) tempFile.length() else 0L
        val remainingSize = totalSize - existingSize

        if (remainingSize <= 0) return

        val threadCount = min(config.threadCount, (remainingSize / config.bufferSize).toInt() + 1)
        val chunkSize = remainingSize / threadCount

        val executor = Executors.newFixedThreadPool(threadCount)
        val futures = mutableListOf<Future<*>>()
        val downloadedBytes = AtomicLong(existingSize)
        val lastUpdateTime = AtomicLong(System.currentTimeMillis())
        val lastDownloadedBytes = AtomicLong(existingSize)

        for (i in 0 until threadCount) {
            val start = existingSize + i * chunkSize
            val end =
                if (i == threadCount - 1) totalSize - 1 else existingSize + (i + 1) * chunkSize - 1

            val future = executor.submit {
                downloadChunk(
                    tempFile,
                    start,
                    end,
                    downloadedBytes,
                    lastUpdateTime,
                    lastDownloadedBytes,
                    totalSize
                )
            }
            futures.add(future)
        }

        // 等待所有线程完成
        try {
            futures.forEach { it.get() }
        } finally {
            executor.shutdown()
        }
    }


    /**
     * 下载文件块
     * @param tempFile 临时文件
     * @param start 起始位置
     * @param end 结束位置
     * @param totalDownloadedBytes 已下载的总字节数
     * @param lastUpdateTime 上次更新进度的时间
     * @param lastDownloadedBytes 上次更新进度时已下载的字节数
     * @param totalSize 文件总大小
     */
    private fun downloadChunk(
        tempFile: File,
        start: Long,
        end: Long,
        totalDownloadedBytes: AtomicLong,
        lastUpdateTime: AtomicLong,
        lastDownloadedBytes: AtomicLong,
        totalSize: Long
    ) {
        val request = Request.Builder()
            .url(downloadInfo.url)
            .header("Range", "bytes=$start-$end")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Server error: ${response.code}")
            }

            val body = response.body ?: throw IOException("Response body is null")

            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.seek(start)

                body.source().use { source ->
                    val buffer = ByteArray(config.bufferSize)

                    while (!isCancelled && !isPaused) {
                        val bytesRead = source.read(buffer)
                        if (bytesRead == -1) break

                        raf.write(buffer, 0, bytesRead)
                        val currentDownloaded = totalDownloadedBytes.addAndGet(bytesRead.toLong())

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime.get() >= config.progressUpdateInterval) {
                            if (lastUpdateTime.compareAndSet(lastUpdateTime.get(), currentTime)) {
                                val lastBytes = lastDownloadedBytes.getAndSet(currentDownloaded)
                                val speed =
                                    ((currentDownloaded - lastBytes) * 1000L) / config.progressUpdateInterval
                                val remainingTime = if (speed > 0) {
                                    (totalSize - currentDownloaded) / speed
                                } else 0L

                                val progress = DownloadProgress(
                                    downloadedBytes = currentDownloaded,
                                    totalBytes = totalSize,
                                    speed = speed,
                                    remainingTime = remainingTime
                                )

                                downloadInfo =
                                    downloadInfo.copy(downloadedBytes = currentDownloaded)
                                downloadManager.updateDownloadInfo(downloadInfo)
                                downloadManager.notifyListeners(downloadInfo.id) {
                                    it.onProgress(downloadInfo, progress)
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * 更新下载任务状态
     * @param status 新的状态
     * @param errorMessage 错误消息，如果没有错误则为null
     */
    private fun updateStatus(status: DownloadStatus, errorMessage: String? = null) {
        downloadInfo = downloadInfo.copy(
            status = status,
            errorMessage = errorMessage
        )
        downloadManager.updateDownloadInfo(downloadInfo)
    }
}