package com.nibodhdaware.intentionality.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nibodhdaware.intentionality.ui.applist.AppListScreen
import com.nibodhdaware.intentionality.ui.auth.LoginScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object AppList : Screen("app_list")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.AppList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AppList.route) {
            AppListScreen()
        }
    }
}

