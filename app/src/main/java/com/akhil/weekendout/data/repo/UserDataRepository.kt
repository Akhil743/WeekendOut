package com.akhil.weekendout.data.repo

import com.akhil.weekendout.data.model.Place
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
    private val places: PlaceRepository
) {
    suspend fun toggleSave(placeId: String, save: Boolean) {
        val uid = auth.ensureSignedIn().uid
        val ref = firestore.collection("users").document(uid)
        val update = if (save) FieldValue.arrayUnion(placeId) else FieldValue.arrayRemove(placeId)
        ref.set(mapOf("saved_place_ids" to update), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    fun observeSavedIds(): Flow<List<String>> = callbackFlow {
        val uid = auth.ensureSignedIn().uid
        val reg = firestore.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                @Suppress("UNCHECKED_CAST")
                val ids = (snap?.get("saved_place_ids") as? List<String>) ?: emptyList()
                trySend(ids)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getSavedPlaces(): List<Place> {
        val uid = auth.ensureSignedIn().uid
        val doc = firestore.collection("users").document(uid).get().await()
        @Suppress("UNCHECKED_CAST")
        val ids = (doc.get("saved_place_ids") as? List<String>) ?: emptyList()
        return places.getByIds(ids)
    }
}
