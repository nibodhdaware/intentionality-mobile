package com.nibodhdaware.intentionality.ui.prompt

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.nibodhdaware.intentionality.supabase.SupabaseRepository
import com.nibodhdaware.intentionality.ui.theme.IntentionalityTheme
import kotlinx.coroutines.launch

class IntentionPromptActivity : ComponentActivity() {
    private val supabaseRepository = SupabaseRepository()

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
                IntentionalityTheme(darkTheme = false) {
                    FullScreenDialog(
                        appName = appName,
                        packageName = packageName,
                        onContinue = { message, selectedOption ->
                            Log.d(TAG, "Continue clicked: message='$message', option=$selectedOption")
                            lifecycleScope.launch {
                                val success = saveEntry(appName, packageName, message, selectedOption)
                                if (success) {
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
                                } else {
                                    Toast.makeText(this@IntentionPromptActivity, "Failed to save entry", Toast.LENGTH_SHORT).show()
                                }
                                finish()
                            }
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

    private suspend fun saveEntry(
        appName: String,
        packageName: String,
        reason: String,
        distractionLevel: String
    ): Boolean {
        return try {
            Log.d(TAG, "Saving entry to Supabase...")
            val userId = supabaseRepository.getUserId() ?: "anonymous"
            Log.d(TAG, "User ID: $userId")
            
            // Convert distraction level to rating (1-5)
            val rating = when (distractionLevel) {
                "productive" -> 1
                "slightly_distracted" -> 2
                "pretty_distracted" -> 3
                "very_distracted" -> 4
                "extremely_distracted" -> 5
                else -> 3
            }
            
            supabaseRepository.saveAppEntry(
                appName = appName,
                packageName = packageName,
                reason = reason,
                rating = rating,
                userId = userId
            )
            Log.d(TAG, "✅ Entry saved successfully!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving entry", e)
            e.printStackTrace()
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenDialog(
    appName: String,
    packageName: String,
    onContinue: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Distraction level options
    data class DistractionOption(val value: String, val label: String)
    val options = listOf(
        DistractionOption("productive", "Actually Productive! 🎯"),
        DistractionOption("slightly_distracted", "Slightly Distracted 😅"),
        DistractionOption("pretty_distracted", "Pretty Distracted 😬"),
        DistractionOption("very_distracted", "Very Distracted 😫"),
        DistractionOption("extremely_distracted", "Extremely Distracted 🤦‍♂️")
    )

    // Full-screen overlay with semi-transparent background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // White card container
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = "Configure Monitored App",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Message input
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    label = { Text("Why are you opening this app?") },
                    placeholder = { Text("Enter your reason...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Dropdown for distraction level
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    OutlinedTextField(
                        value = options.find { it.value == selectedOption }?.label ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        label = { Text("How distracted are you?") },
                        placeholder = { Text("Select an option...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    selectedOption = option.value
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Cancel button
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    // Continue button
                    Button(
                        onClick = {
                            if (message.isNotBlank() && selectedOption.isNotEmpty()) {
                                onContinue(message, selectedOption)
                            }
                        },
                        enabled = message.isNotBlank() && selectedOption.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Continue",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
