package com.example.expenseapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    // Modern Fintech touch: Dynamic soft pastel colors based on category
    val (iconBgColor, iconColor) = when (category) {
        "Food" -> Pair(Color(0xFFFFF3E0), Color(0xFFFF9800))       // Soft Orange
        "Travel" -> Pair(Color(0xFFE3F2FD), Color(0xFF2196F3))     // Soft Blue
        "Shopping" -> Pair(Color(0xFFFCE4EC), Color(0xFFE91E63))   // Soft Pink
        "Grocery" -> Pair(Color(0xFFE8F5E9), Color(0xFF4CAF50))    // Soft Green
        "Income" -> Pair(Color(0xFFE0F2F1), Color(0xFF009688))     // Soft Teal
        else -> Pair(Color(0xFFF3E5F5), Color(0xFF9C27B0))         // Soft Purple
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)) // Smooth, modern corners
            .background(Color.White)         // Flat pure white (no heavy shadow)
            .combinedClickable(
                onClick = { /* Do nothing on normal click */ },
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconId),
                contentDescription = category,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = merchant,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1E1E1E) // Deep elegant gray instead of pitch black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$category • $date",
                fontSize = 13.sp,
                color = Color(0xFFA0A0A0), // Smooth light gray
                fontWeight = FontWeight.Medium
            )
        }

        // Amount
        Text(
            text = if (item.type == "Credit") "+ ₹${String.format("%.2f", amount)}" else "- ₹${String.format("%.2f", amount)}",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = if (item.type == "Credit") Color(0xFF00C853) else Color(0xFFFF3D00) // Premium vibrant status colors
        )
    }
}