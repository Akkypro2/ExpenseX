package com.example.expenseapp

import android.provider.ContactsContract
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ExpenseApi {

    @POST("google-login")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): TokenResponse
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): TokenResponse
    @FormUrlEncoded
    @POST("token")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") pass: String
    ): TokenResponse
    // 1. Get all expenses
    @GET("/expenses")
    suspend fun getExpenses(): List<ExpenseRequest>

    // 2. Save a new expense
    @POST("/save-expense")
    suspend fun saveExpense(@Body expense: ExpenseRequest): ExpenseResponse

    // 3. Chat with AI
    @POST("/chat")
    suspend fun chatWithAi(@Body request: ChatRequest): ChatResponse

    // 4. Upload Receipt Image
    @Multipart
    @POST("/analyze-receipt")
    suspend fun analyzeReceipt(
        @Part file: MultipartBody.Part
    ): ReceiptAnalysis
}