package com.example.expenseapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expenseapp.models.ExpenseRequest

@Composable
fun DashboardCard(expenseList: List<ExpenseRequest>, initialBalance: Float, onEditBalance: () -> Unit) {
    val totalExpense = expenseList.filter { it.type == "Debit" }.sumOf { it.amount?.toDouble() ?: 0.0 }
    val totalIncome = expenseList.filter { it.type == "Credit" }.sumOf { it.amount?.toDouble() ?: 0.0 }
    val currentBalance = initialBalance.toDouble() + totalIncome - totalExpense

    // Premium Gradient Brush
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E1E2C), // Deep midnight blue/gray
            Color(0xFF121212)  // Almost black
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp), // Slightly taller for breathing room
        shape = RoundedCornerShape(24.dp), // Softer, rounder corners
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Subtle shadow
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardGradient)
                .padding(24.dp) // More inner padding for a cleaner look
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP ROW: Total Expense
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Spent", color = Color(0xFFA0A0B0), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "₹ ${String.format("%.2f", totalExpense)}",
                            color = Color(0xFFFF4B4B), // Vibrant modern red
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Small floating badge for initial balance
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable { onEditBalance() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Start: ₹${initialBalance.toInt()} ✏️",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.1f))

                // BOTTOM ROW: Current Balance
                Column {
                    Text("Available Balance", color = Color(0xFFA0A0B0), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "₹ ${String.format("%.2f", currentBalance)}",
                        color = Color(0xFF00E676), // Electric neon green
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}