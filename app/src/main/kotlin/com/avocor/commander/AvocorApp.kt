package com.avocor.commander

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avocor.commander.ui.screens.ControlScreen
import com.avocor.commander.ui.screens.DashboardScreen
import com.avocor.commander.ui.screens.LoginScreen
import com.avocor.commander.ui.theme.AvocorCommanderTheme
import com.avocor.commander.viewmodel.AuthViewModel
import com.avocor.commander.viewmodel.DeviceViewModel

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val CONTROL = "control/{deviceId}"

    fun control(deviceId: Int) = "control/$deviceId"
}

@Composable
fun AvocorApp() {
    val authViewModel: AuthViewModel = viewModel()
    val deviceViewModel: DeviceViewModel = viewModel()

    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val serverUrl by authViewModel.serverUrl.collectAsState()
    val token by authViewModel.token.collectAsState()
    val username by authViewModel.username.collectAsState()

    // Initialize device VM when logged in
    LaunchedEffect(isLoggedIn, serverUrl, token) {
        if (isLoggedIn && token != null && serverUrl.isNotBlank()) {
            deviceViewModel.initialize(serverUrl, token!!)
        }
    }

    AvocorCommanderTheme {
        val startDestination = if (isLoggedIn) Routes.DASHBOARD else Routes.LOGIN

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

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
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.DASHBOARD) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.CONTROL,
                arguments = listOf(
                    navArgument("deviceId") { type = NavType.IntType }
                )
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
        }
    }
}
