package com.akhil.weekendout.data.model

import com.squareup.moshi.JsonClass

enum class DurationClass(val wire: String) {
    FewHours("few-hours"), HalfDay("half-day"), FullDay("full-day"), Weekend("weekend")
}

enum class GroupType(val wire: String) {
    Couples("couples"), Groups("groups"), Solo("solo"), Adventure("adventure"), Family("family")
}

enum class Crowd(val wire: String) { Low("low"), Medium("medium"), High("high") }
enum class TempPref(val wire: String) { Cool("cool"), Temperate("temperate"), Warm("warm") }

@JsonClass(generateAdapter = true)
data class UserPrefs(
    val city: String,
    val durationClass: String,        // serialized as "few-hours" etc.
    val groupType: String,
    val vibe: List<String>,
    val crowd: String,
    val tempPref: String,
    val stayRequired: Boolean,
    val monthIso: String,             // "Jan".."Dec"
    val budget: Int,                  // 1..4
    val city_allowlist: List<String>? = null
)

object PrefBuilders {
    fun fromUi(
        city: String,
        duration: DurationClass,
        group: GroupType,
        vibe: List<String>,
        crowd: Crowd,
        temp: TempPref,
        stay: Boolean,
        monthIso: String,
        budget: Int,
        allowlist: List<String>? = null
    ) = UserPrefs(
        city = city,
        durationClass = duration.wire,
        groupType = group.wire,
        vibe = vibe,
        crowd = crowd.wire,
        tempPref = temp.wire,
        stayRequired = stay,
        monthIso = monthIso,
        budget = budget,
        city_allowlist = allowlist
    )
}
