package com.akhil.weekendout.data.repo

import com.akhil.weekendout.data.model.RecommendedCard
import com.akhil.weekendout.data.model.UserPrefs
import com.akhil.weekendout.data.remote.RecommendApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendRepository @Inject constructor(
    private val api: RecommendApi,
    private val auth: AuthRepository,
    private val places: PlaceRepository
) {
    suspend fun recommend(prefs: UserPrefs): Result<List<RecommendedCard>> = runCatching {
        auth.ensureSignedIn()
        val token = auth.appCheckToken()
        val resp = api.recommend(token, prefs)
        val placesById = places.getByIds(resp.ranked.map { it.placeId }).associateBy { it.id }
        resp.ranked.mapNotNull { r ->
            placesById[r.placeId]?.let { p -> RecommendedCard(p, r.why) }
        }
    }
}
