package com.example.expenseapp.network

import com.example.expenseapp.models.ChatRequest
import com.example.expenseapp.models.ChatResponse
import com.example.expenseapp.models.ExpenseRequest
import com.example.expenseapp.models.ExpenseResponse
import com.example.expenseapp.models.GoogleLoginRequest
import com.example.expenseapp.models.ReceiptAnalysis
import com.example.expenseapp.models.RegisterRequest
import com.example.expenseapp.models.TokenResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

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

    // REMOVED LEADING SLASHES BELOW
    @GET("expenses")
    suspend fun getExpenses(): List<ExpenseRequest>

    @POST("save-expense")
    suspend fun saveExpense(@Body expense: ExpenseRequest): ExpenseResponse

    @POST("chat")
    suspend fun chatWithAi(@Body request: ChatRequest): ChatResponse

    @Multipart
    @POST("analyze-receipt")
    suspend fun analyzeReceipt(
        @Part file: MultipartBody.Part
    ): ReceiptAnalysis

    // ADDED DELETE FUNCTION HERE
    @DELETE("expense/{id}")
    suspend fun deleteExpense(@Path("id") id: Int): Response<Unit>
}