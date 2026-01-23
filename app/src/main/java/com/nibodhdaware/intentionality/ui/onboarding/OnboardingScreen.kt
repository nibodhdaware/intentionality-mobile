package com.nibodhdaware.intentionality.ui.onboarding

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val isPermission: Boolean = false,
    val permissionType: PermissionType? = null,
    val isInteractive: Boolean = false // New parameter
)

enum class PermissionType {
    USAGE_ACCESS,
    OVERLAY,
    NOTIFICATIONS,
    BATTERY_OPTIMIZATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current // Get context once here
    val scope = rememberCoroutineScope()
    
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Star,
            title = "Welcome to Intentionality",
            description = "Be more mindful about your app usage. We'll need a few permissions to help you build better digital habits."
        ),
        // New page to explain the pop-up
        OnboardingPage(
            icon = Icons.Default.ChatBubble,
            title = "How it Works: The Intention Prompt",
            description = "When you open a monitored app, a small pop-up will appear. This prompt asks you to briefly state your reason for opening the app, helping you be more intentional."
        ),
        // New interactive demo page
        OnboardingPage(
            icon = Icons.Default.Lightbulb,
            title = "Try it Out!",
            description = "Imagine you're opening 'Make'. Type your intention below to see how it works.",
            isInteractive = true // Mark as interactive
        ),
        // Permission pages
        OnboardingPage(
            icon = Icons.Default.Info,
            title = "Usage Access",
            description = "Required to detect when you open monitored apps.",
            isPermission = true,
            permissionType = PermissionType.USAGE_ACCESS
        ),
        OnboardingPage(
            icon = Icons.Default.Build,
            title = "Overlay Permission",
            description = "Required to show the intention prompt over apps.",
            isPermission = true,
            permissionType = PermissionType.OVERLAY
        ),
        OnboardingPage(
            icon = Icons.Default.Notifications,
            title = "Notification Permission",
            description = "Required to show a persistent status notification while monitoring. This helps keep the app running in the background.",
            isPermission = true,
            permissionType = PermissionType.NOTIFICATIONS
        ),
        OnboardingPage(
            icon = Icons.Default.Settings,
            title = "Battery Optimization",
            description = "Disable battery optimization to prevent Android from killing the monitoring service. Set to 'Unrestricted' or 'No restrictions' for best reliability.",
            isPermission = true,
            permissionType = PermissionType.BATTERY_OPTIMIZATION
        ),
        OnboardingPage(
            icon = Icons.Default.CheckCircle,
            title = "You're Ready!",
            description = "We'll give you a quick tour of the app next."
        )
    )
    
    val pagerState = rememberPagerState(pageCount = { pages.size })
    
    // Track permission states with periodic refresh
    var permissionRefreshTrigger by remember { mutableIntStateOf(0) }
    
    // Periodically check permissions when on a permission page
    LaunchedEffect(pagerState.currentPage) {
        while (true) {
            delay(500) // Check every 500ms
            permissionRefreshTrigger++
        }
    }
    
    // State to track interaction on interactive pages
    var interactivePageInteractionDone by remember { mutableStateOf(false) }
    
    // Calculate if current page permission is granted
    val currentPage = pages.getOrNull(pagerState.currentPage)
    val isCurrentPermissionGranted = remember(permissionRefreshTrigger, pagerState.currentPage) {
        when (currentPage?.permissionType) {
            PermissionType.USAGE_ACCESS -> hasUsageStatsPermission(context)
            PermissionType.OVERLAY -> Settings.canDrawOverlays(context)
            PermissionType.NOTIFICATIONS -> hasNotificationPermission(context)
            PermissionType.BATTERY_OPTIMIZATION -> isIgnoringBatteryOptimizations(context)
            null -> true // Non-permission pages are always "granted"
        }
    }
    
    // Button should be enabled if: not a permission page, or permission is granted, or interactive page and interaction is done
    val isNextButtonEnabled = remember(currentPage, isCurrentPermissionGranted, interactivePageInteractionDone) {
        if (currentPage == null) return@remember false
        
        when {
            currentPage.isPermission -> isCurrentPermissionGranted
            currentPage.isInteractive -> interactivePageInteractionDone
            else -> true // Regular content page
        }
    }
    
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Skip button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (pagerState.currentPage < pages.size - 1) {
                        TextButton(onClick = onOnboardingComplete) {
                            Text("Skip")
                        }
                    }
                }
                
                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { pageIndex -> // Changed 'page' to 'pageIndex' to avoid confusion with OnboardingPage object
                    val currentPageObject = pages[pageIndex]
                    
                    if (currentPageObject.isInteractive) {
                        OnboardingPopupDemoContent(
                            page = currentPageObject,
                            onInteraction = { interactivePageInteractionDone = it }
                        )
                    } else {
                        OnboardingPageContent(
                            page = currentPageObject,
                            context = context, // Pass context here
                            onInteraction = { /* Not used for non-interactive pages */ }
                        )
                    }
                }
                
                // Bottom section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page indicators
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        repeat(pages.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (isSelected) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                    
                    // Next/Get Started button
                    Button(
                        onClick = {
                            if (pagerState.currentPage < pages.size - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                                // Reset interaction state when moving to a new page
                                interactivePageInteractionDone = false
                            } else {
                                onOnboardingComplete()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = isNextButtonEnabled,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) {
                        Text(
                            text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (pagerState.currentPage < pages.size - 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

// New OnboardingPopupDemoContent composable
@Composable
private fun OnboardingPopupDemoContent(
    page: OnboardingPage,
    onInteraction: (Boolean) -> Unit // Callback for interaction status
) {
    var inputText by remember { mutableStateOf("") }
    
    LaunchedEffect(inputText) {
        onInteraction(inputText.isNotBlank())
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon container
        val infiniteTransition = rememberInfiniteTransition(label = "icon")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(70.dp * scale), // Apply scale here
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Mock pop-up UI
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Why are you opening Make?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Your intention (e.g., 'Check tasks')") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Add a mock 'Submit' button for visual completeness
                Button(
                    onClick = { /* Do nothing for demo */ },
                    enabled = inputText.isNotBlank(), // Enable only if text is entered
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Submit Intention")
                }
            }
        }
    }
}


@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    context: Context, // Re-added context parameter
    onInteraction: (Boolean) -> Unit = { }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon container
        val infiniteTransition = rememberInfiniteTransition(label = "icon")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(70.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
        )
        
        // Permission button if needed
        if (page.isPermission && page.permissionType != null) {
            Spacer(modifier = Modifier.height(32.dp))
            
            val hasPermission = when (page.permissionType) {
                PermissionType.USAGE_ACCESS -> hasUsageStatsPermission(context)
                PermissionType.OVERLAY -> Settings.canDrawOverlays(context)
                PermissionType.NOTIFICATIONS -> hasNotificationPermission(context)
                PermissionType.BATTERY_OPTIMIZATION -> isIgnoringBatteryOptimizations(context)
            }
            
            OutlinedButton(
                onClick = {
                    when (page.permissionType) {
                        PermissionType.USAGE_ACCESS -> {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }
                        PermissionType.OVERLAY -> {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                        PermissionType.NOTIFICATIONS -> {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            )
                        }
                        PermissionType.BATTERY_OPTIMIZATION -> {
                            // Open battery optimization settings for this app
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = if (hasPermission) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                if (hasPermission) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Permission Granted",
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Permission")
                }
            }
        }
    }
}

private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // Permission not required before Android 13
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
