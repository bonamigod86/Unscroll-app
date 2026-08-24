package com.unscroll.app.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unscroll.app.dashboard.AppPickerScreen
import com.unscroll.app.dashboard.GoalSetupScreen
import com.unscroll.app.dashboard.HomeScreen
import com.unscroll.app.dashboard.PermissionsScreen
import com.unscroll.app.dashboard.SettingsScreen
import com.unscroll.app.data.PrefsRepository

object Routes {
    const val GOAL_SETUP = "goal_setup"
    const val HOME = "home"
    const val PICKER = "picker"
    const val PERMISSIONS = "permissions"
    const val SETTINGS = "settings"
}

@Composable
fun UnscrollNavGraph() {
    val context = LocalContext.current
    val prefs = PrefsRepository.get(context)
    val navController = rememberNavController()

    BackHandler(enabled = navController.previousBackStackEntry == null) {
        (context as? Activity)?.moveTaskToBack(true)
    }

    NavHost(
        navController = navController,
        startDestination = if (prefs.goalConfigured.value) Routes.HOME else Routes.GOAL_SETUP
    ) {
        composable(Routes.GOAL_SETUP) {
            val wasConfigured = remember { prefs.goalConfigured.value }
            GoalSetupScreen(onFinished = {
                if (wasConfigured) {
                    navController.popBackStack()
                } else {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.GOAL_SETUP) { inclusive = true }
                    }
                }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenPicker = { navController.navigate(Routes.PICKER) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.PICKER) {
            AppPickerScreen(onDone = { navController.popBackStack() })
        }
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(onDone = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPicker = { navController.navigate(Routes.PICKER) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onOpenGoalSetup = { navController.navigate(Routes.GOAL_SETUP) }
            )
        }
    }
}
