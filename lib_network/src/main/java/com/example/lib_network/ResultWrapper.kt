package com.example.lib_network.model

import com.example.lib_network.model.BaseResponse
import okio.IOException
import retrofit2.HttpException
import java.util.concurrent.TimeoutException

/**
 * 密封类用于封装API调用的结果
 * @param T 泛型参数，表示API返回的数据类型
 */
sealed class ResultWrapper<out T> {
    /**
     * 成功的数据封装
     * @param data 成功返回的数据
     */
    data class Success<out T>(val data: T) : ResultWrapper<T>()

    /**
     * 错误信息封装
     * @param type 错误类型
     * @param code 错误代码
     * @param message 错误信息
     */
    data class Error(
        val type: ErrorType,
        val code: Int? = null,
        val message: String? = null
    ) : ResultWrapper<Nothing>() {
        companion object {
            /**
             * 网络错误
             * @param message 错误信息，默认为"网络连接失败"
             */
            fun network(message: String = "网络连接失败") = Error(
                type = ErrorType.NETWORK,
                message = message
            )
            /**
             * HTTP错误
             * @param code 错误代码
             * @param message 错误信息
             */
            fun http(code: Int, message: String?) = Error(
                type = ErrorType.HTTP,
                code = code,
                message = message
            )

            /**
             * 业务错误
             * @param code 错误代码
             * @param message 错误信息
             */
            fun business(code: Int, message: String?) = Error(
                type = ErrorType.BUSINESS,
                code = code,
                message = message
            )

            /**
             * 未知错误
             * @param message 错误信息，默认为"未知错误"
             */
            fun unknown(message: String? = "未知错误") = Error(
                type = ErrorType.UNKNOWN,
                message = message
            )
        }
    }
    /**
     * 加载中的状态封装
     */
    object Loading : ResultWrapper<Nothing>()

    /**
     * 错误类型枚举
     */
    enum class ErrorType {
        NETWORK,     // 网络层错误（DNS/连接超时等）
        HTTP,        // HTTP协议错误（4xx/5xx）
        BUSINESS,    // 业务逻辑错误（如认证失败）
        UNKNOWN      // 未分类异常
    }
}

/**
 * BaseResponse包装
 */
suspend fun <T> safeApiCall(
    call: suspend () -> BaseResponse<T>
): ResultWrapper<T> = try {
    handleBusinessResponse(call())
} catch (e: Exception) {
    handleException(e)
}


/**
 * 处理直接返回数据对象的情况（非BaseResponse包装）
 * @param call 挂起函数，用于调用API
 * @return ResultWrapper<T> 封装的API调用结果
 */
suspend fun <T> safeRawApiCall(
    call: suspend () -> T
): ResultWrapper<T> = try {
    ResultWrapper.Success(call())
} catch (e: Exception) {
    handleException(e)
}

/**
 * 处理业务响应
 * @param response API返回的响应
 * @return ResultWrapper<T> 封装的业务响应结果
 */
private fun <T> handleBusinessResponse(
    response: BaseResponse<T>
): ResultWrapper<T> = when (response.code) {
    200 -> {
        response.data?.let {
            ResultWrapper.Success(it)
        } ?: ResultWrapper.Error.business(code = response.code, message = "Empty response data")
    }

    401 -> ResultWrapper.Error.business(code = response.code, message = "登录状态已过期")
    403 -> ResultWrapper.Error.business(code = response.code, message = "访问被拒绝")
    else -> ResultWrapper.Error.business(
        code = response.code,
        message = response.message
    )
}

/**
 * 处理异常
 * @param e 异常对象
 * @return ResultWrapper.Error 封装的错误信息
 */
private fun handleException(e: Exception): ResultWrapper.Error = when (e) {
    is IOException -> when (e) {
        is java.net.UnknownHostException -> ResultWrapper.Error.network("无法连接到服务器")
        is java.net.ConnectException -> ResultWrapper.Error.network("连接服务器失败")
        else -> ResultWrapper.Error.network()
    }

    is HttpException -> {
        val errorBody = runCatching {
            e.response()?.errorBody()?.string()
        }.getOrNull()
        ResultWrapper.Error.http(
            code = e.code(),
            message = errorBody ?: e.message()
        )
    }


    is TimeoutException -> ResultWrapper.Error.network("请求超时")

    else -> ResultWrapper.Error.unknown(e.message)
}