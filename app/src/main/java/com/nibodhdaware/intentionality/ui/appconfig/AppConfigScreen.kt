package com.nibodhdaware.intentionality.ui.appconfig

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nibodhdaware.intentionality.database.MonitoredApp
import com.nibodhdaware.intentionality.ui.applist.AppListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    packageName: String,
    onNavigateBack: () -> Unit,
    viewModel: AppListViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
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
    
    var allDay by remember { mutableStateOf(currentApp.allDay) }
    var startHour by remember { mutableStateOf(currentApp.startHour) }
    var startMinute by remember { mutableStateOf(currentApp.startMinute) }
    var endHour by remember { mutableStateOf(currentApp.endHour) }
    var endMinute by remember { mutableStateOf(currentApp.endMinute) }
    var intervalMinutes by remember { mutableStateOf(currentApp.intervalMinutes) }

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

            // Time Schedule Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Active Time Window",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // All Day Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active All Day")
                        Switch(
                            checked = allDay,
                            onCheckedChange = { allDay = it }
                        )
                    }

                    if (!allDay) {
                        HorizontalDivider()
                        
                        // Start Time Picker
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        startHour = hour
                                        startMinute = minute
                                    },
                                    startHour,
                                    startMinute,
                                    true
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Start Time",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = String.format("%02d:%02d", startHour, startMinute),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // End Time Picker
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        endHour = hour
                                        endMinute = minute
                                    },
                                    endHour,
                                    endMinute,
                                    true
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "End Time",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = String.format("%02d:%02d", endHour, endMinute),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Repeat Interval Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Overlay Repeat Interval",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "Show overlay every $intervalMinutes ${if (intervalMinutes == 1) "minute" else "minutes"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Slider(
                        value = intervalMinutes.toFloat(),
                        onValueChange = { intervalMinutes = it.toInt() },
                        valueRange = 1f..60f,
                        steps = 58
                    )
                    
                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 30, 60).forEach { minutes ->
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
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "$minutes",
                                    fontWeight = FontWeight.Bold
                                )
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
                    
                    val timeText = if (allDay) {
                        "Active all day"
                    } else {
                        "Works from ${String.format("%02d:%02d", startHour, startMinute)} to ${String.format("%02d:%02d", endHour, endMinute)}"
                    }
                    
                    Text(
                        text = "• $timeText",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "• Shows overlay every $intervalMinutes ${if (intervalMinutes == 1) "minute" else "minutes"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        val updatedApp = currentApp.copy(
                            allDay = allDay,
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            intervalMinutes = intervalMinutes
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
