package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val token: String = ""
)
