package com.example.bgcollectibleguide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.bgcollectibleguide.data.LandmarkRepository
import com.example.bgcollectibleguide.models.Landmark
import com.example.bgcollectibleguide.ui.LandmarkCard
import kotlinx.coroutines.launch

/**
 * Screen displaying the user's personal collection of landmarks.
 * Features filtering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { LandmarkRepository() }

    val allLandmarks by repository.getLandmarks(context).collectAsState(initial = emptyList())
    val ownedData by repository.getOwnedLandmarkData().collectAsState(initial = emptyMap())

    var selectedRarity by remember { mutableStateOf("All") }
    var sortByRecent by remember { mutableStateOf(true) }
    var showFilters by remember { mutableStateOf(false) }
    
    val rarities = listOf("All", "Legendary", "Epic", "Rare", "Uncommon", "Common")

    val filteredLandmarks = remember(allLandmarks, ownedData, selectedRarity, sortByRecent) {
        allLandmarks
            .filter { ownedData.containsKey(it.id) }
            .filter { selectedRarity == "All" || it.rarity == selectedRarity }
            .let { list ->
                if (sortByRecent) {
                    list.sortedByDescending { ownedData[it.id] ?: 0L }
                } else {
                    list.sortedBy { it.name }
                }
            }
    }

    var selectedLandmark by remember { mutableStateOf<Landmark?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Collection (${filteredLandmarks.size})") },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (showFilters) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Filter by Rarity", style = MaterialTheme.typography.labelLarge)

                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            ScrollableTabRow(
                                selectedTabIndex = rarities.indexOf(selectedRarity),
                                edgePadding = 0.dp,
                                containerColor = Color.Transparent,
                                divider = {}
                            ) {
                                rarities.forEach { rarity ->
                                    FilterChip(
                                        selected = selectedRarity == rarity,
                                        onClick = { selectedRarity = rarity },
                                        label = { Text(rarity) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = sortByRecent,
                                onCheckedChange = { sortByRecent = it }
                            )
                            Text("Sort by Recently Acquired")
                        }
                    }
                }
            }

            if (filteredLandmarks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (ownedData.isEmpty()) "Your collection is empty. Go find some landmarks!"
                        else "No landmarks match your filter.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLandmarks) { landmark ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f)
                                .clickable { selectedLandmark = landmark },
                            contentAlignment = Alignment.Center
                        ) {
                            LandmarkCard(
                                landmark = landmark,
                                modifier = Modifier.scale(0.55f)
                            )
                        }
                    }
                }
            }
        }

        // Zoom
        selectedLandmark?.let { landmark ->
            Dialog(onDismissRequest = { selectedLandmark = null }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LandmarkCard(
                        landmark = landmark,
                        modifier = Modifier.clickable { selectedLandmark = null }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                repository.removeLandmark(landmark.id)
                                selectedLandmark = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Remove from Collection", color = Color.White)
                    }
                }
            }
        }
    }
}
