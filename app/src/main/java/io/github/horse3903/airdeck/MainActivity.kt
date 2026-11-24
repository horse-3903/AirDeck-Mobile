package io.github.horse3903.airdeck

import io.github.horse3903.airdeck.ui.composables.LocalGamepadStyle
import io.github.horse3903.airdeck.ui.composables.LocalStickSnap
import io.github.horse3903.airdeck.ui.composables.LocalDpadSingle
import io.github.horse3903.airdeck.data.defaultDpadSingle
import io.github.horse3903.airdeck.data.defaultStickSnap
import io.github.horse3903.airdeck.data.defaultGamepadStyle
import androidx.compose.runtime.CompositionLocalProvider
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.horse3903.airdeck.data.SettingsRepository
import io.github.horse3903.airdeck.data.defaultBaseColor
import io.github.horse3903.airdeck.data.defaultColorScheme
import io.github.horse3903.airdeck.data.defaultFullScreenEnabled
import io.github.horse3903.airdeck.data.defaultHapticFeedbackEnabled
import io.github.horse3903.airdeck.data.defaultSaveConnectionCredentials
import io.github.horse3903.airdeck.network.ConnectionViewModel
import io.github.horse3903.airdeck.network.ConnectionHub
import io.github.horse3903.airdeck.network.ConnectionService
import io.github.horse3903.airdeck.ui.screens.AboutScreen
import io.github.horse3903.airdeck.ui.screens.ConnectMenu
import io.github.horse3903.airdeck.ui.screens.ConnectingScreen
import io.github.horse3903.airdeck.ui.screens.GamePad
import io.github.horse3903.airdeck.ui.screens.GamepadCustomizationScreen
import io.github.horse3903.airdeck.ui.screens.MainMenu
import io.github.horse3903.airdeck.ui.screens.SettingsScreen
import io.github.horse3903.airdeck.ui.theme.AirDeckTheme
import io.github.horse3903.airdeck.ui.utils.HapticPrefs
import io.github.horse3903.airdeck.ui.utils.HapticUtils


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = SettingsRepository(this)

        // Create a ViewModel the first time the system calls an activity's onCreate() method.
        // Re-created activities receive the same ConnectionViewModel instance created by the first activity.
        // Use the 'by viewModels()' Kotlin property delegate with factory
        // from the activity-ktx artifact
        val connectionViewModel: ConnectionViewModel = ConnectionHub.get(applicationContext)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            AppUI(
                connectionViewModel = connectionViewModel,
                settingsRepository = settingsRepository,
            )
        }
    }

    @Composable
    private fun AppUI(
        connectionViewModel: ConnectionViewModel,
        settingsRepository: SettingsRepository,
    ) {
        val hapticEnabled = settingsRepository.hapticFeedbackEnabled.collectAsState(
            initial = defaultHapticFeedbackEnabled
        )

        LaunchedEffect(hapticEnabled.value) {
            HapticUtils.isEnabled = hapticEnabled.value
        }

        val hapticPrefs = settingsRepository.hapticPrefs.collectAsState(initial = HapticPrefs())
        LaunchedEffect(hapticPrefs.value) {
            HapticUtils.prefs = hapticPrefs.value
        }

        val fullScreenEnabled = settingsRepository.fullScreenEnabled.collectAsState(
            initial = defaultFullScreenEnabled
        )

        LaunchedEffect(fullScreenEnabled.value) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (fullScreenEnabled.value) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        AirDeckTheme(
            darkMode = settingsRepository.colorScheme.collectAsState(
                initial = defaultColorScheme
            ).value,
            baseColor = settingsRepository.baseColor.collectAsState(
                initial = defaultBaseColor
            ).value
        ) {
            val gamepadStyle by settingsRepository.gamepadStyle.collectAsState(
                initial = defaultGamepadStyle
            )
            val stickSnap by settingsRepository.stickSnap.collectAsState(initial = defaultStickSnap)
            val dpadSingle by settingsRepository.dpadSingle.collectAsState(initial = defaultDpadSingle)
            val appPrefs by settingsRepository.appPrefs.collectAsState(initial = io.github.horse3903.airdeck.data.AppPrefs())
            CompositionLocalProvider(
                LocalGamepadStyle provides gamepadStyle,
                LocalStickSnap provides stickSnap,
                LocalDpadSingle provides dpadSingle,
                io.github.horse3903.airdeck.ui.composables.LocalAppPrefs provides appPrefs
            ) {
                NavTree(
                    connectionViewModel = connectionViewModel,
                    settingsRepository = settingsRepository,
                )
            }
        }
    }

    @Composable
    private fun NavTree(
        connectionViewModel: ConnectionViewModel,
        settingsRepository: SettingsRepository,
        navController: NavHostController = rememberNavController(),
    ) {
        NavHost(navController = navController, startDestination = "main_menu") {
            composable("main_menu") {
                val connection by connectionViewModel.uiState.collectAsState()
                MainMenu(
                    connection = connection,
                    onReturnToGamepad = { navController.navigate("gamepad") },
                    onNavigateToConnectScreen = { navController.navigate("connect_screen") },
                    onNavigateToSettingsScreen = { navController.navigate("settings_screen") },
                    onNavigateToAboutScreen = { navController.navigate("about_screen") }
                )
            }
            composable("connect_screen") {
                val lastIpAddress by settingsRepository.lastConnectionIpAddress.collectAsState(
                    initial = ""
                )
                val lastPort by settingsRepository.lastConnectionPort.collectAsState(initial = "")
                val saveCredentials by settingsRepository.saveConnectionCredentials.collectAsState(
                    initial = defaultSaveConnectionCredentials
                )

                val initialIp = if (saveCredentials) lastIpAddress else ""
                val initialPort = if (saveCredentials) lastPort else ""

                ConnectMenu(
                    onNavigateToConnectingScreen = { ipAddress, port ->
                        navController.navigate("connecting_screen/$ipAddress/$port")
                    },
                    initialIp = initialIp,
                    initialPort = initialPort
                )
            }
            composable(
                "connecting_screen/{ipAddress}/{port}",
                arguments = listOf(
                    navArgument("ipAddress") { type = NavType.StringType },
                    navArgument("port") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val ipAddress = backStackEntry.arguments?.getString("ipAddress") ?: ""
                val port = backStackEntry.arguments?.getString("port") ?: ""
                val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
                ConnectingScreen(
                    onNavigateToGamepad = {
                        ConnectionService.start(appContext)
                        navController.navigate("gamepad") {
                            popUpTo("connect_screen") { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() },
                    connectionViewModel = connectionViewModel,
                    ipAddress = ipAddress,
                    port = port
                )
            }
            composable("gamepad") {
                GamePad(
                    connectionViewModel = connectionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("settings_screen") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToGamepadCustomization = { navController.navigate("gamepad_customization") },
                    settingsRepository = settingsRepository
                )
            }
            composable("gamepad_customization") {
                GamepadCustomizationScreen(
                    onNavigateBack = { navController.popBackStack() },
                    settingsRepository = settingsRepository
                )
            }
            composable("about_screen") {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
