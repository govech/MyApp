package com.example.image.impl

import android.widget.ImageView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.base.R
import com.example.image.IImageLoader

class CoilImageLoader : IImageLoader {
    override fun load(url: String, imageView: ImageView) {
        imageView.load(url) {
            crossfade(true)  // 启用渐变动画
//            placeholder(R.drawable.placeholder)  // 加载中的占位图
//            error(R.drawable.error)  // 加载失败时的图片
//            transformations(CircleCropTransformation())  // 图片裁剪/变换（如圆形）
//            size(300, 300)  // 指定目标尺寸
//                .listener(
//                    onStart = { /* 开始加载 */ },
//                    onSuccess = { _, _ -> /* 加载成功 */ },
//                    onError = { _, _ -> /* 加载失败 */ }
//                )
        }
    }

    override fun load(resId: Int, imageView: ImageView) {
        imageView.load(resId) {
            crossfade(true)
        }
    }
}