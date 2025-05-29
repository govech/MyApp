package com.example.feature_login.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature_login.data.LoginRepository
import com.example.feature_login.model.LoginResponse
import com.example.lib_network.model.ResultWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: LoginRepository) : ViewModel() {


    private val _loginResult =
        MutableStateFlow<ResultWrapper<LoginResponse>>(ResultWrapper.Loading)
    val loginResult: StateFlow<ResultWrapper<LoginResponse>> = _loginResult


    fun login(username: String, password: String) {
        viewModelScope.launch {
            // 添加热流转换
            val chatFlow = repository.login(username, password)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000), // 5秒无订阅停止上游
                    initialValue = ResultWrapper.Loading
                )


            chatFlow.collect { result ->
                _loginResult.value = result
            }
        }
    }
} 