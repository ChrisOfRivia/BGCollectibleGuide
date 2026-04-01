package com.example.bgcollectibleguide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.bgcollectibleguide.data.LandmarkRepository
import com.example.bgcollectibleguide.models.Landmark
import com.example.bgcollectibleguide.ui.LandmarkCard

@Composable
fun CollectionScreen() {
    val context = LocalContext.current
    val repository = remember { LandmarkRepository() }
    
    val allLandmarks by repository.getLandmarks(context).collectAsState(initial = emptyList())
    val ownedIds by repository.getOwnedLandmarkIds().collectAsState(initial = emptySet())

    val collectedLandmarks = allLandmarks.filter { ownedIds.contains(it.id) }
    
    var selectedLandmark by remember { mutableStateOf<Landmark?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (collectedLandmarks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your collection is empty. Go find some landmarks!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // Changed to 2 cards per row for better visibility
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(collectedLandmarks) { landmark ->
                    // Card container in the grid
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.7f) // Keep card ratio (260/380 approx 0.68)
                            .clickable { selectedLandmark = landmark },
                        contentAlignment = Alignment.Center
                    ) {
                        // We scale the card down to fit the grid cell
                        // With 2 columns, 0.55f to 0.6f fits much better
                        LandmarkCard(
                            landmark = landmark,
                            modifier = Modifier.scale(0.55f)
                        )
                    }
                }
            }
        }

        // Full-screen zoom view
        selectedLandmark?.let { landmark ->
            Dialog(onDismissRequest = { selectedLandmark = null }) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LandmarkCard(
                        landmark = landmark,
                        modifier = Modifier.clickable { selectedLandmark = null } // Tap again to close
                    )
                }
            }
        }
    }
}
