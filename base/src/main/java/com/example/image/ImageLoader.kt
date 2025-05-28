package com.example.image

import android.widget.ImageView
import com.example.image.impl.CoilImageLoader

object ImageLoader {
    private var loader: IImageLoader = CoilImageLoader() // 默认用Coil，可自由切换

    fun init(customLoader: IImageLoader) {
        loader = customLoader
    }

    fun load(url: String, imageView: ImageView) {
        loader.load(url, imageView)
    }

    fun load(resId: Int, imageView: ImageView) {
        loader.load(resId, imageView)
    }
}