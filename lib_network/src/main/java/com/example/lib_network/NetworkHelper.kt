package com.example.lib_network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkHelper {
    private const val BASE_URL = "https://api.example.com/"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}