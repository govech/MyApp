package com.example.image

import android.graphics.drawable.Drawable

interface ImageLoadCallback {
    fun onStart() {}
    fun onSuccess(result: Drawable?) {}
    fun onError(error: Drawable?) {}
} 