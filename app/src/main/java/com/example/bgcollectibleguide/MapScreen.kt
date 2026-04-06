package com.example.bgcollectibleguide

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.bgcollectibleguide.data.LandmarkRepository
import com.example.bgcollectibleguide.models.Landmark
import com.example.bgcollectibleguide.ui.LandmarkCard
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

/**
 * Main entry point for the Map tab.
 * Handles the logic for requesting location permissions before showing the map.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    // State object to track and request the Fine Location permission
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Check if the permission is already granted by the user
    if (locationPermissionState.status.isGranted) {
        // If granted, proceed to the actual map implementation
        CurrentLocationMap()
    } else {
        // If not granted, show a centered button to trigger the system permission dialog
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                Text("Allow Map Access")
            }
        }
    }
}

/**
 * The core map component. 
 * Manages Google Maps, real-time location tracking, and landmark interaction logic.
 */
@SuppressLint("MissingPermission") // Permission check is handled in the parent MapScreen
@Composable
fun CurrentLocationMap() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { LandmarkRepository() }
    
    // Live collection of all landmarks from the database
    val landmarks by repository.getLandmarks(context).collectAsState(initial = emptyList())
    // Set of IDs the user has already collected
    val ownedIds by repository.getOwnedLandmarkIds().collectAsState(initial = emptySet())

    // Initial camera state focused on Sofia, Bulgaria
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(42.6570, 23.3551), 15f)
    }

    // Client for interacting with Google Play Services location APIs
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // UI States for handling popups
    var landmarkToCollect by remember { mutableStateOf<Landmark?>(null) }
    var showCollectedCard by remember { mutableStateOf<Landmark?>(null) }

    // Start listening for location updates when this component enters the screen
    DisposableEffect(Unit) {
        // Configure how often we want location updates (every 2 seconds, high accuracy)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        // Callback triggered whenever the device's location changes
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { loc ->
                    // Check if the user has walked into the radius of an uncollected landmark
                    val nearby = landmarks.find { landmark ->
                        !ownedIds.contains(landmark.id) && isWithinRange(loc.latitude, loc.longitude, landmark)
                    }
                    // If a nearby uncollected landmark is found, trigger the discovery popup
                    landmarkToCollect = nearby
                }
            }
        }

        // Register the callback with the system
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)

        // Cleanup: Stop location updates when the user leaves the map screen to save battery
        onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // The Google Map UI
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true), // Shows the "Blue Dot"
            uiSettings = MapUiSettings(myLocationButtonEnabled = true) // Shows the "Center on Me" button
        ) {
            // Draw all landmarks on the map
            landmarks.forEach { landmark ->
                val isOwned = ownedIds.contains(landmark.id)
                
                // Visual circle representing the collection area
                Circle(
                    center = LatLng(landmark.latitude, landmark.longitude),
                    radius = landmark.radius,
                    fillColor = if (isOwned) Color(0x3300FF00) else Color(0x330000FF), // Green if owned, Blue if not
                    strokeColor = if (isOwned) Color.Green else Color.Blue,
                    strokeWidth = 2f
                )

                // Standard marker pin
                Marker(
                    state = rememberMarkerState(position = LatLng(landmark.latitude, landmark.longitude)),
                    title = landmark.name,
                    snippet = if (isOwned) "Collected!" else landmark.rarity,
                    alpha = if (isOwned) 0.6f else 1.0f, // Fade out owned landmarks slightly
                    onInfoWindowClick = {
                        // If the user taps the info bubble of an owned landmark, show its card
                        if (isOwned) {
                            showCollectedCard = landmark
                        }
                    }
                )
            }
        }

        // Discovery Popup: Triggered when walk near a new landmark
        landmarkToCollect?.let { landmark ->
            Dialog(onDismissRequest = { landmarkToCollect = null }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LandmarkCard(landmark = landmark)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Update the database to mark this landmark as owned
                            scope.launch {
                                repository.collectLandmark(landmark.id)
                                landmarkToCollect = null
                                Toast.makeText(context, "${landmark.name} added to collection!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Claim Collectible", color = Color.White)
                    }
                }
            }
        }

        // Inspect Mode: Shows the full card for a landmark the user already owns
        showCollectedCard?.let { landmark ->
            Dialog(onDismissRequest = { showCollectedCard = null }) {
                LandmarkCard(landmark = landmark)
            }
        }
    }
}

/**
 * Helper function to calculate if a coordinate is within a landmark's radius.
 * Uses the Haversine formula via Android's Location.distanceBetween.
 */
private fun isWithinRange(lat: Double, lng: Double, landmark: Landmark): Boolean {
    val results = FloatArray(1)
    Location.distanceBetween(lat, lng, landmark.latitude, landmark.longitude, results)
    // results[0] contains the distance in meters
    return results[0] <= landmark.radius
}
