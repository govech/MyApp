package com.example.lib_network.model

import com.google.gson.annotations.SerializedName

// -------------------- 基础响应结构 --------------------
/**
 * 标准API响应格式（示例）
 * 实际字段需要根据接口文档调整
 */
data class BaseResponse<T>(
    @SerializedName("code")    // 根据实际字段名修改
    val code: Int,             // 业务状态码
    
    @SerializedName("message") // 根据实际字段名修改
    val message: String? = null, // 业务消息
    
    @SerializedName("data")    // 根据实际字段名修改
    val data: T? = null        // 业务数据（泛型）
)