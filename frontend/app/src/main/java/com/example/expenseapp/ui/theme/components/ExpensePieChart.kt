package com.example.expenseapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expenseapp.models.ExpenseRequest

@Composable
fun ExpensePieChart(expenses: List<ExpenseRequest>) {
    val total = expenses.filter { it.type == "Debit" }.sumOf { it.amount?.toDouble() ?: 0.0 }
    if (total == 0.0) return

    val categories = expenses.filter { it.type == "Debit" }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount?.toDouble() ?: 0.0 }.toFloat() }

    val colors = listOf(Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF5722), Color(0xFFFFC107), Color(0xFFE91E63))

    var animationPlayed by remember { mutableStateOf(false) }
    val animateSweep by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "pie_animation"
    )

    LaunchedEffect(Unit) { animationPlayed = true }

    Row(
        modifier = Modifier.fillMaxWidth().height(150.dp).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(120.dp)) {
                drawArc(
                    color = Color(0xFFF0F0F0),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Butt),
                    size = Size(size.width, size.height)
                )

                var startAngle = -90f
                categories.values.forEachIndexed { index, value ->
                    val sweepAngle = (value / total.toFloat()) * 360f * animateSweep
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Butt),
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Spent", fontSize = 10.sp, color = Color.Gray)
                Text("₹${total.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            categories.keys.forEachIndexed { index, name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(colors[index % colors.size], CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(name ?: "Other", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}