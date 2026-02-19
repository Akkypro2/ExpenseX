package com.example.expenseapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.expenseapp.GoogleLoginRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 1. Check for saved token
        val prefs = getSharedPreferences("expense_prefs", MODE_PRIVATE)
        val savedToken = prefs.getString("jwt_token", null)

        // 2. Initialize Networking
        if (savedToken != null) {
            RetrofitInstance.jwtToken = savedToken
        }

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF6200EE),
                    secondary = Color(0xFF03DAC5),
                    background = Color(0xFFF5F5F5)
                )
            ) {
                // Determine start screen
                val startScreen = if (savedToken != null) "Dashboard" else "Login"
                AppNavigation(startScreen)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun AppNavigation(startScreen: String) {
    var currentScreen by remember { mutableStateOf(startScreen) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Dashboard State Hoisting (Persists chat when switching tabs)
    val chatHistory = remember { mutableStateListOf<ChatMessagePayload>() }
    val chatDisplay = remember { mutableStateListOf<ChatMessagePayload>() }

    // --- AUTH ACTIONS ---
    fun onLoginSuccess(token: String) {
        // Save to Prefs
        val prefs = context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("jwt_token", token).apply()

        // Set Global Token
        RetrofitInstance.jwtToken = token

        currentScreen = "Dashboard"
    }

    fun onLogout() {
        val prefs = context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("jwt_token").apply()
        RetrofitInstance.jwtToken = null
        currentScreen = "Login"

        // Clear chat history on logout
        chatHistory.clear()
        chatDisplay.clear()
    }

    when (currentScreen) {
        "Login" -> LoginScreen(
            onLoginSuccess = { token -> onLoginSuccess(token) },
            onNavigateToRegister = { currentScreen = "Register" }
        )
        "Register" -> RegisterScreen(
            onRegisterSuccess = { token -> onLoginSuccess(token) },
            onNavigateToLogin = { currentScreen = "Login" }
        )
        "Dashboard", "Chat" -> MainScreenHolder(
            currentScreenName = currentScreen,
            onScreenChange = { currentScreen = it },
            chatHistory = chatHistory,
            chatDisplay = chatDisplay,
            onLogout = { onLogout() }
        )
    }
}

// --- 1. LOGIN SCREEN ---
@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- GOOGLE SIGN IN CONFIG ---
    // KEEP YOUR EXISTING CLIENT ID HERE!
    val webClientId = "5838840209-nrcp322bk07qrtsqnibhnd9ph0uglpl7.apps.googleusercontent.com"

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val googleIdToken = account.idToken

            if (googleIdToken != null) {
                isLoading = true

                // --- STEP 1: Exchange Google Token for Firebase Credential ---
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)

                // --- STEP 2: Sign In to Firebase ---
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            // --- STEP 3: Get the FIREBASE Token (This is what Python wants!) ---
                            val user = FirebaseAuth.getInstance().currentUser
                            user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    val firebaseToken = tokenTask.result?.token
                                    if (firebaseToken != null) {
                                        // --- STEP 4: Send to Python Backend ---
                                        scope.launch {
                                            try {
                                                val response = RetrofitInstance.api.googleLogin(GoogleLoginRequest(firebaseToken))
                                                onLoginSuccess(response.accessToken)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Backend Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Failed to get Firebase Token", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            isLoading = false
                            Toast.makeText(context, "Firebase Auth Failed: ${authTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        } catch (e: ApiException) {
            isLoading = false
            Toast.makeText(context, "Google Sign-In Failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    // --- UI LAYOUT (No changes needed here) ---
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome Back! \uD83D\uDC4B", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isLoading = true
                scope.launch {
                    try {
                        val response = RetrofitInstance.api.login(email, password)
                        onLoginSuccess(response.accessToken)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White) else Text("Login")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("OR", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else Text("Continue with Google", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Sign Up")
        }
    }
}

// --- 2. REGISTER SCREEN ---
@Composable
fun RegisterScreen(onRegisterSuccess: (String) -> Unit, onNavigateToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create Account \uD83D\uDE80", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isLoading = true
                scope.launch {
                    try {
                        val response = RetrofitInstance.api.register(RegisterRequest(email, password))
                        onRegisterSuccess(response.accessToken)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Register Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White) else Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Login")
        }
    }
}

// --- 3. MAIN DASHBOARD HOLDER ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenHolder(
    currentScreenName: String,
    onScreenChange: (String) -> Unit,
    chatHistory: MutableList<ChatMessagePayload>,
    chatDisplay: MutableList<ChatMessagePayload>,
    onLogout: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ExpenseX", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, "Logout", tint = Color.Red)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Dashboard") },
                    selected = currentScreenName == "Dashboard",
                    onClick = { onScreenChange("Dashboard") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Face, "AI Chat") },
                    label = { Text("Ask AI") },
                    selected = currentScreenName == "Chat",
                    onClick = { onScreenChange("Chat") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (currentScreenName == "Dashboard") {
                BeautifulDashboard()
            } else {
                ChatBotScreen(chatHistory, chatDisplay)
            }
        }
    }
}

@Composable
fun BeautifulDashboard() {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // --- STATE ---
    var expenseList by remember { mutableStateOf(listOf<ExpenseRequest>()) }
    var isLoading by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }

    // --- API FUNCTIONS ---

    fun fetchExpenses() {
        scope.launch {
            try {
                // Now authenticated!
                val expenses = RetrofitInstance.api.getExpenses()
                expenseList = expenses
            } catch (e: Exception) {
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) { fetchExpenses() }

    fun addExpenseToBackend(merchant: String, amount: String, category: String, type: String) {
        scope.launch {
            try {
                val newExpense = ExpenseRequest(
                    merchant = merchant,
                    amount = amount.toFloatOrNull() ?: 0f,
                    date = getCurrentDate(),
                    category = category,
                    type = type
                )
                // UPDATED: Now returns ExpenseResponse directly
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

                // UPDATED: Now returns ReceiptAnalysis directly
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            DashboardCard(expenseList)
            Spacer(modifier = Modifier.height(16.dp))

            if (expenseList.isNotEmpty()) {
                Text("Spending Analysis", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                ExpensePieChart(expenseList)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("History", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(expenseList) { item -> ExpenseItem(item) }
            }
        }
    }

    if (showManualDialog) ManualAddDialog(onDismiss = { showManualDialog = false }) {
        addExpenseToBackend(
            merchant = it.merchant ?: "Unknown",
            amount = it.amount?.toString() ?: "0",
            category = it.category ?: "Other",
            type = it.type ?: "Debit"
        )
        showManualDialog = false
    }
}

// --- CHATBOT SCREEN ---
@Composable
fun ChatBotScreen(
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

// --- HELPERS ---

@Composable
fun ExpensePieChart(expenses: List<ExpenseRequest>) {
    val total = expenses.filter { it.type == "Debit" }.sumOf { it.amount?.toDouble() ?: 0.0 }
    if (total == 0.0) return

    val categories = expenses.filter { it.type == "Debit" }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount?.toDouble() ?: 0.0 }.toFloat() }

    val colors = listOf(Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF5722), Color(0xFFFFC107), Color(0xFFE91E63))

    // 1. Add Animation for a premium feel
    var animationPlayed by remember { mutableStateOf(false) }
    val animateSweep by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "pie_animation"
    )

    LaunchedEffect(Unit) { animationPlayed = true }

    Row(
        modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(120.dp)) {
                // 2. Draw a faint background track so the chart doesn't look empty
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

            // 3. Add explicit text explaining what the circle is
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Spent", fontSize = 10.sp, color = Color.Gray)
                Text("₹${total.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // 4. Make the legend scrollable in case they add many categories!
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

@Composable
fun DashboardCard(expenseList: List<ExpenseRequest>) {
    val initialBalance = 5000.0
    val totalExpense = expenseList.filter { it.type == "Debit" }.sumOf { it.amount?.toDouble() ?: 0.0 }
    val totalIncome = expenseList.filter { it.type == "Credit" }.sumOf { it.amount?.toDouble() ?: 0.0 }
    val currentBalance = initialBalance + totalIncome - totalExpense

    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF292929))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Total Expense 📉", color = Color.Gray, fontSize = 12.sp)
                Text("₹ ${String.format("%.2f", totalExpense)}", color = Color(0xFFFF5252), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Current Balance 💰", color = Color.Gray, fontSize = 12.sp)
                    Text("₹ ${String.format("%.2f", currentBalance)}", color = Color(0xFF4CAF50), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Text("(Start: ₹5000)", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

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

@Composable
fun ExpenseItem(item: ExpenseRequest) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
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

// --- UTILS ---

fun getCurrentDate(): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

fun uriToFile(context: Context, uri: Uri): File {
    val inputStream = context.contentResolver.openInputStream(uri)
    val originalBitmap = BitmapFactory.decodeStream(inputStream)
    val ratio = 512.0 / originalBitmap.width.coerceAtLeast(1)
    val height = (originalBitmap.height * ratio).toInt()
    val width = 512
    val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
    val file = File(context.cacheDir, "temp_receipt_compressed.jpg")
    val outputStream = FileOutputStream(file)
    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
    outputStream.flush()
    outputStream.close()
    return file
}