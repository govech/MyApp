package com.example.utils.ktx

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleableRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat

/**
 * 资源工具类（直接通过 Context/Resources 的扩展函数调用）
 * 依赖：androidx.core:core-ktx（推荐添加）
 */

// ========================================================
// 颜色相关扩展
// ========================================================
fun Context.color(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)

fun Context.colorStateList(@ColorRes id: Int): ColorStateList? =
    ContextCompat.getColorStateList(this, id)

/**
 * 解析主题中的颜色属性（如 ?attr/colorPrimary）
 */
fun Context.themeColor(@AttrRes attrRes: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue.data
}

// ========================================================
// 字符串相关扩展
// ========================================================
fun Context.string(@StringRes id: Int): String = getString(id)

fun Context.string(@StringRes id: Int, vararg args: Any): String = getString(id, *args)

/**
 * 获取带 HTML 样式的字符串（简化处理）
 */
fun Context.htmlString(@StringRes id: Int, vararg args: Any): CharSequence {
    val text = getString(id, *args)
    return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
}

// ========================================================
// 尺寸相关扩展
// ========================================================
fun Context.dimension(@DimenRes id: Int): Float = resources.getDimension(id)

fun Context.dimensionPixelSize(@DimenRes id: Int): Int = resources.getDimensionPixelSize(id)

// ========================================================
// Drawable 相关扩展
// ========================================================
fun Context.drawable(@DrawableRes id: Int): Drawable? = ContextCompat.getDrawable(this, id)

/**
 * 解析主题中的 Drawable 属性（如 ?attr/selectableItemBackground）
 */
fun Context.themeDrawable(@AttrRes attrRes: Int): Drawable? {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrRes, typedValue, true)
    return if (typedValue.resourceId != 0) {
        ContextCompat.getDrawable(this, typedValue.resourceId)
    } else {
        null
    }
}

// ========================================================
// 其他工具函数
// ========================================================
/**
 * 安全使用 TypedArray（自动回收）
 */
inline fun <R> Context.useAttributes(
    @StyleableRes attrs: IntArray,
    block: (TypedArray) -> R
): R {
    val typedArray = obtainStyledAttributes(attrs)
    return try {
        block(typedArray)
    } finally {
        typedArray.recycle()
    }
}