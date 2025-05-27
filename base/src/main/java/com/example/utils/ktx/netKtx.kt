package com.example.gxy.ktx

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


/**
 * 在CoroutineScope中安全地启动一个协程，该协程能够处理异常和成功情况
 * （例如用于在任意地方调用网络请求，免得报错）
 *
 * @param block 协程中执行的主要代码块，如果执行成功将调用onSuccess，否则将调用onError
 * @param onError 当block中抛出异常时调用的处理异常的代码块，用于处理错误情况
 * @param onSuccess 当block中代码执行成功时调用的代码块，用于处理成功情况
 */
inline fun <T> CoroutineScope.launchSafe(
    crossinline block: suspend () -> T,
    crossinline onError: (Throwable) -> Unit,
    crossinline onSuccess: (T) -> Unit
) {
    this.launch {
        try {
            // 执行主要代码块，并在成功后调用onSuccess
            val result = block()
            onSuccess(result)
        } catch (e: Throwable) {
            // 捕获block中抛出的异常，并调用onError进行处理
            onError(e)
        }
    }
}

