package com.akhil.weekendout.data.repo

import com.akhil.weekendout.data.model.Place
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaceRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /** Hydrate ranked place_ids in chunks of 10 (Firestore `whereIn` cap). */
    suspend fun getByIds(ids: List<String>): List<Place> {
        if (ids.isEmpty()) return emptyList()
        val out = mutableListOf<Place>()
        ids.chunked(10).forEach { chunk ->
            val snap = firestore.collection("places")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                .get().await()
            snap.documents.forEach { doc ->
                doc.toPlace()?.let(out::add)
            }
        }
        // Preserve the input order (LLM ranking matters).
        val byId = out.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    suspend fun getOne(id: String): Place? =
        firestore.collection("places").document(id).get().await().toPlace()

    private fun com.google.firebase.firestore.DocumentSnapshot.toPlace(): Place? {
        if (!exists()) return null
        @Suppress("UNCHECKED_CAST")
        val geoMap = get("geo") as? Map<String, Any?>
        return Place(
            id = id,
            name = getString("name") ?: return null,
            city = getString("city") ?: "",
            region = getString("region"),
            address = getString("address"),
            geo = geoMap?.let {
                Place.Geo(
                    lat = (it["lat"] as? Number)?.toDouble() ?: 0.0,
                    lng = (it["lng"] as? Number)?.toDouble() ?: 0.0,
                    geohash = it["geohash"] as? String
                )
            },
            vibe = (get("vibe") as? List<String>) ?: emptyList(),
            crowd = getString("crowd"),
            good_for = (get("good_for") as? List<String>) ?: emptyList(),
            temp_class = getString("temp_class"),
            has_stay = getBoolean("has_stay") ?: false,
            duration_class = getString("duration_class"),
            budget = (getLong("budget") ?: 1L).toInt(),
            season = (get("season") as? List<String>) ?: emptyList(),
            drive_time_from_blr_min = (getLong("drive_time_from_blr_min") ?: 0L).toInt(),
            photo_urls = (get("photo_urls") as? List<String>) ?: emptyList(),
            short_summary = getString("short_summary") ?: "",
            maps_query = getString("maps_query") ?: ""
        )
    }
}
