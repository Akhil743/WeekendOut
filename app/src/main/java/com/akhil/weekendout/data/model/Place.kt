package com.akhil.weekendout.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Place(
    val id: String,
    val name: String,
    val city: String,
    val region: String? = null,
    val address: String? = null,
    val geo: Geo? = null,
    val vibe: List<String> = emptyList(),
    val crowd: String? = null,
    val good_for: List<String> = emptyList(),
    val temp_class: String? = null,
    val has_stay: Boolean = false,
    val duration_class: String? = null,
    val budget: Int = 1,
    val season: List<String> = emptyList(),
    val drive_time_from_blr_min: Int = 0,
    val photo_urls: List<String> = emptyList(),
    val short_summary: String = "",
    val maps_query: String = ""
) {
    @JsonClass(generateAdapter = true)
    data class Geo(val lat: Double, val lng: Double, val geohash: String? = null)
}

@JsonClass(generateAdapter = true)
data class RankedPlace(@Json(name = "place_id") val placeId: String, val why: String)

@JsonClass(generateAdapter = true)
data class RecommendResponse(
    val ranked: List<RankedPlace>,
    val candidate_count: Int = 0,
    val cached: Boolean = false
)

/** Hydrated card used by the Results screen. */
data class RecommendedCard(val place: Place, val why: String)
