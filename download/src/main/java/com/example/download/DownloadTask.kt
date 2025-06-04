package com.example.download

import okhttp3.Request
import okio.IOException
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class DownloadTask(
    private var downloadInfo: DownloadInfo,
    private val downloadManager: DownloadManager
) : Runnable {
    
    @Volatile
    private var isPaused = false
    @Volatile
    private var isCancelled = false
    
    private val config = downloadManager.config
    private val okHttpClient = downloadManager.okHttpClient
    
    fun pause(): Boolean {
        if (downloadInfo.status == DownloadStatus.DOWNLOADING) {
            isPaused = true
            updateStatus(DownloadStatus.PAUSED)
            downloadManager.notifyListeners(downloadInfo.id) { it.onPause(downloadInfo) }
            return true
        }
        return false
    }
    
    fun cancel(): Boolean {
        isCancelled = true
        updateStatus(DownloadStatus.CANCELLED)
        downloadManager.notifyListeners(downloadInfo.id) { it.onCancel(downloadInfo) }
        return true
    }
    
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
                            val speed = ((downloadedBytes - lastDownloadedBytes) * 1000L) / (currentTime - lastUpdateTime)
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
            val end = if (i == threadCount - 1) totalSize - 1 else existingSize + (i + 1) * chunkSize - 1
            
            val future = executor.submit {
                downloadChunk(tempFile, start, end, downloadedBytes, lastUpdateTime, lastDownloadedBytes, totalSize)
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
                                val speed = ((currentDownloaded - lastBytes) * 1000L) / config.progressUpdateInterval
                                val remainingTime = if (speed > 0) {
                                    (totalSize - currentDownloaded) / speed
                                } else 0L
                                
                                val progress = DownloadProgress(
                                    downloadedBytes = currentDownloaded,
                                    totalBytes = totalSize,
                                    speed = speed,
                                    remainingTime = remainingTime
                                )
                                
                                downloadInfo = downloadInfo.copy(downloadedBytes = currentDownloaded)
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
    
    private fun updateStatus(status: DownloadStatus, errorMessage: String? = null) {
        downloadInfo = downloadInfo.copy(
            status = status,
            errorMessage = errorMessage
        )
        downloadManager.updateDownloadInfo(downloadInfo)
    }
}