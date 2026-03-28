package com.example.bgcollectibleguide.models

import com.google.firebase.firestore.PropertyName

data class Landmark(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Double = 100.0,
    val rarity: String = "Common",
    val imageUrl: String = "",
    @get:PropertyName("isOwned") @set:PropertyName("isOwned")
    var isOwned: Boolean = false
)
