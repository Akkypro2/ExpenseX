package com.example.expenseapp.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expenseapp.models.GoogleLoginRequest
import com.example.expenseapp.network.RetrofitInstance
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- GOOGLE SIGN IN CONFIG ---
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
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)

                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val user = FirebaseAuth.getInstance().currentUser
                            user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    val firebaseToken = tokenTask.result?.token
                                    if (firebaseToken != null) {
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

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome Back! 👋", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
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