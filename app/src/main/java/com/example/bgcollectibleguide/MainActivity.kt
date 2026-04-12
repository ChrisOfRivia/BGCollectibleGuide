package com.example.bgcollectibleguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.bgcollectibleguide.data.LandmarkRepository
import com.example.bgcollectibleguide.ui.theme.BGCollectibleGuideTheme
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * The main entry point of the application.
 * Manages the high-level authentication state and navigation setup.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enables edge-to-edge display (drawing behind status/navigation bars)
        enableEdgeToEdge()

        val auth = FirebaseAuth.getInstance()
        val repository = LandmarkRepository()

        setContent {
            // Track if the user is currently logged in via Firebase
            var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
            val scope = rememberCoroutineScope()

            // Seed the database with local landmark data if the user is logged in
            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) {
                    repository.seedDatabase(this@MainActivity)
                }
            }

            BGCollectibleGuideTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (isLoggedIn) {
                        // User is authenticated, show the main app content
                        MainScreen(onLogout = {
                            auth.signOut()
                            isLoggedIn = false
                        })
                    } else {
                        // User is not authenticated, show the login/registration screen
                        LoginScreen(onLoginSuccess = { isLoggedIn = true })
                    }
                }
            }
        }
    }
}

/**
 * Main application container after login.
 * Sets up the Bottom Navigation Bar and the NavHost for switching between screens.
 */
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Persistent bottom navigation menu
            BottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { padding ->
        // Navigation container that switches between Map, Collection, and Profile
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

/**
 * User Profile screen displaying account information and collection statistics.
 */
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { LandmarkRepository() }
    val auth = remember { FirebaseAuth.getInstance() }
    val user = auth.currentUser

    // Observe flows from the repository for real-time stats updates
    val allLandmarks by repository.getLandmarks(context).collectAsState(initial = emptyList())
    val ownedIds by repository.getOwnedLandmarkIds().collectAsState(initial = emptySet())

    // Calculate collection statistics
    val totalLandmarks = allLandmarks.size
    val collectedCount = ownedIds.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Profile Picture Section
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 4.dp
        ) {
            if (user?.photoUrl != null) {
                // Display user's profile image if available (e.g., from Google Login)
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Default icon if no profile picture is set
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Profile",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User Identity Information
        Text(
            text = user?.displayName ?: "Explorer",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = user?.email ?: "Not signed in",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Collection Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Collection Stats",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Stat counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(label = "Collected", value = collectedCount.toString())
                    StatItem(label = "Total", value = totalLandmarks.toString())
                    
                    // Specific stat for high-rarity items
                    val legendaryCount = allLandmarks.filter { ownedIds.contains(it.id) }.count { it.rarity == "Legendary" }
                    StatItem(label = "Legendary", value = legendaryCount.toString())
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sign out button
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text("Logout")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * A reusable UI component for displaying a labeled statistic.
 */
@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The bottom navigation bar UI.
 * Handles the selection state and navigation actions.
 */
@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        // Profile Tab
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = { navController.navigate("profile") { launchSingleTop = true } },
            label = { Text("Profile") },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") }
        )

        // Collection Tab
        NavigationBarItem(
            selected = currentRoute == "collection",
            onClick = { navController.navigate("collection") { launchSingleTop = true } },
            label = { Text("Collection") },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Collection") }
        )

        // Map Tab
        NavigationBarItem(
            selected = currentRoute == "map",
            onClick = { navController.navigate("map") { launchSingleTop = true } },
            label = { Text("Map") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Map") }
        )
    }
}
