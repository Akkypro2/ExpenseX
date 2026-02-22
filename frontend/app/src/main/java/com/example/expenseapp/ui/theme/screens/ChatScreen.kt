package com.example.expenseapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expenseapp.models.ChatMessagePayload
import com.example.expenseapp.models.ChatRequest
import com.example.expenseapp.network.RetrofitInstance
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    messages: MutableList<ChatMessagePayload>,
    displayMessages: MutableList<ChatMessagePayload>
) {
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (displayMessages.isEmpty()) displayMessages.add(ChatMessagePayload("Hi! I'm connected to your Python Brain. Ask me anything about your spending!", false))
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(displayMessages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (msg.isUser) Color(0xFF6200EE) else Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = msg.text,
                            modifier = Modifier.padding(12.dp),
                            color = if (msg.isUser) Color.White else Color.Black
                        )
                    }
                }
            }
            if (isTyping) item { Text("Thinking...", fontSize = 12.sp, color = Color.Gray) }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color.White, RoundedCornerShape(24.dp)).padding(4.dp)) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask something...") },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )
            IconButton(onClick = {
                if (inputText.isNotEmpty()) {
                    val question = inputText
                    val userMsg = ChatMessagePayload(question, true)
                    displayMessages.add(userMsg)
                    inputText = ""
                    isTyping = true

                    scope.launch {
                        try {
                            val request = ChatRequest(
                                message = question,
                                history = messages.toList()
                            )
                            val response = RetrofitInstance.api.chatWithAi(request)
                            val aiMsg = ChatMessagePayload(response.reply, false)

                            displayMessages.add(aiMsg)
                            messages.add(userMsg)
                            messages.add(aiMsg)

                        } catch (e: Exception) {
                            displayMessages.add(ChatMessagePayload("Error: ${e.message}", false))
                        }
                        isTyping = false
                    }
                }
            }) {
                Icon(Icons.Default.Send, "Send", tint = Color(0xFF6200EE))
            }
        }
    }
}