package com.example.newload

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import java.io.File

object StorageUtils {

    // 获取内部存储可用空间 (bytes)
    fun getAvailableInternalStorageSize(): Long {
        return getAvailableBytes(Environment.getDataDirectory())
    }

    // 获取主外部存储可用空间 (bytes)
    fun getAvailableExternalStorageSize(context: Context): Long {
        // 检查外部存储状态
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            return 0L
        }

        // API 24+ 使用 StorageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            storageManager.storageVolumes
                .firstOrNull { it.isPrimary }
                ?.let { 
                    return getAvailableBytes(getVolumeDirectory(it))
                }
        }

        // 回退方案
        return getAvailableBytes(Environment.getExternalStorageDirectory())
    }

    // 获取指定路径的可用空间
    private fun getAvailableBytes(path: File?): Long {
        if (path == null) return 0L
        return try {
            val stat = StatFs(path.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: IllegalArgumentException) {
            0L
        }
    }

    // 获取存储卷的目录路径 (兼容不同版本)
    private fun getVolumeDirectory(volume: StorageVolume): File? {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> volume.directory
            else -> try {
                // 反射获取路径 (Android 10 及以下)
                val getPath = volume.javaClass.getMethod("getPath")
                getPath.invoke(volume) as? File
            } catch (e: Exception) {
                null
            }
        }
    }

}