package com.nibodhdaware.intentionality.ui.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.nibodhdaware.intentionality.ui.appconfig.AppConfigScreen
import com.nibodhdaware.intentionality.ui.applist.AppListScreen
import com.nibodhdaware.intentionality.ui.billing.PaywallScreen
import com.nibodhdaware.intentionality.ui.home.HomeScreen
import com.nibodhdaware.intentionality.ui.profile.ProfileScreen
import com.nibodhdaware.intentionality.ui.settings.SettingsScreen // Import SettingsScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object MyUsage : BottomNavItem(
        route = "home",
        title = "My Usage",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    object UsageLimits : BottomNavItem(
        route = "usage_limits",
        title = "Usage Limits",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
    
    object InAppBlocking : BottomNavItem(
        route = "in_app_blocking",
        title = "In-App Blocking",
        selectedIcon = Icons.Filled.Lock,
        unselectedIcon = Icons.Outlined.Lock
    )
}

private const val ANIMATION_DURATION = 300

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    showFeatureDiscovery: Boolean = false,
    onFeatureDiscoveryComplete: () -> Unit = {},
    requestNotificationPermission: Boolean = false,
    onNotificationPermissionRequested: () -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = BottomNavItem.MyUsage.route,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(ANIMATION_DURATION)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(ANIMATION_DURATION)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(ANIMATION_DURATION)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(ANIMATION_DURATION)
            )
        }
    ) {
        composable(BottomNavItem.MyUsage.route) {
            HomeScreen(
                onNavigateToAddApps = {
                    navController.navigate("add_apps")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToAppConfig = { packageName ->
                    navController.navigate("app_config/$packageName")
                },
                onNavigateToPaywall = {
                    navController.navigate("paywall")
                },
                onNavigateToSettings = {
                    navController.navigate("settings_route") // Navigate to settings
                },
                showFeatureDiscovery = showFeatureDiscovery,
                onFeatureDiscoveryComplete = onFeatureDiscoveryComplete,
                requestNotificationPermission = requestNotificationPermission,
                onNotificationPermissionRequested = onNotificationPermissionRequested
            )
        }

        composable("paywall") {
            PaywallScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPurchaseSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(BottomNavItem.UsageLimits.route) {
            // Placeholder for Usage Limits screen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Usage Limits - Coming Soon")
            }
        }

        composable(BottomNavItem.InAppBlocking.route) {
            // Placeholder for In-App Blocking screen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("In-App Blocking - Coming Soon")
            }
        }

        composable("profile") {
            ProfileScreen(
                onLogout = onLogout,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPaywall = {
                    navController.navigate("paywall")
                }
            )
        }

        composable("add_apps") {
            AppListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPaywall = {
                    navController.navigate("paywall")
                }
            )
        }
        
        composable(
            route = "app_config/{packageName}",
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: return@composable
            AppConfigScreen(
                packageName = packageName,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPaywall = {
                    navController.navigate("paywall")
                }
            )
        }

        // New composable for SettingsScreen
        composable("settings_route") {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
