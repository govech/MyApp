package com.example.image

data class ImageOptions(
    val placeholder: Int? = null,           // 占位图资源ID
    val error: Int? = null,                 // 错误图资源ID
    val isCircle: Boolean = false,          // 是否圆形
    val cornerRadius: Float = 0f,           // 圆角半径
    val isBlur: Boolean = false,            // 是否模糊
    val blurRadius: Float = 10f,            // 模糊半径
    val blurSampling: Float = 1f,           // 模糊采样
    val thumbnailUrl: String? = null,       // 缩略图URL
    val scaleType: ScaleType = ScaleType.FILL, // 缩放类型
    val allowGif: Boolean = false           // 是否允许GIF
) {
    enum class ScaleType { FIT, FILL }
} 