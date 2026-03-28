package com.example.bgcollectibleguide.data

import com.example.bgcollectibleguide.models.Landmark
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LandmarkRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId get() = auth.currentUser?.uid

    /**
     * Get all landmarks.
     * In a real app, 'isOwned' should be stored per user. 
     * For simplicity here, we'll assume a 'user_collections' collection.
     */
    fun getLandmarks(): Flow<List<Landmark>> = callbackFlow {
        val subscription = firestore.collection("landmarks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val landmarks = snapshot.toObjects(Landmark::class.java)
                    // We would also merge user-specific 'isOwned' status here
                    trySend(landmarks)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun collectLandmark(landmarkId: String) {
        val uid = userId ?: return
        // Mark as owned for this user in a sub-collection or separate collection
        firestore.collection("users").document(uid)
            .collection("collection").document(landmarkId)
            .set(mapOf("owned" to true, "collectedAt" to System.currentTimeMillis()))
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
}
