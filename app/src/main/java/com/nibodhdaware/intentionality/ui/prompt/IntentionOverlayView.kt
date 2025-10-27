package com.nibodhdaware.intentionality.ui.prompt

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nibodhdaware.intentionality.supabase.SupabaseRepository
import com.nibodhdaware.intentionality.ui.theme.IntentionalityTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentionOverlayView(
    appName: String,
    packageName: String,
    onProceed: (String, Int) -> Unit,
    onGoBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val supabaseRepository = remember { SupabaseRepository() }
    
    var reason by remember { mutableStateOf("") }
    var selectedRating by remember { mutableStateOf(0) }

    // Distraction level ratings
    data class DistractionOption(val rating: Int, val label: String, val emoji: String)
    val distractionOptions = listOf(
        DistractionOption(1, "Actually Productive!", "🎯"),
        DistractionOption(2, "Slightly Distracted", "😅"),
        DistractionOption(3, "Pretty Distracted", "😬"),
        DistractionOption(4, "Very Distracted", "😫"),
        DistractionOption(5, "Extremely Distracted", "🤦‍♂️")
    )

    IntentionalityTheme(darkTheme = false) {
        // Full-screen overlay with semi-transparent background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        ) {
            // Main content card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = "Why are you opening",
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                    )

                    // Reason input
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Your reason") },
                        placeholder = { Text("Type your reason here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    // Distraction level selection (Radio button style)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = "How dumb is this reason? 🤔",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        distractionOptions.forEach { option ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = if (selectedRating == option.rating) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selectedRating == option.rating) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    }
                                ),
                                onClick = { selectedRating = option.rating }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = option.emoji,
                                            style = MaterialTheme.typography.headlineSmall,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                        Text(
                                            text = option.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (selectedRating == option.rating) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                    
                                    RadioButton(
                                        selected = selectedRating == option.rating,
                                        onClick = { selectedRating = option.rating },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Buttons in a row - Go Back on left, Submit on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Go Back button (left side)
                        OutlinedButton(
                            onClick = onGoBack,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE63946) // Red color for cancel action
                            ),
                            border = BorderStroke(2.dp, Color(0xFFE63946)),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "Go Back",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Submit button (right side)
                        Button(
                            onClick = {
                                if (reason.isNotBlank() && selectedRating > 0) {
                                    // Save to database
                                    scope.launch {
                                        try {
                                            val userId = supabaseRepository.getUserId() ?: "anonymous"
                                            supabaseRepository.saveAppEntry(
                                                appName = appName,
                                                packageName = packageName,
                                                reason = reason,
                                                rating = selectedRating,
                                                userId = userId
                                            )
                                            Log.d("IntentionOverlayView", "✅ Entry saved successfully!")
                                            
                                            // Call onProceed to launch the app
                                            onProceed(reason, selectedRating)
                                        } catch (e: Exception) {
                                            Log.e("IntentionOverlayView", "❌ Error saving entry", e)
                                            Toast.makeText(context, "Failed to save entry", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            ),
                            enabled = reason.isNotBlank() && selectedRating > 0,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "Submit",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


