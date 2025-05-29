package com.example.feature_login.data

import com.example.feature_login.model.LoginRequest
import com.example.feature_login.model.LoginResponse
import com.example.lib_network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST


interface LoginApiService {
    @POST("user/login")
    suspend fun login(@Body request: LoginRequest): BaseResponse<LoginResponse>
} 