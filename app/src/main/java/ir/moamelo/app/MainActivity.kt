package ir.moamelo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Session.init(this)
        setContent {
            MoameloTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
                        if (Session.loggedIn) MainNav() else LoginScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun MainNav() {
    val nav = rememberNavController()
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route
    val tabs = listOf(
        Triple("home", "خانه", Icons.Filled.Home),
        Triple("account", "حساب و اشتراک", Icons.Filled.Person),
        Triple("support", "پشتیبانی", Icons.Filled.SupportAgent)
    )
    Scaffold(
        containerColor = Bg,
        bottomBar = {
            if (tabs.any { it.first == route }) {
                NavigationBar(containerColor = CardBg) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.first,
                            onClick = {
                                nav.navigate(tab.first) {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(tab.third, contentDescription = tab.second) },
                            label = { Text(tab.second, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OnBrand,
                                selectedTextColor = Brand,
                                indicatorColor = Brand,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(nav) }
            composable("account") { AccountScreen() }
            composable("support") { SupportScreen() }
            composable("properties/{type}") { e ->
                PropertyListScreen(nav, e.arguments?.getString("type") ?: "sale")
            }
            composable("property/{id}") { e ->
                PropertyDetailScreen(nav, e.arguments?.getString("id")?.toIntOrNull() ?: 0)
            }
            composable("property_form/{type}/{id}") { e ->
                PropertyFormScreen(
                    nav,
                    e.arguments?.getString("type") ?: "sale",
                    e.arguments?.getString("id")?.toIntOrNull() ?: 0
                )
            }
            composable("customers") { CustomerListScreen(nav) }
            composable("customer/{id}") { e ->
                CustomerDetailScreen(nav, e.arguments?.getString("id")?.toIntOrNull() ?: 0)
            }
            composable("customer_form/{id}") { e ->
                CustomerFormScreen(nav, e.arguments?.getString("id")?.toIntOrNull() ?: 0)
            }
            composable("matches") { MatchesScreen(nav) }
            composable("notifications") { NotificationsScreen(nav) }
        }
    }
}
