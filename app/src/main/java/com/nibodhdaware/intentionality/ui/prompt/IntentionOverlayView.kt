package com.nibodhdaware.intentionality.ui.prompt

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nibodhdaware.intentionality.ui.theme.IntentionalityTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentionOverlayView(
    appName: String,
    packageName: String,
    onProceed: (String, Int) -> Unit,
    onGoBack: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var dumbnessRating by remember { mutableIntStateOf(0) }
    
    // Count words (at least 3 required)
    val wordCount = reason.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    val hasEnoughWords = wordCount >= 3

    // Use the app's Material 3 theme with dynamic colors
    IntentionalityTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    LargeTopAppBar(
                        title = { 
                            Text(
                                text = "Intention Check",
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        navigationIcon = {
                            IconButton(onClick = onGoBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Go Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App name card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Opening",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "What do you need to do?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Reason input
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Your intention") },
                        placeholder = { Text("e.g., Check a specific notification") },
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (hasEnoughWords) "✓ Good" else "Minimum 3 words",
                                    color = if (hasEnoughWords) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("$wordCount words")
                            }
                        },
                        isError = reason.isNotBlank() && !hasEnoughWords,
                        minLines = 3,
                        maxLines = 5,
                        shape = MaterialTheme.shapes.large
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Rating section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "How valid is this reason?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = when(dumbnessRating) {
                                    1 -> "😬 Completely pointless"
                                    2 -> "😕 Pretty weak"
                                    3 -> "😐 Somewhat valid"
                                    4 -> "🙂 Good reason"
                                    5 -> "✨ Excellent reason"
                                    else -> "Tap a star to rate"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (dumbnessRating > 0) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Star rating
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                (1..5).forEach { star ->
                                    FilledIconButton(
                                        onClick = { dumbnessRating = star },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = if (star <= dumbnessRating) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (star <= dumbnessRating) 
                                                Icons.Filled.Star 
                                            else 
                                                Icons.Outlined.Star,
                                            contentDescription = "Star $star",
                                            tint = if (star <= dumbnessRating) 
                                                MaterialTheme.colorScheme.onPrimary 
                                            else 
                                                MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Buttons
                    Button(
                        onClick = {
                            Log.d("IntentionOverlayView", "Continue: reason='$reason', rating=$dumbnessRating")
                            onProceed(reason.trim(), dumbnessRating)
                        },
                        enabled = hasEnoughWords && dumbnessRating > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = "Continue to $appName",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = {
                            Log.d("IntentionOverlayView", "Go back pressed")
                            onGoBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Go Back")
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
