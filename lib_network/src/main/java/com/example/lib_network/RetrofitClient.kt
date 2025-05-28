package com.example.lib_network.model


import com.example.lib_network.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

// -------------------- 网络层配置 --------------------
object RetrofitClient {
    private const val BASE_URL = "https://open.bigmodel.cn/api/paas/v4/"
    private const val TAG = "NetworkClient"

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("Authorization", BuildConfig.ZHIPU_API_KEY)
            .build()
        chain.proceed(request)
    }


    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(SmartLoggingInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    fun <T> createService(service: Class<T>): T = retrofit.create(service)

}
