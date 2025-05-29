package com.example.image

import android.content.Context
import android.widget.ImageView

interface IImageLoader {
    fun load(url: String, imageView: ImageView, options: ImageOptions = ImageOptions(), callback: ImageLoadCallback? = null)
    fun load(resId: Int, imageView: ImageView, options: ImageOptions = ImageOptions(), callback: ImageLoadCallback? = null)
    fun preload(context: Context, url: String)
    fun clearMemoryCache(context: Context)
    fun clearDiskCache(context: Context)
}