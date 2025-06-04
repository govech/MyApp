package com.example.download

/**
 * 下载监听器接口，用于监听下载过程中的各种状态变化
 * 通过实现此接口，可以获取下载任务的实时状态，如开始、进度、暂停、恢复、完成、错误和取消
 */
interface DownloadListener {
    /**
     * 下载任务开始时调用
     *
     * @param downloadInfo 下载任务的信息，包含下载的相关数据和状态
     */
    fun onStart(downloadInfo: DownloadInfo) {}

    /**
     * 下载任务进度更新时调用
     *
     * @param downloadInfo 下载任务的信息，包含下载的相关数据和状态
     * @param progress 下载任务的当前进度
     */
    fun onProgress(downloadInfo: DownloadInfo, progress: DownloadProgress) {}

    /**
     * 下载任务暂停时调用
     *
     * @param downloadInfo 下载任务的信息，包含下载的相关数据和状态
     */
    fun onPause(downloadInfo: DownloadInfo) {}

    /**
     * 下载任务恢复时调用
     *
     * @param downloadInfo 下载任务的信息，包含下载的相关数据和状态
     */
    fun onResume(downloadInfo: DownloadInfo) {}

    /**
     * 下载任务完成时调用
     *
     * @param downloadInfo 下载任务的信息，包含下载的相关数据和状态
     */
    fun onComplete(downloadInfo: DownloadInfo) {}

    /**
     * 下载任务发生错误时调用
     *
     * @param downloadInfo 下载任务的信息，包含下载的相关数据和状态
     * @param error 发生的错误类型
     */
    fun onError(downloadInfo: DownloadInfo, error: Throwable) {}

    /**
     * 下载任务取消时调用
     *
     * @param downloadInfo 下载任务的信息，包含下载的相关数据和状态
     */
    fun onCancel(downloadInfo: DownloadInfo) {}
}
