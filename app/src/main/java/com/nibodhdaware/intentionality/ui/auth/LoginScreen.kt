package com.nibodhdaware.intentionality.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Logo/Title
            Text(
                text = "Intentionality",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Be mindful of your app usage",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Google Sign-In Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.signInWithGoogle(context)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Sign in with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Error message
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            // Skip for now (development only)
            TextButton(
                onClick = {
                    viewModel.skipLogin()
                    onLoginSuccess()
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Skip for now (Dev Mode)")
            }
        }

        // Navigate on successful login
        LaunchedEffect(uiState.isLoggedIn) {
            if (uiState.isLoggedIn) {
                onLoginSuccess()
            }
        }
    }
}

