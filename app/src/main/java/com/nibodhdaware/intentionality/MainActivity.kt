package com.nibodhdaware.intentionality

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nibodhdaware.intentionality.ui.main.MainScreen
import com.nibodhdaware.intentionality.ui.onboarding.OnboardingPreferences
import com.nibodhdaware.intentionality.ui.onboarding.OnboardingScreen
import com.nibodhdaware.intentionality.ui.billing.PaywallScreen
import com.nibodhdaware.intentionality.ui.theme.IntentionalityTheme
import kotlinx.coroutines.launch

enum class AppScreen {
    ONBOARDING,
    MAIN,
    PAYWALL // New screen for pricing
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntentionalityTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onboardingPreferences = remember { OnboardingPreferences(context) }

    val hasCompletedOnboarding by onboardingPreferences.hasCompletedOnboarding.collectAsState(initial = null)
    
    var currentScreen by remember { mutableStateOf<AppScreen?>(null) }
    var showFeatureDiscovery by remember { mutableStateOf(false) }

    LaunchedEffect(hasCompletedOnboarding) {
        if (hasCompletedOnboarding != null) {
            currentScreen = if (hasCompletedOnboarding == true) {
                AppScreen.MAIN
            } else {
                AppScreen.ONBOARDING
            }
        }
    }

    val hasCompletedFeatureDiscovery by onboardingPreferences.hasCompletedFeatureDiscovery.collectAsState(initial = true)
    val hasAskedNotificationPermission by onboardingPreferences.hasAskedNotificationPermission.collectAsState(initial = true)

    // Track if we should request notification permission
    var requestNotificationPermission by remember { mutableStateOf(false) }

    if (currentScreen == null) {
        // Show a loading state or a blank screen while determining the route
        return
    }

    when (currentScreen) {
        AppScreen.ONBOARDING -> {
            OnboardingScreen(
                onOnboardingComplete = {
                    scope.launch {
                        onboardingPreferences.setOnboardingCompleted()
                    }
                    // Navigate to PaywallScreen after onboarding
                    currentScreen = AppScreen.PAYWALL
                }
            )
        }
        
        AppScreen.MAIN -> {
            // Show feature discovery if coming from onboarding and not yet completed
            val shouldShowDiscovery = showFeatureDiscovery || (hasCompletedFeatureDiscovery == false)
            
            // Request notification permission after feature discovery is done (or if skipped)
            val shouldRequestNotification = !shouldShowDiscovery && hasAskedNotificationPermission == false
            
            MainScreen(
                onLogout = { /* No-op, no login/logout */ },
                modifier = Modifier.fillMaxSize(),
                showFeatureDiscovery = shouldShowDiscovery,
                onFeatureDiscoveryComplete = {
                    showFeatureDiscovery = false
                    scope.launch {
                        onboardingPreferences.setFeatureDiscoveryCompleted()
                    }
                    // Trigger notification permission request after feature discovery
                    requestNotificationPermission = true
                },
                requestNotificationPermission = shouldRequestNotification || requestNotificationPermission,
                onNotificationPermissionRequested = {
                    requestNotificationPermission = false
                }
            )
        }
        
        AppScreen.PAYWALL -> {
            PaywallScreen(
                onNavigateBack = {
                    // Navigate to MainScreen after paywall is dismissed
                    currentScreen = AppScreen.MAIN
                }
            )
        }
        null -> {
            // Handled by the check above
        }
    }
}