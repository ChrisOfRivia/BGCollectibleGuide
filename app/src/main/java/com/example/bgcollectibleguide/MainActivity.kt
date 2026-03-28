package com.example.bgcollectibleguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home // Added for Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.bgcollectibleguide.ui.theme.BGCollectibleGuideTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val auth = FirebaseAuth.getInstance()

        setContent {
            var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

            BGCollectibleGuideTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (isLoggedIn) {
                        // Предаваме ламбда функция, която изключва потребителя
                        MainScreen(onLogout = {
                            auth.signOut()
                            isLoggedIn = false
                        })
                    } else {
                        LoginScreen(onLoginSuccess = { isLoggedIn = true })
                    }
                }
            }
        }
    }
}

// --- COMPOSE SCREENS ---

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()

    // 1. Вземаме информация за текущия екран
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // 2. Подаваме и двата аргумента тук
            BottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "map",
            modifier = Modifier.padding(padding)
        ) {
            composable("map") { MapScreen() }
            composable("profile") { ProfileScreen(onLogout) }
            composable("collection") { CollectionScreen() }
        }
    }
}

// ВАЖНО: Трябва да промениш и самата функция ProfileScreen!
@Composable
fun ProfileScreen(onLogout: () -> Unit) { // Добави параметъра тук
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Profile Screen")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onLogout) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = { navController.navigate("profile") { launchSingleTop = true } },
            label = { Text("Profile") },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") }
        )

        NavigationBarItem(
            selected = currentRoute == "collection",
            onClick = { navController.navigate("collection") { launchSingleTop = true } },
            label = { Text("Collection") },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Collection") }
        )

        NavigationBarItem(
            selected = currentRoute == "map",
            onClick = { navController.navigate("map") { launchSingleTop = true } },
            label = { Text("Map") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Map") } // Fixed Icon
        )
    }
}

// Mock Screens (Implement these in separate files later!)
@Composable fun MapScreen() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Map Screen") } }
@Composable fun ProfileScreen() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Profile Screen") } }
@Composable fun CollectionScreen() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Collection Screen") } }