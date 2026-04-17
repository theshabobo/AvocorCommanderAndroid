package com.avocor.commander

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avocor.commander.ui.screens.*
import com.avocor.commander.ui.theme.AvocorCommanderTheme
import com.avocor.commander.viewmodel.AppMode
import com.avocor.commander.viewmodel.AuthViewModel
import com.avocor.commander.viewmodel.DeviceViewModel
import com.avocor.commander.viewmodel.SettingsViewModel

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val ROOM = "room/{groupId}"
    const val CONTROL = "control/{deviceId}"
    const val SETTINGS = "settings"
    const val TABLET_ROOM = "tablet_room"
    const val TABLET_SETTINGS = "tablet_settings"

    fun control(deviceId: Int) = "control/$deviceId"
    fun room(groupId: Int) = "room/$groupId"
}

@Composable
fun AvocorApp() {
    val authViewModel: AuthViewModel = viewModel()
    val deviceViewModel: DeviceViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val serverUrl by authViewModel.serverUrl.collectAsState()
    val token by authViewModel.token.collectAsState()
    val username by authViewModel.username.collectAsState()
    val appMode by settingsViewModel.appMode.collectAsState()
    val isReady by settingsViewModel.isReady.collectAsState()
    val assignedRoomId by settingsViewModel.assignedRoomId.collectAsState()

    // Initialize device VM when logged in
    LaunchedEffect(isLoggedIn, serverUrl, token) {
        if (isLoggedIn && token != null && serverUrl.isNotBlank()) {
            deviceViewModel.initialize(serverUrl, token!!)
        }
    }

    AvocorCommanderTheme {
        // Wait for settings DataStore to load before rendering
        if (!isReady) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@AvocorCommanderTheme
        }

        if (!isLoggedIn) {
            // Login screen — same for both modes
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { /* isLoggedIn flips, recompose handles it */ }
            )
            return@AvocorCommanderTheme
        }

        // Logged in — branch by mode
        when (appMode) {
            AppMode.PHONE -> PhoneNavigation(
                navController = navController,
                authViewModel = authViewModel,
                deviceViewModel = deviceViewModel,
                settingsViewModel = settingsViewModel,
                username = username
            )

            AppMode.TABLET -> TabletNavigation(
                navController = navController,
                authViewModel = authViewModel,
                deviceViewModel = deviceViewModel,
                settingsViewModel = settingsViewModel,
                assignedRoomId = assignedRoomId
            )
        }
    }
}

@Composable
private fun PhoneNavigation(
    navController: androidx.navigation.NavHostController,
    authViewModel: AuthViewModel,
    deviceViewModel: DeviceViewModel,
    settingsViewModel: SettingsViewModel,
    username: String
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                deviceViewModel = deviceViewModel,
                username = username,
                onDeviceSelected = { device ->
                    navController.navigate(Routes.control(device.id))
                },
                onLogout = {
                    deviceViewModel.cleanup()
                    authViewModel.logout()
                },
                onRoomSelected = { groupId ->
                    navController.navigate(Routes.room(groupId))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(
            route = Routes.ROOM,
            arguments = listOf(navArgument("groupId") { type = NavType.IntType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: -1
            RoomDetailScreen(
                groupId = groupId,
                deviceViewModel = deviceViewModel,
                onDeviceSelected = { device ->
                    navController.navigate(Routes.control(device.id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CONTROL,
            arguments = listOf(navArgument("deviceId") { type = NavType.IntType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getInt("deviceId") ?: -1
            val devices by deviceViewModel.devices.collectAsState()
            val initialDevice = devices.find { it.id == deviceId }

            ControlScreen(
                deviceViewModel = deviceViewModel,
                initialDevice = initialDevice,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                deviceViewModel = deviceViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun TabletNavigation(
    navController: androidx.navigation.NavHostController,
    authViewModel: AuthViewModel,
    deviceViewModel: DeviceViewModel,
    settingsViewModel: SettingsViewModel,
    assignedRoomId: Int?
) {
    // If no room assigned, go straight to settings for setup
    val startDest = if (assignedRoomId != null) Routes.TABLET_ROOM else Routes.TABLET_SETTINGS

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(Routes.TABLET_ROOM) {
            TabletRoomScreen(
                settingsViewModel = settingsViewModel,
                deviceViewModel = deviceViewModel,
                onSettingsRequested = {
                    navController.navigate(Routes.TABLET_SETTINGS)
                }
            )
        }

        composable(Routes.TABLET_SETTINGS) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                deviceViewModel = deviceViewModel,
                onBack = {
                    if (assignedRoomId != null) {
                        navController.popBackStack()
                    }
                    // If no room assigned, don't allow back — stay on settings
                }
            )
        }
    }
}
