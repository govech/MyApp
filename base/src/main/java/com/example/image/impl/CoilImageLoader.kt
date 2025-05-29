package com.example.image.impl

import android.content.Context
import android.widget.ImageView
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import com.example.base.R
import com.example.image.IImageLoader
import com.example.image.ImageLoadCallback
import com.example.image.ImageOptions
import jp.wasabeef.transformers.coil.BlurTransformation

class CoilImageLoader : IImageLoader {
    override fun load(url: String, imageView: ImageView, options: ImageOptions, callback: ImageLoadCallback?) {
        val request = ImageRequest.Builder(imageView.context)
            .data(url)
            .target(
                onStart = { callback?.onStart() },
                onSuccess = { result ->
                    imageView.setImageDrawable(result)
                    callback?.onSuccess(result)
                },
                onError = { error ->
                    imageView.setImageDrawable(error)
                    callback?.onError(error)
                }
            )
            .apply {
                options.placeholder?.let { placeholder(R.drawable.placeholder) }
                options.error?.let { error(R.drawable.error) }
                if (options.isCircle) transformations(CircleCropTransformation())
                if (options.cornerRadius > 0f) transformations(RoundedCornersTransformation(options.cornerRadius))
                if (options.isBlur) transformations(BlurTransformation(imageView.context,
                    options.blurRadius.toInt(), options.blurSampling.toInt()
                ))
                if (options.thumbnailUrl != null) placeholderMemoryCacheKey(options.thumbnailUrl)
                if (options.scaleType == ImageOptions.ScaleType.FIT) scale(Scale.FIT) else scale(Scale.FILL)
                if (options.allowGif) allowHardware(false)
            }
            .build()
        imageView.context.imageLoader.enqueue(request)
    }

    override fun load(resId: Int, imageView: ImageView, options: ImageOptions, callback: ImageLoadCallback?) {
        val request = ImageRequest.Builder(imageView.context)
            .data(resId)
            .target(
                onStart = { callback?.onStart() },
                onSuccess = { result ->
                    imageView.setImageDrawable(result)
                    callback?.onSuccess(result)
                },
                onError = { error ->
                    imageView.setImageDrawable(error)
                    callback?.onError(error)
                }
            )
            .apply {
                options.placeholder?.let { placeholder(R.drawable.placeholder) }
                options.error?.let { error(R.drawable.error) }
                if (options.isCircle) transformations(CircleCropTransformation())
                if (options.cornerRadius > 0f) transformations(RoundedCornersTransformation(options.cornerRadius))
                if (options.isBlur) transformations(BlurTransformation(imageView.context, options.blurRadius.toInt(), options.blurSampling.toInt()))
                if (options.scaleType == ImageOptions.ScaleType.FIT) scale(Scale.FIT) else scale(Scale.FILL)
                if (options.allowGif) allowHardware(false)
            }
            .build()
        imageView.context.imageLoader.enqueue(request)
    }

    override fun preload(context: Context, url: String) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        context.imageLoader.enqueue(request)
    }

    override fun clearMemoryCache(context: Context) {
        context.imageLoader.memoryCache?.clear()
    }

    @OptIn(ExperimentalCoilApi::class)
    override fun clearDiskCache(context: Context) {
        // Coil 2.x 及以上支持
        context.imageLoader.diskCache?.clear()
    }
}