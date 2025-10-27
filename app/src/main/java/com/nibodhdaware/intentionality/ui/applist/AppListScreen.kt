package com.nibodhdaware.intentionality.ui.applist

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(modifier: Modifier = Modifier, viewModel: AppListViewModel = viewModel()) {
    val context = LocalContext.current
    val filteredApps by viewModel.filteredApps.collectAsState()
    val monitoredApps by viewModel.monitoredApps.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val isInitialized by viewModel.isInitialized.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-load initial apps (fast batch loading)
    LaunchedEffect(isInitialized) {
        if (!isInitialized) return@LaunchedEffect
        
        // Load 50 apps initially (2 batches of 25) - fills screen + buffer for smooth scrolling
        repeat(2) {
            if (viewModel.hasMoreApps() && searchQuery.isBlank()) {
                viewModel.loadNextBatch()
                // Wait for batch to complete before next
                while (isLoadingApps) {
                    kotlinx.coroutines.delay(10) // Fast polling
                }
            }
        }
    }
    
    // Detect when user scrolls near bottom and load more (aggressive prefetching)
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@snapshotFlow false
            
            val lastVisibleIndex = visibleItems.last().index
            val totalItems = layoutInfo.totalItemsCount
            
            // Trigger loading when we're 10 items away from the end (aggressive prefetch)
            lastVisibleIndex >= totalItems - 10
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && !isLoadingApps && viewModel.hasMoreApps() && searchQuery.isBlank()) {
                viewModel.loadNextBatch()
            }
        }
    }
    
    // Calculate scroll-based alpha for graph (fades out as you scroll)
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
    
    val graphHeight by animateFloatAsState(
        targetValue = when {
            listState.firstVisibleItemIndex > 0 -> 0f
            scrollOffset.value > 300 -> 0f
            scrollOffset.value > 0 -> 1f - (scrollOffset.value / 300f)
            else -> 1f
        },
        animationSpec = tween(durationMillis = 200),
        label = "graphHeight"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar with Full-Width Search
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Full-Width Search Bar with rounded rectangle corners
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        placeholder = { Text("Search apps...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp), // Rounded rectangle instead of fully rounded
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            // Scrollable Content
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Graph Area (with smooth fade)
                item {
                    if (graphHeight > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((200 * graphHeight).dp)
                                .alpha(graphAlpha)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Usage Graph",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Coming soon...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
                
                // Monitored Apps Section (at the top)
                if (monitoredApps.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .animateContentSize()
                        ) {
                            Text(
                                text = "📌 Monitored Apps (${monitoredApps.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                            )
                        }
                    }
                    
                    // Show monitored apps
                    val monitoredAppInfos = filteredApps.filter { monitoredApps.contains(it.packageName) }
                    items(
                        items = monitoredAppInfos,
                        key = { "monitored_${it.packageName}" }
                    ) { appInfo ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300)) +
                                    scaleIn(animationSpec = tween(300), initialScale = 0.8f) +
                                    slideInVertically(animationSpec = tween(300), initialOffsetY = { -20 })
                        ) {
                            AppListItem(
                                appInfo = appInfo,
                                isChecked = true,
                                onCheckedChange = {
                                    viewModel.onAppChecked(appInfo.packageName, false)
                                }
                            )
                        }
                    }
                    
                    // Divider
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }
                
                // Section Header for All Apps
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (monitoredApps.isEmpty()) "Select Apps to Monitor" else "All Apps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (isMonitoring) {
                                    "Monitoring ${monitoredApps.size} apps"
                                } else if (monitoredApps.isEmpty()) {
                                    "No apps selected yet"
                                } else {
                                    "${monitoredApps.size} apps selected"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isMonitoring) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                }
                            )
                        }
                    }
                }
                
                // App List
                if (filteredApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    text = "No apps found",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            } else if (!isInitialized) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Initializing...",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            } else if (isLoadingApps) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Loading apps...",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = filteredApps,
                        key = { it.packageName }
                    ) { appInfo ->
                        // Fade in animation for each app item
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300)) + 
                                    slideInHorizontally(
                                        animationSpec = tween(300),
                                        initialOffsetX = { -100 } // Slide in from left
                                    )
                        ) {
                            AppListItem(
                                appInfo = appInfo,
                                isChecked = monitoredApps.contains(appInfo.packageName),
                                onCheckedChange = {
                                    viewModel.onAppChecked(appInfo.packageName, !monitoredApps.contains(appInfo.packageName))
                                }
                            )
                        }
                    }
                    
                    // Loading indicator at bottom when loading more apps
                    if (isLoadingApps && searchQuery.isBlank()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Loading apps...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Monitor Button at Bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shadowElevation = 16.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ) {
            Button(
                onClick = {
                    if (isMonitoring) {
                        viewModel.stopMonitoring()
                    } else {
                        if (monitoredApps.isEmpty()) {
                            android.widget.Toast.makeText(
                                context,
                                "Please select at least one app to monitor",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else if (checkPermissions(context)) {
                            viewModel.startMonitoring()
                            // Scroll to top to show the graph
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        } else {
                            showPermissionDialog = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isMonitoring) "Stop Monitoring" else "Start Monitoring",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showPermissionDialog) {
            PermissionDialog(
                onDismiss = { showPermissionDialog = false },
                onOpenSettings = {
                    requestPermissions(context)
                    showPermissionDialog = false
                }
            )
        }
    }
}

@Composable
fun AppListItem(appInfo: AppInfo, isChecked: Boolean, onCheckedChange: () -> Unit) {
    // Memoize the drawable painter to avoid recreating on recomposition
    val iconPainter = rememberDrawablePainter(drawable = appInfo.icon)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = if (isChecked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isChecked) 2.dp else 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = appInfo.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onCheckedChange() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
fun PermissionDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions Required") },
        text = {
            Text(
                "Intentionality needs two permissions:\n\n" +
                        "1. Usage Access - to detect when monitored apps are opened\n" +
                        "2. Display over other apps - to show the intention prompt\n\n" +
                        "This allows the app to show you the intention prompt when you launch a monitored app.\n\n" +
                        "Your privacy is important - we only monitor apps you select and all data stays on your device and Supabase."
            )
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun checkPermissions(context: Context): Boolean {
    return hasUsageStatsPermission(context) && hasOverlayPermission(context)
}

private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun hasOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true // Permission not required on older versions
    }
}

private fun requestPermissions(context: Context) {
    // First check which permission is missing and request it
    if (!hasUsageStatsPermission(context)) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    } else if (!hasOverlayPermission(context)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    }
}
