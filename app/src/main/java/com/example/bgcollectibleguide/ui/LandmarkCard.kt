package com.example.bgcollectibleguide.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.bgcollectibleguide.models.Landmark

@Composable
fun LandmarkCard(landmark: Landmark, modifier: Modifier = Modifier) {
    val frameColor = when (landmark.rarity) {
        "Legendary" -> Color(0xFFFFD700) // Gold
        "Epic" -> Color(0xFF9400D3) // Purple
        "Rare" -> Color(0xFF1E90FF) // Blue
        "Uncommon" -> Color(0xFF32CD32) // Lime Green
        else -> Color(0xFFCD7F32) // Bronze/Brown
    }

    val cardBackground = Color(0xFFF5DEB3) // Classic parchment color

    Card(
        modifier = modifier
            .width(260.dp)
            .height(380.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(frameColor, cardBackground),
                        startY = 0f,
                        endY = 400f
                    )
                )
                .padding(8.dp)
                .border(4.dp, frameColor, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            // Header: Name and Rarity Symbol
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = landmark.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(frameColor, RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Black, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = landmark.rarity.first().toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .border(3.dp, Color(0xFF3E2723)) // Dark brown frame
                    .background(Color.Gray)
            ) {
                if (landmark.imageUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(landmark.imageUrl),
                        contentDescription = landmark.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Image", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description/Effect Box
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAAFFFFFF), RoundedCornerShape(2.dp))
                    .border(1.dp, Color.Gray)
                    .padding(6.dp)
            ) {
                Text(
                    text = "[Landmark / ${landmark.rarity}]",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = landmark.description,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 14.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
