package com.example.feature_login.data

import com.example.feature_login.model.LoginRequest
import com.example.feature_login.model.LoginResponse
import com.example.lib_network.model.ResultWrapper
import com.example.lib_network.model.RetrofitClient
import com.example.lib_network.model.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LoginRepository {
    private val api = RetrofitClient.createService(LoginApiService::class.java)

    fun login(username: String, password: String): Flow<ResultWrapper<LoginResponse>> = flow {
        emit(ResultWrapper.Loading)
        val result = safeApiCall {
            api.login(LoginRequest(username, password))
        }
        emit(result)
    }
} 