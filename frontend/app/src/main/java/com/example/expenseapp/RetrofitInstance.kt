package com.example.expenseapp

import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val BASE_URL = "https://hui-pronegotiation-clifton.ngrok-free.dev"

    var jwtToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()

        val requestBuilder = original.newBuilder()

        if(jwtToken != null){
            requestBuilder.header("Authorization", "Bearer $jwtToken")
        }

        val request = requestBuilder.build()
        chain.proceed(request)

    }
    // 1. Create the Client with increased timeouts
    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS) // Time to find the server
        .readTimeout(60, TimeUnit.SECONDS)    // Time to wait for the AI response (Critical!)
        .writeTimeout(30, TimeUnit.SECONDS)   // Time to send the request
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