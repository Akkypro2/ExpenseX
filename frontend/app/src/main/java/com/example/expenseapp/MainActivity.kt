package com.example.expenseapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.expenseapp.models.ChatMessagePayload
import com.example.expenseapp.network.RetrofitInstance
import com.example.expenseapp.ui.components.FloatingBottomBar
import com.example.expenseapp.ui.screens.ChatScreen
import com.example.expenseapp.ui.screens.DashboardScreen
import com.example.expenseapp.ui.screens.LoginScreen
import com.example.expenseapp.ui.screens.RegisterScreen
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ExpenseX", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, "Logout", tint = Color.Red)
                    }
                }
            )
        }
        // Notice we REMOVED the default bottomBar completely!
    ) { innerPadding ->

        // We use a Box here so the floating nav bar can sit ON TOP of your screens
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // 1. The Background Screens
            if (currentScreenName == "Dashboard") {
                DashboardScreen()
            } else {
                ChatScreen(chatHistory, chatDisplay)
            }

            // 2. The Floating Navigation Bar Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Lock to the bottom center
                    .padding(start = 32.dp, end = 32.dp, bottom = 24.dp) // Make it float safely above the bottom edge
            ) {
                FloatingBottomBar(
                    // Convert to lowercase to match the "dashboard" and "chat" logic inside FloatingBottomBar
                    currentScreen = currentScreenName.lowercase(),
                    onScreenSelected = { selected ->
                        // Map it back to your capitalized state names
                        if (selected == "dashboard") onScreenChange("Dashboard")
                        if (selected == "chat") onScreenChange("Chat")
                    }
                )
            }
        }
    }
}
