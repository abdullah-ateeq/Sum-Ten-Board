package com.example.sumtenboard.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sumtenboard.game.SumTenViewModel
import com.example.sumtenboard.ui.screens.AboutScreen
import com.example.sumtenboard.ui.screens.GameScreen
import com.example.sumtenboard.ui.screens.HomeScreen
import com.example.sumtenboard.ui.screens.ResultScreen
import com.example.sumtenboard.ui.screens.SettingsScreen
import com.example.sumtenboard.ui.screens.SplashScreen
import com.example.sumtenboard.util.SoundPlayer
import com.example.sumtenboard.util.hapticFor
import com.example.sumtenboard.util.performHaptic

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val GAME = "game"
    const val RESULT = "result"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Home", Icons.Filled.Home),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
    TopLevelDestination(Routes.ABOUT, "About", Icons.Filled.Info)
)

@Composable
fun SumTenApp(viewModel: SumTenViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = topLevelDestinations.any { it.route == currentRoute }

    GameFeedbackEffects(viewModel)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = { navController.navigateTopLevel(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(if (showBottomBar) padding else PaddingValues())
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(viewModel, onPlay = {
                    viewModel.startNewGame()
                    navController.navigate(Routes.GAME) { launchSingleTop = true }
                })
            }
            composable(Routes.GAME) {
                GameScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack(Routes.HOME, inclusive = false) },
                    onFinished = {
                        navController.navigate(Routes.RESULT) {
                            popUpTo(Routes.GAME) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.RESULT) {
                ResultScreen(
                    viewModel = viewModel,
                    onPlayAgain = {
                        viewModel.startNewGame()
                        navController.navigate(Routes.GAME) {
                            popUpTo(Routes.RESULT) { inclusive = true }
                        }
                    },
                    onHome = { navController.popBackStack(Routes.HOME, inclusive = false) }
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen(viewModel) }
            composable(Routes.ABOUT) { AboutScreen() }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        // Splash is removed from the stack, so Home is the root of the bottom-bar graph.
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Plays sound and haptics for game events; lives above the NavHost so it survives screen changes. */
@Composable
private fun GameFeedbackEffects(viewModel: SumTenViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentSettings by rememberUpdatedState(settings)
    val view = LocalView.current
    val sound = remember { SoundPlayer() }
    DisposableEffect(Unit) { onDispose { sound.release() } }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (currentSettings.sound) sound.play(event)
            if (currentSettings.haptics) hapticFor(event)?.let { view.performHaptic(it) }
        }
    }
}
