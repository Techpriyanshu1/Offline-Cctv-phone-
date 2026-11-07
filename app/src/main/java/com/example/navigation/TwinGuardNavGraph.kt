package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.components.RequestCctvPermissions
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.camera.CameraModeScreen
import com.example.ui.screens.devices.DeviceManagementScreen
import com.example.ui.screens.monitor.LiveMonitoringScreen
import com.example.ui.screens.monitor.MonitorModeScreen
import com.example.ui.screens.motion.MotionHistoryScreen
import com.example.ui.screens.recordings.RecordingsScreen
import com.example.ui.screens.settings.AppSettingsScreen
import com.example.ui.screens.settings.PrivacyAndPermissionsScreen

@Composable
fun TwinGuardNavGraph(
    navController: NavHostController
) {
    var monitorTargetIp by remember { mutableStateOf("192.168.49.1") }
    var monitorTargetPort by remember { mutableStateOf(8888) }
    var monitorTargetPin by remember { mutableStateOf("") }
    var monitorTargetName by remember { mutableStateOf("TwinGuard Camera") }

    RequestCctvPermissions {
        // Permissions requested on launch
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateNext = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            HomeDashboardScreen(
                onNavigateCameraMode = { navController.navigate(Screen.CameraMode.route) },
                onNavigateMonitorMode = { navController.navigate(Screen.MonitorMode.route) },
                onNavigateRecordings = { navController.navigate(Screen.Recordings.route) },
                onNavigateMotionHistory = { navController.navigate(Screen.MotionHistory.route) },
                onNavigateDeviceManagement = { navController.navigate(Screen.DeviceManagement.route) },
                onNavigateSettings = { navController.navigate(Screen.AppSettings.route) },
                onNavigatePrivacy = { navController.navigate(Screen.PrivacyPermissions.route) },
                onConnectQr = { ip, port, pin, name ->
                    monitorTargetIp = ip
                    monitorTargetPort = port
                    monitorTargetPin = pin
                    monitorTargetName = name
                    navController.navigate(Screen.LiveMonitoring.route)
                }
            )
        }

        composable(Screen.CameraMode.route) {
            CameraModeScreen(
                onExitCamera = { navController.popBackStack() }
            )
        }

        composable(Screen.MonitorMode.route) {
            MonitorModeScreen(
                onConnectToCamera = { ip, port, pin, name ->
                    monitorTargetIp = ip
                    monitorTargetPort = port
                    monitorTargetPin = pin
                    monitorTargetName = name
                    navController.navigate(Screen.LiveMonitoring.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LiveMonitoring.route) {
            LiveMonitoringScreen(
                cameraIp = monitorTargetIp,
                cameraPort = monitorTargetPort,
                cameraPin = monitorTargetPin,
                cameraName = monitorTargetName,
                onDisconnect = { navController.popBackStack() }
            )
        }

        composable(Screen.Recordings.route) {
            RecordingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MotionHistory.route) {
            MotionHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DeviceManagement.route) {
            DeviceManagementScreen(
                onConnectToCamera = { ip, port, pin, name ->
                    monitorTargetIp = ip
                    monitorTargetPort = port
                    monitorTargetPin = pin
                    monitorTargetName = name
                    navController.navigate(Screen.LiveMonitoring.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppSettings.route) {
            AppSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PrivacyPermissions.route) {
            PrivacyAndPermissionsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
