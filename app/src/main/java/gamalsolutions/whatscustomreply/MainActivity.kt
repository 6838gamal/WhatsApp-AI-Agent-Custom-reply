package gamalsolutions.whatscustomreply

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import gamalsolutions.whatscustomreply.ui.ArStrings
import gamalsolutions.whatscustomreply.ui.EnStrings
import gamalsolutions.whatscustomreply.ui.screens.DashboardScreen
import gamalsolutions.whatscustomreply.ui.screens.GeminiSettingsScreen
import gamalsolutions.whatscustomreply.ui.screens.LogsScreen
import gamalsolutions.whatscustomreply.ui.screens.RepliesScreen
import gamalsolutions.whatscustomreply.ui.screens.SettingsScreen
import gamalsolutions.whatscustomreply.ui.screens.StatisticsScreen
import gamalsolutions.whatscustomreply.ui.theme.MyApplicationTheme
import gamalsolutions.whatscustomreply.ui.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request crucial permissions on launch for optimal capabilities
        val requestPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { }
        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.READ_PHONE_STATE
            )
        )

        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

sealed class NavigationItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : NavigationItem("dashboard", "Home", Icons.Filled.Dashboard)
    object Replies : NavigationItem("replies", "Rules", Icons.Filled.Chat)
    object Gemini : NavigationItem("gemini", "Custom API", Icons.Filled.Code)
    object Logs : NavigationItem("logs", "Logs", Icons.Filled.History)
    object Statistics : NavigationItem("statistics", "Stats", Icons.Filled.BarChart)
    object Settings : NavigationItem("settings", "Settings", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = koinViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val labels = if (settings.appLanguage == "en") EnStrings else ArStrings
    val appError by viewModel.appError.collectAsStateWithLifecycle()

    if (appError != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissAppError() },
            title = { Text(if (settings.appLanguage == "en") "Notification Auto Reply Alert" else "تنبيه الرد التلقائي الذكي") },
            text = { Text(appError ?: "") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissAppError() }) {
                    Text(if (settings.appLanguage == "en") "OK" else "حسناً")
                }
            },
            icon = { Icon(Icons.Filled.Warning, contentDescription = "Error", tint = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        )
    }

    val navItems = listOf(
        NavigationItem.Dashboard,
        NavigationItem.Replies,
        NavigationItem.Gemini,
        NavigationItem.Logs,
        NavigationItem.Statistics,
        NavigationItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val layoutDirection = if (settings.appLanguage == "ar") {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                "dashboard" -> labels.appName
                                "replies" -> labels.customRepliesHeader
                                "gemini" -> labels.geminiEngineHeader
                                "logs" -> labels.logHeader
                                "statistics" -> labels.stats
                                "settings" -> labels.settings
                                else -> labels.appName
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("main_top_app_bar")
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav_bar")
                ) {
                    navItems.forEach { item ->
                        val title = when (item) {
                            NavigationItem.Dashboard -> labels.home
                            NavigationItem.Replies -> labels.rules
                            NavigationItem.Gemini -> labels.gemini
                            NavigationItem.Logs -> labels.logs
                            NavigationItem.Statistics -> labels.stats
                            NavigationItem.Settings -> labels.settings
                        }
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = title) },
                            label = { Text(title, maxLines = 1) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.testTag("nav_item_${item.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = NavigationItem.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(NavigationItem.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToReplies = { navController.navigate(NavigationItem.Replies.route) },
                        onNavigateToGemini = { navController.navigate(NavigationItem.Gemini.route) },
                        onNavigateToSettings = { navController.navigate(NavigationItem.Settings.route) }
                    )
                }
                composable(NavigationItem.Replies.route) {
                    RepliesScreen(viewModel = viewModel)
                }
                composable(NavigationItem.Gemini.route) {
                    GeminiSettingsScreen(viewModel = viewModel)
                }
                composable(NavigationItem.Logs.route) {
                    LogsScreen(
                        viewModel = viewModel,
                        onNavigateToReplies = {
                            navController.navigate(NavigationItem.Replies.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(NavigationItem.Statistics.route) {
                    StatisticsScreen(viewModel = viewModel)
                }
                composable(NavigationItem.Settings.route) {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
