package com.example.expenseapp.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    // ADDED THE TRAILING SLASH HERE:
    private const val BASE_URL = "https://hui-pronegotiation-clifton.ngrok-free.dev/"

    var jwtToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()

        val requestBuilder = original.newBuilder()

        if (jwtToken != null) {
            requestBuilder.header("Authorization", "Bearer $jwtToken")
        }

        val request = requestBuilder.build()
        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ExpenseApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExpenseApi::class.java)
    }
}