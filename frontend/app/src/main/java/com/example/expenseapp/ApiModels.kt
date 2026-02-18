package com.example.expenseapp

import com.google.gson.annotations.SerializedName

data class GoogleLoginRequest(
    val token: String
)
data class RegisterRequest(
    val email: String,
    val password: String
)

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)
data class ExpenseRequest(
    val id: Int? = null,
    val merchant: String?,
    val amount: Float?,
    val date: String?,
    val category: String?,
    val type: String?
)

data class ExpenseResponse(
    val status: String,
    val id: Int
)

// --- CHAT ---
data class ChatRequest(
    val message: String,
    val history: List<ChatMessagePayload>
)

data class ChatMessagePayload(
    val text: String,
    val isUser: Boolean
)

data class ChatResponse(
    val reply: String
)

// --- RECEIPT ANALYSIS ---
data class ReceiptAnalysis(
    val id: Int,
    val merchant: String,
    val amount: String,
    val date: String,
    val category: String
)