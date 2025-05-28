package com.example.image.impl

import android.widget.ImageView
import coil.load
import com.example.image.IImageLoader

class CoilImageLoader : IImageLoader {
    override fun load(url: String, imageView: ImageView) {
        imageView.load(url) {
            crossfade(true)
        }
    }

    override fun load(resId: Int, imageView: ImageView) {
        imageView.load(resId) {
            crossfade(true)
        }
    }
}