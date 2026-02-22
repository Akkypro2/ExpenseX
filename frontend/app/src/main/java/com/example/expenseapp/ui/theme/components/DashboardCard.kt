package com.example.expenseapp.ui.components

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

    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF292929))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Total Expense \uD83D\uDCC9", color = Color.Gray, fontSize = 12.sp)
                Text("₹ ${String.format("%.2f", totalExpense)}", color = Color(0xFFFF5252), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Current Balance \uD83D\uDCB0", color = Color.Gray, fontSize = 12.sp)
                    Text("₹ ${String.format("%.2f", currentBalance)}", color = Color(0xFF4CAF50), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "(Start: ₹${initialBalance.toInt()}) ✏️",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onEditBalance() }
                )
            }
        }
    }
}