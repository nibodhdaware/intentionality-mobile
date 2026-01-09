package com.nibodhdaware.intentionality.ui.prompt

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nibodhdaware.intentionality.ui.theme.IntentionalityTheme

class IntentionPromptActivity : ComponentActivity() {
    companion object {
        private const val TAG = "IntentionPromptActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "===== IntentionPromptActivity onCreate =====")

        val appName = intent.getStringExtra("app_name") ?: "this app"
        val packageName = intent.getStringExtra("package_name") ?: ""
        
        Log.d(TAG, "Received app_name: $appName")
        Log.d(TAG, "Received package_name: $packageName")

        try {
            setContent {
                IntentionalityTheme(darkTheme = true) {
                    IntentionPromptScreen(
                        appName = appName,
                        packageName = packageName,
                        onContinue = { _, _ ->
                            Log.d(TAG, "Continue clicked - launching app")
                            // Launch the app
                            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                            launchIntent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            if (launchIntent != null) {
                                Log.d(TAG, "Launching app: $packageName")
                                startActivity(launchIntent)
                            } else {
                                Log.e(TAG, "No launch intent found for: $packageName")
                                Toast.makeText(this@IntentionPromptActivity, "Could not launch app", Toast.LENGTH_SHORT).show()
                            }
                            finish()
                        },
                        onCancel = {
                            Log.d(TAG, "Cancel clicked")
                            // Return to home screen
                            val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                addCategory(android.content.Intent.CATEGORY_HOME)
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            finish()
                        }
                    )
                }
            }
            Log.d(TAG, "✅ Content set successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in onCreate", e)
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentionPromptScreen(
    appName: String,
    packageName: String,
    onContinue: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf("") }

    // Distraction level options - without emojis
    data class DistractionOption(val value: String, val label: String)
    val options = listOf(
        DistractionOption("productive", "Actually Productive!"),
        DistractionOption("slightly_distracted", "Slightly Distracted"),
        DistractionOption("pretty_distracted", "Pretty Distracted"),
        DistractionOption("very_distracted", "Very Distracted"),
        DistractionOption("extremely_distracted", "Extremely Distracted")
    )

    // Full-screen with gradient background using theme colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Section - Minimal
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Before You Continue...",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "You're about to open",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Question 1: Why are you opening this app?
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Why are you opening this app?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                "e.g., Check messages from friends, Learn something new...", 
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Question 2: How productive is this?
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "How productive is this?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    // Radio button options
                    options.forEach { option ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedOption == option.value) 
                                MaterialTheme.colorScheme.primaryContainer
                            else 
                                MaterialTheme.colorScheme.surface,
                            border = if (selectedOption == option.value)
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            onClick = { selectedOption = option.value }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selectedOption == option.value)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selectedOption == option.value) FontWeight.SemiBold else FontWeight.Normal
                                )
                                
                                if (selectedOption == option.value) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Add space for buttons (fixed at bottom)
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Fixed Buttons at Bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Go Back Button
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Go Back",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Continue Button
                Button(
                    onClick = {
                        if (message.isNotBlank() && selectedOption.isNotEmpty()) {
                            onContinue(message, selectedOption)
                        }
                    },
                    enabled = message.isNotBlank() && selectedOption.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Continue to App",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}