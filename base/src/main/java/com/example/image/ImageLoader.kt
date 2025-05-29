package com.example.image

import android.content.Context
import android.widget.ImageView
import com.example.image.impl.CoilImageLoader

object ImageLoader {
    private var loader: IImageLoader = CoilImageLoader() // 默认用Coil，可自由切换

    fun init(customLoader: IImageLoader) {
        loader = customLoader
    }

    fun load(url: String, imageView: ImageView, options: ImageOptions = ImageOptions(), callback: ImageLoadCallback? = null) {
        loader.load(url, imageView, options, callback)
    }

    fun load(resId: Int, imageView: ImageView, options: ImageOptions = ImageOptions(), callback: ImageLoadCallback? = null) {
        loader.load(resId, imageView, options, callback)
    }

    fun preload(context: Context, url: String) {
        loader.preload(context, url)
    }

    fun clearMemoryCache(context: Context) {
        loader.clearMemoryCache(context)
    }

    fun clearDiskCache(context: Context) {
        loader.clearDiskCache(context)
    }
}