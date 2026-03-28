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
import com.example.bgcollectibleguide.data.LandmarkRepository
import com.example.bgcollectibleguide.models.Landmark
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    if (locationPermissionState.status.isGranted) {
        CurrentLocationMap()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                Text("Request Location Permission")
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun CurrentLocationMap() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { LandmarkRepository() }
    
    val landmarks by repository.getLandmarks().collectAsState(initial = emptyList())
    val ownedIds by repository.getOwnedLandmarkIds().collectAsState(initial = emptySet())

    val cameraPositionState = rememberCameraPositionState {
        // Start view at TU Sofia
        position = CameraPosition.fromLatLngZoom(LatLng(42.6570, 23.3551), 15f)
    }

    // Tracks if we have already snapped the camera to the user's real location
    var hasSnappedToUser by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var landmarkToCollect by remember { mutableStateOf<Landmark?>(null) }

    // Real-time location tracking
    DisposableEffect(Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { loc ->
                    val userLatLng = LatLng(loc.latitude, loc.longitude)
                    
                    // If the user's location changes (e.g. via adb command), 
                    // snap the camera to them once.
                    if (!hasSnappedToUser) {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 17f)
                        hasSnappedToUser = true
                    }

                    // Check proximity to any landmark
                    val nearby = landmarks.find { landmark ->
                        !ownedIds.contains(landmark.id) && isWithinRange(loc.latitude, loc.longitude, landmark)
                    }
                    landmarkToCollect = nearby
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)
        onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true)
        ) {
            landmarks.forEach { landmark ->
                val isOwned = ownedIds.contains(landmark.id)
                
                Circle(
                    center = LatLng(landmark.latitude, landmark.longitude),
                    radius = landmark.radius,
                    fillColor = if (isOwned) Color(0x3300FF00) else Color(0x330000FF),
                    strokeColor = if (isOwned) Color.Green else Color.Blue,
                    strokeWidth = 2f
                )

                Marker(
                    state = rememberMarkerState(position = LatLng(landmark.latitude, landmark.longitude)),
                    title = landmark.name,
                    snippet = if (isOwned) "Collected!" else landmark.rarity,
                    alpha = if (isOwned) 0.6f else 1.0f
                )
            }
        }

        // Collection UI Popup
        landmarkToCollect?.let { landmark ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("You've discovered ${landmark.name}!", style = MaterialTheme.typography.headlineSmall)
                    Text("Rarity: ${landmark.rarity}", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            repository.collectLandmark(landmark.id)
                            landmarkToCollect = null
                            Toast.makeText(context, "Added to collection!", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Collect Landmark")
                    }
                }
            }
        }
    }
}

private fun isWithinRange(lat: Double, lng: Double, landmark: Landmark): Boolean {
    val results = FloatArray(1)
    Location.distanceBetween(
        lat, lng,
        landmark.latitude, landmark.longitude,
        results
    )
    return results[0] <= landmark.radius
}
