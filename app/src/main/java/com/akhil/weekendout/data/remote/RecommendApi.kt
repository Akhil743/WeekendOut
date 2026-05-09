package com.akhil.weekendout.data.remote

import com.akhil.weekendout.data.model.RecommendResponse
import com.akhil.weekendout.data.model.UserPrefs
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface RecommendApi {
    @POST("recommend")
    suspend fun recommend(
        @Header("X-Firebase-AppCheck") appCheckToken: String,
        @Body prefs: UserPrefs
    ): RecommendResponse
}
