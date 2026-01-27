package com.nibodhdaware.intentionality.ui.applist

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToPaywall: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = viewModel()
) {
    val context = LocalContext.current
    val filteredApps by viewModel.filteredApps.collectAsState()
    val monitoredApps by viewModel.monitoredApps.collectAsState()
    val selectedApps by viewModel.selectedApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val hasMoreApps by viewModel.hasMoreApps.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    
    // List state for pagination
    val listState = rememberLazyListState()
    
    // Trigger load more when near end of list
    LaunchedEffect(listState.firstVisibleItemIndex, filteredApps.size) {
        val lastVisibleIndex = listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size
        if (lastVisibleIndex >= filteredApps.size - 5 && hasMoreApps) {
            viewModel.loadNextPage()
        }
    }
    
    // Clear selection when entering this screen (we're adding NEW apps)
    LaunchedEffect(Unit) {
        viewModel.clearSelection()
    }
    
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    
    // Request focus when screen loads
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // Always show search TopAppBar
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("Search apps...") },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        onNavigateBack?.invoke()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = {
                        if (selectedApps.isEmpty()) {
                            android.widget.Toast.makeText(
                                context,
                                "Please select at least one app to monitor",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            coroutineScope.launch {
                                viewModel.addSelectedAppsToMonitored()
                                android.widget.Toast.makeText(
                                    context,
                                    "${selectedApps.size} app${if (selectedApps.size == 1) "" else "s"} added to monitoring",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                onNavigateBack?.invoke()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(48.dp),
                    enabled = selectedApps.isNotEmpty()
                ) {
                    Text(
                        text = if (selectedApps.isEmpty()) {
                            "Select Apps to Monitor"
                        } else {
                            "Add ${selectedApps.size} App${if (selectedApps.size == 1) "" else "s"}"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { paddingValues ->
        if (searchQuery.isBlank()) {
            // Show helpful message when no search query
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Search for an app",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Type the name of the app you want to monitor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Show filtered apps when searching
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { appInfo ->
                    AppListItem(
                        appInfo = appInfo,
                        isChecked = selectedApps.contains(appInfo.packageName),
                        onCheckedChange = {
                            val success = viewModel.toggleAppSelection(appInfo.packageName)
                            if (!success) {
                                // Free user hit their limit - show upgrade dialog
                                showUpgradeDialog = true
                            }
                        }
                    )
                }
                
                // Show "no results" message
                if (filteredApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No apps found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Usage Access Permission Required") },
            text = { 
                Text("To monitor apps, you need to grant usage access permission. This allows the app to detect when you open monitored apps.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Upgrade dialog for free users hitting app limit
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = { Text("Upgrade to Premium") },
            text = { 
                Text("Free users can monitor up to 1 app. Upgrade to Premium to monitor unlimited apps and unlock all features!") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUpgradeDialog = false
                        onNavigateToPaywall?.invoke()
                    }
                ) {
                    Text("Upgrade")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text("Maybe Later")
                }
            }
        )
    }
}

@Composable
fun AppListItem(
    appInfo: AppInfo, 
    isChecked: Boolean, 
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Cache the icon using remember with the package name as key
    val icon = remember(appInfo.packageName) {
        try {
            context.packageManager.getApplicationIcon(appInfo.packageName)
        } catch (e: Exception) {
            context.packageManager.defaultActivityIcon
        }
    }
    
    // Use stable callback to prevent recomposition
    val stableOnCheckedChange = remember(appInfo.packageName) { { onCheckedChange() } }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // App icon - directly draw without box wrapping for better performance
        Image(
            painter = rememberDrawablePainter(drawable = icon),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        
        // App name
        Text(
            text = appInfo.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
        
        // Checkbox
        Checkbox(
            checked = isChecked,
            onCheckedChange = { stableOnCheckedChange() }
        )
    }
}

