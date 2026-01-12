package com.nibodhdaware.intentionality.ui.home

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.core.content.ContextCompat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.nibodhdaware.intentionality.billing.BillingManager
import com.nibodhdaware.intentionality.database.IntentionLog
import com.nibodhdaware.intentionality.ui.applist.AppListViewModel
import com.nibodhdaware.intentionality.ui.onboarding.FeatureDiscoveryOverlay
import com.nibodhdaware.intentionality.ui.onboarding.FeatureHighlight
import com.nibodhdaware.intentionality.ui.onboarding.HighlightCoordinatesState
import com.nibodhdaware.intentionality.ui.onboarding.OnboardingPreferences
import com.nibodhdaware.intentionality.ui.onboarding.trackHighlight
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddApps: () -> Unit,
    onNavigateToProfile: (() -> Unit)? = null,
    onNavigateToAppConfig: ((String) -> Unit)? = null,
    onNavigateToPaywall: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = viewModel(),
    showFeatureDiscovery: Boolean = false,
    onFeatureDiscoveryComplete: () -> Unit = {},
    requestNotificationPermission: Boolean = false,
    onNotificationPermissionRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onboardingPreferences = remember { OnboardingPreferences(context) }
    val monitoredApps by viewModel.monitoredApps.collectAsState()
    val todaysLogs by viewModel.todaysLogs.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    
    // Premium status
    val isPremium by BillingManager.isPremium.collectAsState()
    
    var showUsagePermissionDialog by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    
    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, notification will work
        }
    }
    
    // Check notification permission on first load after onboarding
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required before Android 13
        }
    }
    
    // Show notification permission dialog when requested (after onboarding/feature discovery)
    LaunchedEffect(requestNotificationPermission) {
        if (requestNotificationPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
                showNotificationPermissionDialog = true
            }
            onNotificationPermissionRequested()
        }
    }
    
    // Feature discovery state
    val highlightState = remember { HighlightCoordinatesState() }
    var currentHighlight by remember { 
        mutableStateOf(if (showFeatureDiscovery) FeatureHighlight.SETTINGS_BUTTON else FeatureHighlight.COMPLETE) 
    }
    
    // Update highlight when showFeatureDiscovery changes
    LaunchedEffect(showFeatureDiscovery) {
        if (showFeatureDiscovery) {
            currentHighlight = FeatureHighlight.SETTINGS_BUTTON
        }
    }
    
    fun advanceHighlight() {
        currentHighlight = when (currentHighlight) {
            FeatureHighlight.SETTINGS_BUTTON -> FeatureHighlight.ADD_APPS_FAB
            FeatureHighlight.ADD_APPS_FAB -> FeatureHighlight.START_MONITORING
            FeatureHighlight.START_MONITORING -> FeatureHighlight.COMPLETE
            FeatureHighlight.COMPLETE -> FeatureHighlight.COMPLETE
        }
        if (currentHighlight == FeatureHighlight.COMPLETE) {
            onFeatureDiscoveryComplete()
        }
    }
    
    // Filter to show only installed apps
    val installedApps = remember(monitoredApps) {
        monitoredApps.filter { app ->
            try {
                context.packageManager.getPackageInfo(app.packageName, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Intentionality",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                )
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToProfile?.invoke() },
                        modifier = Modifier.trackHighlight(highlightState, FeatureHighlight.SETTINGS_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    // Check if user can add more apps (premium or under free limit)
                    if (BillingManager.canAddMoreApps(monitoredApps.size)) {
                        onNavigateToAddApps()
                    } else {
                        onNavigateToPaywall?.invoke()
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("Add Apps") },
                modifier = Modifier.trackHighlight(highlightState, FeatureHighlight.ADD_APPS_FAB)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Monitoring Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (isMonitoring) "Monitoring Active" else "Monitoring Paused",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "${installedApps.size} apps being monitored",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Start/Stop Monitoring Button
                        Button(
                            onClick = {
                                if (isMonitoring) {
                                    viewModel.stopMonitoring()
                                } else {
                                    if (monitoredApps.isEmpty()) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Please add apps to monitor first",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        onNavigateToAddApps()
                                    } else if (!hasUsageStatsPermission()) {
                                        showUsagePermissionDialog = true
                                    } else if (!hasOverlayPermission()) {
                                        showOverlayPermissionDialog = true
                                    } else {
                                        viewModel.startMonitoring()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .trackHighlight(highlightState, FeatureHighlight.START_MONITORING),
                            colors = if (isMonitoring) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Icon(
                                imageVector = if (isMonitoring) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isMonitoring) "Stop Monitoring" else "Start Monitoring",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            // Today's Dumbness Rating Graph - Premium feature with paywall overlay
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    DumbnessRatingGraph(
                        logs = todaysLogs,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Paywall overlay if not premium
                    if (!isPremium) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .matchParentSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Premium Feature",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
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
                                    Text("Upgrade")
                                }
                            }
                        }
                    }
                }
            }
            
            // Monitored Apps Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monitored Apps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (monitoredApps.isNotEmpty()) {
                        IconButton(onClick = { isEditMode = !isEditMode }) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Delete,
                                contentDescription = if (isEditMode) "Done" else "Remove Apps",
                                tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (monitoredApps.isEmpty()) {
                // Empty state
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No apps monitored yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap 'Add Apps' to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Monitored apps list items
                items(installedApps, key = { it.packageName }) { app ->
                    MonitoredAppItem(
                        appName = app.appName,
                        packageName = app.packageName,
                        viewModel = viewModel,
                        isEditMode = isEditMode,
                        onConfigure = onNavigateToAppConfig
                    )
                }
            }
        }
    }

    // Usage Stats Permission Dialog
    if (showUsagePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showUsagePermissionDialog = false },
            title = { Text("Usage Access Permission Required") },
            text = { Text("To monitor apps, you need to grant usage access permission. This allows the app to detect when you open monitored apps.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUsagePermissionDialog = false
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsagePermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Overlay Permission Dialog
    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = { Text("Overlay Permission Required") },
            text = { Text("To show intention prompts over other apps, you need to grant the 'Display over other apps' permission.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOverlayPermissionDialog = false
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Notification Permission Dialog
    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNotificationPermissionDialog = false
                scope.launch { onboardingPreferences.setNotificationPermissionAsked() }
            },
            title = { Text("Enable Notifications") },
            text = { 
                Text(
                    "Intentionality needs notification permission to show a persistent status " +
                    "notification while monitoring your apps. This helps keep the app running " +
                    "in the background."
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationPermissionDialog = false
                        scope.launch { onboardingPreferences.setNotificationPermissionAsked() }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showNotificationPermissionDialog = false
                    scope.launch { onboardingPreferences.setNotificationPermissionAsked() }
                }) {
                    Text("Not Now")
                }
            }
        )
    }
    
    // Feature Discovery Overlay
    if (showFeatureDiscovery && currentHighlight != FeatureHighlight.COMPLETE) {
        FeatureDiscoveryOverlay(
            currentHighlight = currentHighlight,
            highlightBounds = highlightState.getBounds(currentHighlight),
            onNext = { advanceHighlight() },
            onSkip = {
                currentHighlight = FeatureHighlight.COMPLETE
                onFeatureDiscoveryComplete()
            }
        )
    }
}

@Composable
fun MonitoredAppItem(
    appName: String,
    packageName: String,
    viewModel: AppListViewModel,
    isEditMode: Boolean = false,
    onConfigure: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var monitoredApp by remember { mutableStateOf<com.nibodhdaware.intentionality.database.MonitoredApp?>(null) }
    
    // Cache icon synchronously for smooth scrolling
    val appIcon = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            context.packageManager.defaultActivityIcon
        }
    }

    LaunchedEffect(packageName) {
        monitoredApp = viewModel.getMonitoredApp(packageName)
    }

    ListItem(
        headlineContent = {
            Text(
                text = appName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            if (monitoredApp != null) {
                Text(
                    text = "Every ${monitoredApp!!.intervalMinutes}min${if (monitoredApp!!.customIntention.isNotBlank()) " • Custom prompt" else ""}"
                )
            }
        },
        leadingContent = {
            Image(
                painter = rememberDrawablePainter(appIcon),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        },
        trailingContent = {
            if (isEditMode) {
                IconButton(onClick = { viewModel.deleteMonitoredApp(packageName) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                IconButton(onClick = { onConfigure?.invoke(packageName) }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure"
                    )
                }
            }
        },
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumbnessRatingGraph(
    logs: List<IntentionLog>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // Sort logs by timestamp for proper line drawing
    val sortedLogs = remember(logs) { logs.sortedBy { it.timestamp } }
    
    // State for showing the details dialog
    var showDetailsDialog by remember { mutableStateOf(false) }
    
    Card(
        onClick = { if (logs.isNotEmpty()) showDetailsDialog = true },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Reason Quality",
                    style = MaterialTheme.typography.titleMedium
                )
                if (logs.isNotEmpty()) {
                    Text(
                        text = "Tap for details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${logs.size} intention${if (logs.size != 1) "s" else ""} logged today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (logs.isNotEmpty()) {
                val avgRating = logs.map { it.dumbnessRating }.average()
                Text(
                    text = "Average: %.1f/5".format(avgRating),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Line graph with axes
            if (sortedLogs.size >= 1) {
                val minTimestamp = sortedLogs.first().timestamp
                val maxTimestamp = if (sortedLogs.size > 1) sortedLogs.last().timestamp else minTimestamp + 3600000L
                val timeRange = (maxTimestamp - minTimestamp).coerceAtLeast(1L)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    // Y-axis labels (ratings 5 to 1)
                    Column(
                        modifier = Modifier
                            .width(24.dp)
                            .fillMaxHeight()
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (rating in 5 downTo 1) {
                            Text(
                                text = "$rating",
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVariant
                            )
                        }
                    }
                    
                    // Graph area
                    Column(modifier = Modifier.weight(1f)) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            val width = size.width
                            val height = size.height
                            val paddingTop = 4.dp.toPx()
                            val paddingBottom = 4.dp.toPx()
                            val graphHeight = height - paddingTop - paddingBottom
                            
                            // Draw horizontal grid lines for ratings 1-5
                            for (rating in 1..5) {
                                val y = paddingTop + graphHeight - ((rating - 1f) / 4f) * graphHeight
                                drawLine(
                                    color = outlineColor.copy(alpha = 0.2f),
                                    start = Offset(0f, y),
                                    end = Offset(width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            
                            // Draw the line connecting all points
                            if (sortedLogs.size >= 2) {
                                val path = Path()
                                var isFirst = true
                                
                                sortedLogs.forEach { log ->
                                    val x = ((log.timestamp - minTimestamp).toFloat() / timeRange) * width
                                    val y = paddingTop + graphHeight - ((log.dumbnessRating - 1f) / 4f) * graphHeight
                                    
                                    if (isFirst) {
                                        path.moveTo(x, y)
                                        isFirst = false
                                    } else {
                                        path.lineTo(x, y)
                                    }
                                }
                                
                                drawPath(
                                    path = path,
                                    color = primaryColor,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                            
                            // Draw points for each log
                            sortedLogs.forEach { log ->
                                val x = ((log.timestamp - minTimestamp).toFloat() / timeRange) * width
                                val y = paddingTop + graphHeight - ((log.dumbnessRating - 1f) / 4f) * graphHeight
                                
                                drawCircle(
                                    color = primaryColor,
                                    radius = 5.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }
                        
                        // X-axis labels (time)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = timeFormat.format(Date(minTimestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVariant
                            )
                            if (sortedLogs.size > 1) {
                                Text(
                                    text = timeFormat.format(Date(maxTimestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Axis labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Time →",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant
                    )
                }
            } else {
                // Empty state - no logs yet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No intentions logged today.\nOpen a monitored app to start tracking.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
    
    // Bottom sheet showing all reasons
    if (showDetailsDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        ModalBottomSheet(
            onDismissRequest = { showDetailsDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Text(
                    text = "Today's Intentions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${sortedLogs.size} ${if (sortedLogs.size == 1) "reason" else "reasons"} logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (sortedLogs.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = outlineColor
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No intentions logged today",
                                style = MaterialTheme.typography.bodyLarge,
                                color = onSurfaceVariant
                            )
                            Text(
                                text = "Open a monitored app to start tracking",
                                style = MaterialTheme.typography.bodySmall,
                                color = outlineColor
                            )
                        }
                    }
                } else {
                    // Scrollable list of reasons
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedLogs.reversed()) { log ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                tonalElevation = 1.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    // App name and time
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.appName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = timeFormat.format(Date(log.timestamp)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    // The reason
                                    Text(
                                        text = log.reason,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Quality rating
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(5) { index ->
                                            Icon(
                                                imageVector = if (index < log.dumbnessRating) 
                                                    Icons.Filled.Star else Icons.Outlined.Star,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = if (index < log.dumbnessRating) 
                                                    primaryColor else outlineColor.copy(alpha = 0.3f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = when(log.dumbnessRating) {
                                                1 -> "Pointless"
                                                2 -> "Weak"
                                                3 -> "Okay"
                                                4 -> "Good"
                                                5 -> "Excellent"
                                                else -> ""
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
