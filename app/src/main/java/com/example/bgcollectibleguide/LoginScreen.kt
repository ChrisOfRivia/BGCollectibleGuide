package com.example.bgcollectibleguide

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Data holder and logic handler for the Login Screen.
 * Using a ViewModel ensures state is preserved during configuration changes (like rotating the phone).
 */
class LoginViewModel : ViewModel() {
    // Current input values from the user
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    
    // UI state flags
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    
    // Firebase instance for authentication
    private val auth = FirebaseAuth.getInstance()

    /**
     * Attempt to log in with the provided credentials.
     * returns true if successful, false otherwise.
     */
    suspend fun signIn(): Boolean {
        isLoading = true
        errorMessage = null
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Login failed"
            false
        } finally {
            isLoading = false
        }
    }

    /**
     * Create a new account in Firebase.
     * returns true if successful, false otherwise.
     */
    suspend fun signUp(): Boolean {
        isLoading = true
        errorMessage = null
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Registration failed"
            false
        } finally {
            isLoading = false
        }
    }
}

/**
 * The entry point for the Login/Registration UI.
 * Provides fields for email and password and buttons for Sign In/Up.
 */
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    // Link the UI to the ViewModel logic
    val viewModel: LoginViewModel = viewModel()
    // Scope for launching coroutines from UI interactions
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BG Collectible Guide",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Email Input Field
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password Input Field with hidden text transformation
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading
        )

        // Error message display area - shown only when an error occurs
        viewModel.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.isLoading) {
            // Show a progress spinner while communicating with Firebase
            CircularProgressIndicator()
        } else {
            // Main Action Button for Sign In
            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.signIn()) onLoginSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                // Basic validation: ensure fields aren't empty before allowing click
                enabled = viewModel.email.isNotBlank() && viewModel.password.isNotBlank()
            ) {
                Text("Login")
            }

            // Link to switch to registration/sign-up
            TextButton(
                onClick = {
                    scope.launch {
                        if (viewModel.signUp()) onLoginSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Don't have an account? Sign Up")
            }
        }
    }
}
