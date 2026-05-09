package com.akhil.weekendout.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhil.weekendout.data.model.Crowd
import com.akhil.weekendout.data.model.DurationClass
import com.akhil.weekendout.data.model.GroupType
import com.akhil.weekendout.data.model.PrefBuilders
import com.akhil.weekendout.data.model.TempPref
import com.akhil.weekendout.ui.PrefsHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class PlannerUiState(
    val durationClass: DurationClass = DurationClass.HalfDay,
    val groupType: GroupType = GroupType.Couples,
    val vibe: Set<String> = setOf("scenic"),
    val crowd: Crowd = Crowd.Medium,
    val tempPref: TempPref = TempPref.Temperate,
    val stayRequired: Boolean = false,
    val budget: Int = 2,
    val dateMillis: Long = System.currentTimeMillis(),
    val includeNearbyGetaways: Boolean = true
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val holder: PrefsHolder
) : ViewModel() {

    private val _state = MutableStateFlow(PlannerUiState())
    val state: StateFlow<PlannerUiState> = _state

    fun setDuration(d: DurationClass) { _state.value = _state.value.copy(durationClass = d) }
    fun setGroup(g: GroupType) { _state.value = _state.value.copy(groupType = g) }
    fun setCrowd(c: Crowd) { _state.value = _state.value.copy(crowd = c) }
    fun setTemp(t: TempPref) { _state.value = _state.value.copy(tempPref = t) }
    fun setStay(b: Boolean) { _state.value = _state.value.copy(stayRequired = b) }
    fun setBudget(n: Int) { _state.value = _state.value.copy(budget = n.coerceIn(1, 4)) }
    fun setDate(ms: Long) { _state.value = _state.value.copy(dateMillis = ms) }
    fun setIncludeGetaways(b: Boolean) { _state.value = _state.value.copy(includeNearbyGetaways = b) }
    fun toggleVibe(v: String) {
        val cur = _state.value.vibe
        _state.value = _state.value.copy(vibe = if (v in cur) cur - v else cur + v)
    }

    fun submit(onDone: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        val month = SimpleDateFormat("MMM", Locale.US).format(Calendar.getInstance().apply {
            timeInMillis = s.dateMillis
        }.time)
        val allowlist = if (s.includeNearbyGetaways) NEARBY_BLR else listOf("Bengaluru")
        val prefs = PrefBuilders.fromUi(
            city = "Bengaluru",
            duration = s.durationClass,
            group = s.groupType,
            vibe = s.vibe.toList(),
            crowd = s.crowd,
            temp = s.tempPref,
            stay = s.stayRequired,
            monthIso = month,
            budget = s.budget,
            allowlist = allowlist
        )
        holder.set(prefs)
        onDone()
    }

    companion object {
        val ALL_VIBES = listOf("scenic", "quiet", "lively", "romantic", "adventure", "retro", "offbeat")
        val NEARBY_BLR = listOf(
            "Bengaluru", "Nandi Hills", "Skandagiri",
            "Madikeri", "Chikmagalur", "Sakleshpur",
            "Mysuru", "Ooty", "Yelagiri"
        )
    }
}
