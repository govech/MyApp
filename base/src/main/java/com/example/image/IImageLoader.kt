package com.example.image

import android.widget.ImageView

interface IImageLoader {
    fun load(url: String, imageView: ImageView)
    fun load(resId: Int, imageView: ImageView)
}