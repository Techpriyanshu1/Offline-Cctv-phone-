package com.example.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object CameraMode : Screen("camera_mode")
    object MonitorMode : Screen("monitor_mode")
    object DeviceDiscovery : Screen("device_discovery")
    object Pairing : Screen("pairing")
    object LiveMonitoring : Screen("live_monitoring")
    object Recordings : Screen("recordings")
    object MotionHistory : Screen("motion_history")
    object DeviceManagement : Screen("device_management")
    object AppSettings : Screen("app_settings")
    object PrivacyPermissions : Screen("privacy_permissions")
}
