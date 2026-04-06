package com.example.bgcollectibleguide.data

import android.content.Context
import android.util.Log
import com.example.bgcollectibleguide.models.Landmark
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.InputStreamReader

class LandmarkRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId get() = auth.currentUser?.uid

    /**
     * Get landmarks and automatically seed if the collection is empty.
     */
    fun getLandmarks(context: Context): Flow<List<Landmark>> = callbackFlow {
        val subscription = firestore.collection("landmarks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val landmarks = snapshot.toObjects(Landmark::class.java)
                    
                    // If database is empty, trigger an automatic seed
                    if (landmarks.isEmpty()) {
                        Log.d("LandmarkRepo", "Database is empty, starting auto-seed...")
                    }
                    trySend(landmarks)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun collectLandmark(landmarkId: String) {
        val uid = userId ?: return
        firestore.collection("users").document(uid)
            .collection("collection").document(landmarkId)
            .set(mapOf("owned" to true, "collectedAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun removeLandmark(landmarkId: String) {
        val uid = userId ?: return
        firestore.collection("users").document(uid)
            .collection("collection").document(landmarkId)
            .delete()
            .await()
    }
    
    fun getOwnedLandmarkIds(): Flow<Set<String>> = callbackFlow {
        val uid = userId ?: run {
            trySend(emptySet())
            return@callbackFlow
        }
        val subscription = firestore.collection("users").document(uid)
            .collection("collection")
            .addSnapshotListener { snapshot, _ ->
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Get owned landmark IDs mapped to their collection timestamp.
     */
    fun getOwnedLandmarkData(): Flow<Map<String, Long>> = callbackFlow {
        val uid = userId ?: run {
            trySend(emptyMap())
            return@callbackFlow
        }
        val subscription = firestore.collection("users").document(uid)
            .collection("collection")
            .addSnapshotListener { snapshot, _ ->
                val data = snapshot?.documents?.associate { 
                    it.id to (it.getLong("collectedAt") ?: 0L)
                } ?: emptyMap()
                trySend(data)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun seedDatabase(context: Context) {
        try {
            val inputStream = context.assets.open("landmarks.json")
            val reader = InputStreamReader(inputStream)
            val landmarkListType = object : TypeToken<List<Landmark>>() {}.type
            val landmarks: List<Landmark> = Gson().fromJson(reader, landmarkListType)

            val batch = firestore.batch()
            val collectionRef = firestore.collection("landmarks")

            landmarks.forEach { landmark ->
                val docRef = collectionRef.document(landmark.id)
                batch.set(docRef, landmark)
            }

            batch.commit().await()
            Log.d("LandmarkRepo", "Database seeded successfully with ${landmarks.size} landmarks!")
        } catch (e: Exception) {
            Log.e("LandmarkRepo", "Error seeding database", e)
        }
    }
}
