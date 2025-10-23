package com.nibodhdaware.intentionality.ui.prompt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.nibodhdaware.intentionality.supabase.SupabaseRepository
import com.nibodhdaware.intentionality.ui.theme.IntentionalityTheme
import kotlinx.coroutines.launch

class IntentionPromptActivity : ComponentActivity() {
    private val supabaseRepository = SupabaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName = intent.getStringExtra("app_name") ?: "this app"
        val packageName = intent.getStringExtra("package_name") ?: ""

        setContent {
            IntentionalityTheme(darkTheme = true) {
                IntentionPromptScreen(
                    appName = appName,
                    onSubmit = { reason, rating ->
                        lifecycleScope.launch {
                            saveEntry(appName, packageName, reason, rating)
                            finish()
                        }
                    },
                    onGoBack = {
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
    }

    private suspend fun saveEntry(
        appName: String,
        packageName: String,
        reason: String,
        rating: Int
    ) {
        try {
            val userId = supabaseRepository.getUserId() ?: "anonymous"
            supabaseRepository.saveAppEntry(
                appName = appName,
                packageName = packageName,
                reason = reason,
                rating = rating,
                userId = userId
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentionPromptScreen(
    appName: String,
    onSubmit: (String, Int) -> Unit,
    onGoBack: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var selectedRating by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }

    val ratings = listOf(
        "1 - Very intentional",
        "2 - Somewhat intentional",
        "3 - Not intentional",
        "4 - Mindless",
        "5 - Regretful"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "Why are you opening",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = appName + "?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Reason input
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Your reason") },
                placeholder = { Text("Type your reason...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )

            // Intentionality dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (selectedRating > 0) ratings[selectedRating - 1] else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Intentionality Level") },
                    placeholder = { Text("Select level...") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ratings.forEachIndexed { index, rating ->
                        DropdownMenuItem(
                            text = { Text(rating) },
                            onClick = {
                                selectedRating = index + 1
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue button
            Button(
                onClick = {
                    if (reason.isNotBlank() && selectedRating > 0) {
                        onSubmit(reason, selectedRating)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = reason.isNotBlank() && selectedRating > 0
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Go Back button
            OutlinedButton(
                onClick = onGoBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Go Back to Home",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

