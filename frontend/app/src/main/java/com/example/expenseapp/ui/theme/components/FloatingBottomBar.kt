package com.example.expenseapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FloatingBottomBar(currentScreen: String, onScreenSelected: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        shape = RoundedCornerShape(36.dp), // Premium pill shape
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp) // Soft floating shadow
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingNavItem(
                title = "Dashboard",
                icon = Icons.Default.Home,
                isSelected = currentScreen == "dashboard",
                onClick = { onScreenSelected("dashboard") }
            )
            FloatingNavItem(
                title = "Ask AI",
                icon = Icons.Default.Face, // Fun icon for your AI bot!
                isSelected = currentScreen == "chat",
                onClick = { onScreenSelected("chat") }
            )
        }
    }
}

@Composable
fun FloatingNavItem(title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    // Dynamic colors based on selection
    val background = if (isSelected) Color(0xFF6200EE).copy(alpha = 0.1f) else Color.Transparent
    val contentColor = if (isSelected) Color(0xFF6200EE) else Color(0xFFA0A0A0)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(26.dp))
        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}