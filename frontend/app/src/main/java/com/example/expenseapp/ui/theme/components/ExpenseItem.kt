package com.example.expenseapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expenseapp.models.ExpenseRequest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseItem(item: ExpenseRequest, onLongClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* Do nothing on normal click */ },
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val category = item.category ?: "Other"
            val merchant = item.merchant ?: "Unknown Merchant"
            val date = item.date ?: "Unknown Date"
            val amount = item.amount ?: 0.0

            val iconId = when (category) {
                "Food" -> android.R.drawable.ic_menu_myplaces
                "Travel" -> android.R.drawable.ic_menu_compass
                "Shopping" -> android.R.drawable.ic_menu_gallery
                "Grocery" -> android.R.drawable.ic_menu_agenda
                else -> android.R.drawable.ic_menu_view
            }

            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEDE7F6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(iconId), contentDescription = null, tint = Color(0xFF6200EE), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(merchant, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("$category • $date", fontSize = 12.sp, color = Color.Gray)
            }
            Text(
                text = if (item.type == "Credit") "+ ₹$amount" else "- ₹$amount",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (item.type == "Credit") Color(0xFF4CAF50) else Color.Red
            )
        }
    }
}