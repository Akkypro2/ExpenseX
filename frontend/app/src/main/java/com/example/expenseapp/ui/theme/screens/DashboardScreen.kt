package com.example.expenseapp.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.example.expenseapp.* // Imports utils and models from your base package
import com.example.expenseapp.models.ExpenseRequest
import com.example.expenseapp.network.RetrofitInstance
import com.example.expenseapp.ui.components.DashboardCard
import com.example.expenseapp.ui.components.ExpenseItem
import com.example.expenseapp.ui.components.ExpensePieChart
import com.example.expenseapp.ui.components.ManualAddDialog
import com.example.expenseapp.utils.getCurrentDate
import com.example.expenseapp.utils.uriToFile
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)

    // --- STATE ---
    var expenseList by remember { mutableStateOf(listOf<ExpenseRequest>()) }
    var isLoading by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }

    val savedBalance = prefs.getFloat("initial_balance", -1f)
    var initialBalance by remember { mutableStateOf(if (savedBalance == -1f) 0f else savedBalance) }
    var showBalanceDialog by remember { mutableStateOf(savedBalance == -1f) }

    var expenseToDelete by remember { mutableStateOf<ExpenseRequest?>(null) }

    // --- API FUNCTIONS ---
    fun fetchExpenses() {
        scope.launch {
            try {
                val expenses = RetrofitInstance.api.getExpenses()
                expenseList = expenses
            } catch (e: Exception) {
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) { fetchExpenses() }

    fun deleteExpenseFromBackend(id: Int) {
        scope.launch {
            try {
                RetrofitInstance.api.deleteExpense(id)
                fetchExpenses()
                Toast.makeText(context, "Expense Deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error deleting: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addExpenseToBackend(merchant: String, amount: String, category: String, type: String) {
        scope.launch {
            try {
                val newExpense = ExpenseRequest(merchant = merchant, amount = amount.toFloatOrNull() ?: 0f, date = getCurrentDate(), category = category, type = type)
                val response = RetrofitInstance.api.saveExpense(newExpense)
                if (response.status == "saved") {
                    fetchExpenses()
                    Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- CAMERA LOGIC ---
    fun analyzeReceipt(uri: Uri) {
        isLoading = true
        scope.launch {
            try {
                val file = uriToFile(context, uri)
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val analysis = RetrofitInstance.api.analyzeReceipt(body)

                fetchExpenses()
                isLoading = false
                Toast.makeText(context, "Added: ${analysis.merchant} - ${analysis.amount}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(context, "Scan Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val file = File(context.getExternalFilesDir(null), "temp_receipt.jpg")
            val uri = FileProvider.getUriForFile(context, "com.example.expenseapp.provider", file)
            analyzeReceipt(uri)
        }
    }

    // --- SMS LOGIC ---
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    val currentIntent = activity?.intent
    LaunchedEffect(currentIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS))
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS))
        }

        val autoAmount = currentIntent?.getStringExtra("auto_add_amount")
        val smsType = currentIntent?.getStringExtra("sms_type") ?: "Debit"

        if (autoAmount != null) {
            NotificationManagerCompat.from(context).cancel(1001)
            val cat = if (smsType == "Credit") "Income" else "Bills"
            val mer = if (smsType == "Credit") "Money Received" else "SMS Transaction"
            addExpenseToBackend(mer, autoAmount, cat, smsType)
            currentIntent?.removeExtra("auto_add_amount")
            currentIntent?.removeExtra("sms_type")
        }
    }

    // --- UI LAYOUT ---
    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { showManualDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(painter = painterResource(android.R.drawable.ic_menu_edit), contentDescription = "Manual")
                }
                FloatingActionButton(
                    onClick = {
                        val file = File(context.getExternalFilesDir(null), "temp_receipt.jpg")
                        val uri = FileProvider.getUriForFile(context, "com.example.expenseapp.provider", file)
                        cameraLauncher.launch(uri)
                    },
                    containerColor = Color(0xFF6200EE),
                    contentColor = Color.White
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Icon(Icons.Default.Add, "Scan")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp)) {
            DashboardCard(expenseList = expenseList, initialBalance = initialBalance, onEditBalance = { showBalanceDialog = true })
            Spacer(modifier = Modifier.height(16.dp))

            if (expenseList.isNotEmpty()) {
                Text("Spending Analysis", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                ExpensePieChart(expenseList)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("History (Long press to delete)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(expenseList) { item -> ExpenseItem(item, onLongClick = { expenseToDelete = item }) }
            }
        }
    }

    // --- DIALOGS ---
    if (showBalanceDialog) {
        var inputBalance by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBalanceDialog = false },
            title = { Text("Set Current Balance \uD83D\uDCB0") },
            text = {
                OutlinedTextField(value = inputBalance, onValueChange = { inputBalance = it }, label = { Text("How much do you have right now?") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    val newBalance = inputBalance.toFloatOrNull() ?: 0f
                    initialBalance = newBalance
                    prefs.edit().putFloat("initial_balance", newBalance).commit()
                    showBalanceDialog = false
                }) { Text("Save") }
            }
        )
    }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Transaction?") },
            text = { Text("Are you sure you want to delete '${expenseToDelete?.merchant}'? This cannot be undone.") },
            confirmButton = {
                Button(onClick = { expenseToDelete?.id?.let { deleteExpenseFromBackend(it) }; expenseToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { expenseToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showManualDialog) ManualAddDialog(onDismiss = { showManualDialog = false }) {
        addExpenseToBackend(merchant = it.merchant ?: "Unknown", amount = it.amount?.toString() ?: "0", category = it.category ?: "Other", type = it.type ?: "Debit")
        showManualDialog = false
    }
}