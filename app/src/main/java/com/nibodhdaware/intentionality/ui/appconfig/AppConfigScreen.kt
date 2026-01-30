package com.nibodhdaware.intentionality.ui.appconfig

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nibodhdaware.intentionality.billing.BillingManager
import com.nibodhdaware.intentionality.database.MonitoredApp
import com.nibodhdaware.intentionality.ui.applist.AppListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    packageName: String,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: (() -> Unit)? = null,
    viewModel: AppListViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Premium status
    val isPremium by BillingManager.isPremium.collectAsState()
    
    // Fetch the app data
    var app by remember { mutableStateOf<MonitoredApp?>(null) }
    
    LaunchedEffect(packageName) {
        app = viewModel.getMonitoredApp(packageName)
    }
    
    // Show loading while fetching
    if (app == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    val currentApp = app!!
    
    var intervalMinutes by remember { mutableStateOf(currentApp.intervalMinutes) }
    var customIntention by remember { mutableStateOf(currentApp.customIntention) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure ${currentApp.appName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Info Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = currentApp.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentApp.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Custom Intention Section - Premium Feature
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Intention",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isPremium) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Premium",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    Text(
                        text = if (isPremium) "Set a custom prompt for this app" else "Create personalized prompts for each app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (isPremium) {
                        OutlinedTextField(
                            value = customIntention,
                            onValueChange = { customIntention = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Custom intention prompt") },
                            placeholder = { Text("e.g., \"Why are you opening this app right now?\"") },
                            minLines = 2,
                            maxLines = 4,
                            supportingText = {
                                Text(
                                    text = if (customIntention.isBlank()) "Leave empty to use default prompt" else "${customIntention.length} characters"
                                )
                            }
                        )
                    } else {
                        // Upgrade prompt for non-premium users
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Uses default: \"What's your intention?\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onNavigateToPaywall?.invoke() },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upgrade to Customize")
                            }
                        }
                    }
                }
            }

            // Repeat Interval Section - Premium Feature
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overlay Repeat Interval",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isPremium) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Premium",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    Text(
                        text = if (isPremium) {
                            if (intervalMinutes == 0) "Show overlay every time you open the app"
                            else "Show overlay every $intervalMinutes ${if (intervalMinutes == 1) "minute" else "minutes"}"
                        } else "Customize how often the intention prompt appears",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (isPremium) {
                        Slider(
                            value = intervalMinutes.toFloat(),
                            onValueChange = { intervalMinutes = it.toInt() },
                            valueRange = 0f..60f,
                            steps = 59
                        )
                        
                        // Quick presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0, 5, 15, 30, 60).forEach { minutes ->
                                FilledTonalButton(
                                    onClick = { intervalMinutes = minutes },
                                    modifier = Modifier.weight(1f),
                                    colors = if (intervalMinutes == minutes) {
                                        ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        ButtonDefaults.filledTonalButtonColors()
                                    },
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (minutes == 0) "Every" else "$minutes",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    } else {
                        // Upgrade prompt for non-premium users
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Default: Every time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onNavigateToPaywall?.invoke() },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upgrade to Customize")
                            }
                        }
                    }
                }
            }

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = if (intervalMinutes == 0) "• Shows overlay every time you open the app"
                        else "• Shows overlay every $intervalMinutes ${if (intervalMinutes == 1) "minute" else "minutes"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = if (customIntention.isNotBlank()) "• Custom prompt: \"${customIntention.take(30)}${if (customIntention.length > 30) "..." else ""}\"" else "• Using default prompt",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        val updatedApp = currentApp.copy(
                            intervalMinutes = intervalMinutes,
                            customIntention = if (isPremium) customIntention else ""
                        )
                        viewModel.updateMonitoredApp(updatedApp)
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Save Configuration",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
