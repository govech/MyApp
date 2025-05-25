package com.example.lib_network.model


import com.elvishew.xlog.XLog
import com.example.lib_network.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

/**
 * 自定义日志拦截器，用于格式化网络请求和响应的日志
 */
class SmartLoggingInterceptor : Interceptor {
    private val logger = HttpLoggingInterceptor.Logger { message ->
        when {
            message.isJson() -> XLog.json(message)   // JSON 格式化
            message.isXml() -> XLog.xml(message)     // XML 格式化
            else -> XLog.d("OkHttp: $message")       // 普通日志
        }
    }
    // 扩展函数，判断字符串是否可能是 JSON 格式
    private fun String.isJson(): Boolean {
        val trim = trim()
        return (trim.startsWith('{') && trim.endsWith('}')) ||
                (trim.startsWith('[') && trim.endsWith(']'))
    }
    // 扩展函数，判断字符串是否可能是 XML 格式
    private fun String.isXml(): Boolean {
        val trim = trim()
        return trim.startsWith('<') && trim.endsWith('>')
    }

    /**
     * 拦截网络请求并记录日志
     * 根据当前是否是调试模式来决定日志记录的详细程度
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNs = System.nanoTime() // 请求开始时间（纳秒）

        val response = HttpLoggingInterceptor(logger)
            .apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
            .intercept(chain)
        val tookMs = (System.nanoTime() - startNs) / 1_000_000 // 转换成毫秒
        // 打印性能日志
        val url = request.url.toString()
        XLog.i("Request to $url 耗时： ${tookMs}ms")
        return response
    }
}

