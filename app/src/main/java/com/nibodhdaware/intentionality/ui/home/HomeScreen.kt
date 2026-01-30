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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.nibodhdaware.intentionality.api.ApiManager
import com.nibodhdaware.intentionality.api.ApiUser
import com.nibodhdaware.intentionality.billing.BillingManager
import com.nibodhdaware.intentionality.database.IntentionLog
import com.nibodhdaware.intentionality.ui.applist.AppListViewModel
import com.nibodhdaware.intentionality.ui.onboarding.FeatureDiscoveryOverlay
import com.nibodhdaware.intentionality.ui.onboarding.FeatureHighlight
import com.nibodhdaware.intentionality.ui.onboarding.HighlightCoordinatesState
import com.nibodhdaware.intentionality.ui.onboarding.OnboardingPreferences
import com.nibodhdaware.intentionality.ui.onboarding.trackHighlight
import com.nibodhdaware.intentionality.ui.theme.MistTeal
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
    val installedApps by viewModel.installedMonitoredApps.collectAsState()
    val todaysLogs by viewModel.todaysLogs.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    // List state for scroll-based animations
    val listState = rememberLazyListState()
    
    // Calculate alpha based on scroll
    val scrollOffset = remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }
    val graphAlpha by animateFloatAsState(
        targetValue = when {
            listState.firstVisibleItemIndex > 0 -> 0f
            scrollOffset.value > 300 -> 0f
            scrollOffset.value > 0 -> 1f - (scrollOffset.value / 300f)
            else -> 1f
        },
        animationSpec = tween(durationMillis = 200),
        label = "graphAlpha"
    )

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
        if (isGranted) { }
    }
    
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }
    
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
    
    LaunchedEffect(showFeatureDiscovery) {
        if (showFeatureDiscovery) currentHighlight = FeatureHighlight.SETTINGS_BUTTON
    }
    
    fun advanceHighlight() {
        currentHighlight = when (currentHighlight) {
            FeatureHighlight.SETTINGS_BUTTON -> FeatureHighlight.ADD_APPS_FAB
            FeatureHighlight.ADD_APPS_FAB -> FeatureHighlight.START_MONITORING
            FeatureHighlight.START_MONITORING -> FeatureHighlight.COMPLETE
            FeatureHighlight.COMPLETE -> FeatureHighlight.COMPLETE
        }
        if (currentHighlight == FeatureHighlight.COMPLETE) onFeatureDiscoveryComplete()
    }
    
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } == AppOpsManager.MODE_ALLOWED
    }
    
    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        placeholder = { Text("Search apps...", style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Settings Icon
                    IconButton(
                        onClick = { onNavigateToProfile?.invoke() },
                        modifier = Modifier
                            .size(48.dp)
                            .trackHighlight(highlightState, FeatureHighlight.SETTINGS_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    if (BillingManager.canAddMoreApps(monitoredApps.size)) {
                        onNavigateToAddApps()
                    } else {
                        onNavigateToPaywall?.invoke()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Apps", fontWeight = FontWeight.Bold) },
                modifier = Modifier.trackHighlight(highlightState, FeatureHighlight.ADD_APPS_FAB)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Graph Area with Smooth Fade
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp * graphAlpha)
                            .alpha(graphAlpha)
                            .padding(vertical = 12.dp)
                    ) {
                        DumbnessRatingGraph(
                            logs = todaysLogs,
                            monitoredAppsCount = monitoredApps.size,
                            isMonitoring = isMonitoring,
                            isPremium = isPremium,
                            onNavigateToPaywall = { onNavigateToPaywall?.invoke() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // Monitoring Control Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isMonitoring) "Monitoring Active" else "Monitoring Paused",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMonitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${installedApps.size} apps monitored",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Button(
                                onClick = {
                                    if (isMonitoring) viewModel.stopMonitoring()
                                    else {
                                        if (!hasUsageStatsPermission()) showUsagePermissionDialog = true
                                        else if (!hasOverlayPermission()) showOverlayPermissionDialog = true
                                        else viewModel.startMonitoring()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isMonitoring) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.trackHighlight(highlightState, FeatureHighlight.START_MONITORING)
                            ) {
                                Icon(
                                    imageVector = if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isMonitoring) "Stop" else "Start", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                // App List Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monitored Apps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (monitoredApps.isNotEmpty()) {
                            IconButton(onClick = { isEditMode = !isEditMode }) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                if (installedApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No apps added yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    val filteredList = if (searchQuery.isBlank()) installedApps 
                                     else installedApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
                    
                    items(filteredList, key = { it.packageName }) { app ->
                        MonitoredAppItem(
                            app = app,
                            viewModel = viewModel,
                            isEditMode = isEditMode,
                            onConfigure = onNavigateToAppConfig
                        )
                    }
                }
            }
        }
    }

    // Dialogs remain unchanged
    if (showUsagePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showUsagePermissionDialog = false },
            title = { Text("Usage Access Required") },
            text = { Text("To monitor apps, Intentionality needs usage access permission.") },
            confirmButton = {
                TextButton(onClick = {
                    showUsagePermissionDialog = false
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }) { Text("Open Settings") }
            },
            dismissButton = { TextButton(onClick = { showUsagePermissionDialog = false }) { Text("Cancel") } }
        )
    }
    
    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = { Text("Overlay Permission Required") },
            text = { Text("To show intention prompts, please grant 'Display over other apps' permission.") },
            confirmButton = {
                TextButton(onClick = {
                    showOverlayPermissionDialog = false
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                }) { Text("Open Settings") }
            },
            dismissButton = { TextButton(onClick = { showOverlayPermissionDialog = false }) { Text("Cancel") } }
        )
    }
    
    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNotificationPermissionDialog = false
                scope.launch { onboardingPreferences.setNotificationPermissionAsked() }
            },
            title = { Text("Enable Notifications") },
            text = { Text("Stay updated with a persistent notification while monitoring.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationPermissionDialog = false
                    scope.launch { onboardingPreferences.setNotificationPermissionAsked() }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showNotificationPermissionDialog = false
                    scope.launch { onboardingPreferences.setNotificationPermissionAsked() }
                }) { Text("Not Now") }
            }
        )
    }
    
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
    app: com.nibodhdaware.intentionality.database.MonitoredApp,
    viewModel: AppListViewModel,
    isEditMode: Boolean = false,
    onConfigure: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val appIcon = remember(app.packageName) {
        try { context.packageManager.getApplicationIcon(app.packageName) }
        catch (e: Exception) { context.packageManager.defaultActivityIcon }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberDrawablePainter(appIcon),
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
            )
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(text = app.appName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "Every ${app.intervalMinutes}m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            if (isEditMode) {
                IconButton(onClick = { viewModel.deleteMonitoredApp(app.packageName) }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            } else {
                IconButton(onClick = { onConfigure?.invoke(app.packageName) }) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumbnessRatingGraph(
    logs: List<IntentionLog>,
    monitoredAppsCount: Int = 0,
    isMonitoring: Boolean = false,
    isPremium: Boolean = true,
    onNavigateToPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val sortedLogs = remember(logs) { logs.sortedBy { it.timestamp } }
    var showDetailsDialog by remember { mutableStateOf(false) }
    
    Card(
        onClick = { if (logs.isNotEmpty() && isPremium) showDetailsDialog = true },
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Focus Pulse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "${logs.size} intentions today", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
                    }
                    
                    if (logs.isNotEmpty() && isPremium) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.clickable { showDetailsDialog = true }
                        ) {
                            Text(
                                text = "Details",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                if (sortedLogs.size >= 1 && isPremium) {
                    // Graph implementation remains similar but styled
                    val minTimestamp = sortedLogs.first().timestamp
                    val maxTimestamp = if (sortedLogs.size > 1) sortedLogs.last().timestamp else minTimestamp + 3600000L
                    val timeRange = (maxTimestamp - minTimestamp).coerceAtLeast(1L)
                    
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            val paddingTop = 10.dp.toPx()
                            val paddingBottom = 10.dp.toPx()
                            val graphHeight = height - paddingTop - paddingBottom
                            
                            // Horizontal grid
                            for (rating in 1..5) {
                                val y = paddingTop + graphHeight - ((rating - 1f) / 4f) * graphHeight
                                drawLine(
                                    color = outlineColor.copy(alpha = 0.05f),
                                    start = Offset(0f, y),
                                    end = Offset(width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            
                            if (sortedLogs.size >= 2) {
                                val path = Path()
                                sortedLogs.forEachIndexed { index, log ->
                                    val x = ((log.timestamp - minTimestamp).toFloat() / timeRange) * width
                                    val y = paddingTop + graphHeight - ((log.dumbnessRating - 1f) / 4f) * graphHeight
                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path = path, color = primaryColor, style = Stroke(width = 3.dp.toPx()))
                            }
                            
                            sortedLogs.forEach { log ->
                                val x = ((log.timestamp - minTimestamp).toFloat() / timeRange) * width
                                val y = paddingTop + graphHeight - ((log.dumbnessRating - 1f) / 4f) * graphHeight
                                drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = Offset(x, y))
                            }
                        }
                    }
                } else if (!isPremium) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(32.dp), tint = primaryColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Premium Insight", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            TextButton(onClick = onNavigateToPaywall) { Text("Unlock Flow Graph") }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No intentions logged yet", style = MaterialTheme.typography.bodyMedium, color = onSurfaceVariant)
                    }
                }
            }
        }
    }
    
    // Details Sheet implementation remains similar but styled
    if (showDetailsDialog) {
        ModalBottomSheet(
            onDismissRequest = { showDetailsDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(text = "Your Rituals", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 32.dp)) {
                    items(sortedLogs.reversed()) { log ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MistTeal,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = log.appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(text = timeFormat.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = log.reason, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
