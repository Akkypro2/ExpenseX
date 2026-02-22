package com.example.expenseapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expenseapp.models.ExpenseRequest
import com.example.expenseapp.utils.getCurrentDate

@Composable
fun ManualAddDialog(onDismiss: () -> Unit, onConfirm: (ExpenseRequest) -> Unit) {
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Debit") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text("Title") })
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                    label = { Text("Amount") }
                )
                Row {
                    FilterChip(selected = type == "Debit", onClick = { type = "Debit" }, label = { Text("Expense") })
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = type == "Credit", onClick = { type = "Credit" }, label = { Text("Income") })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (merchant.isNotEmpty() && amount.isNotEmpty()) {
                    onConfirm(ExpenseRequest(merchant = merchant, amount = amount.toFloatOrNull() ?: 0f, date = getCurrentDate(), category = if (type == "Credit") "Income" else "Food", type = type))
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}