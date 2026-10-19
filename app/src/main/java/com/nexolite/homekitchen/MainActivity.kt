package com.nexolite.homekitchen

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexolite.homekitchen.ui.AppViewModel
import com.nexolite.homekitchen.ui.DuesScreen
import com.nexolite.homekitchen.ui.MenuScreen
import com.nexolite.homekitchen.ui.OrderEditorScreen
import com.nexolite.homekitchen.ui.OrdersScreen
import com.nexolite.homekitchen.ui.ReportsScreen
import com.nexolite.homekitchen.ui.SettingsScreen
import com.nexolite.homekitchen.ui.theme.HomeKitchenTheme
import com.nexolite.homekitchen.ui.theme.Palette

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("menu", "Menu", Icons.Rounded.RestaurantMenu),
    Tab("orders", "Orders", Icons.AutoMirrored.Rounded.ReceiptLong),
    Tab("dues", "Dues", Icons.Rounded.AccountBalanceWallet),
    Tab("reports", "Reports", Icons.Rounded.BarChart)
)

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.WHITE, AndroidColor.WHITE)
        )
        super.onCreate(savedInstanceState)
        setContent {
            HomeKitchenTheme { KitchenApp(vm) }
        }
    }
}

@Composable
private fun KitchenApp(vm: AppViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showTabs = tabs.any { it.route == route }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Palette.Ground,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (showTabs) BottomTabs(nav, route) }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "menu",
            modifier = Modifier.padding(padding)
        ) {
            composable("menu") { MenuScreen(vm, onOpenSettings = { nav.navigate("settings") }) }
            composable("orders") {
                OrdersScreen(
                    vm,
                    onNewOrder = { nav.navigate("order/0") },
                    onOpenOrder = { id -> nav.navigate("order/$id") }
                )
            }
            composable("dues") { DuesScreen(vm) }
            composable("reports") { ReportsScreen(vm) }
            composable("settings") { SettingsScreen(vm, onClose = { nav.popBackStack() }) }
            composable(
                "order/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                OrderEditorScreen(vm, orderId = id.takeIf { it > 0 }, onClose = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomTabs(nav: NavHostController, route: String?) {
    Column {
        HorizontalDivider(color = Palette.Line)
        NavigationBar(containerColor = Palette.Card, tonalElevation = 0.dp) {
            tabs.forEach { tab ->
                NavigationBarItem(
                    selected = route == tab.route,
                    onClick = {
                        nav.navigate(tab.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Palette.Brand,
                        selectedTextColor = Palette.Brand,
                        indicatorColor = Palette.BrandSoft,
                        unselectedIconColor = Palette.Muted,
                        unselectedTextColor = Palette.Muted
                    )
                )
            }
        }
    }
}
