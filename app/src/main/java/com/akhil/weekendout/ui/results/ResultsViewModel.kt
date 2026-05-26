package com.akhil.weekendout.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhil.weekendout.data.model.RecommendedCard
import com.akhil.weekendout.data.repo.RecommendRepository
import com.akhil.weekendout.ui.PrefsHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

sealed interface ResultsState {
    data object Idle : ResultsState
    data object Loading : ResultsState
    data class Loaded(val cards: List<RecommendedCard>) : ResultsState
    data class Error(val message: String) : ResultsState
    data object NoMatches : ResultsState
}

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val repo: RecommendRepository,
    private val holder: PrefsHolder
) : ViewModel() {

    private val _state = MutableStateFlow<ResultsState>(ResultsState.Idle)
    val state: StateFlow<ResultsState> = _state

    init { fetch() }

    fun fetch() {
        val prefs = holder.peek() ?: run {
            _state.value = ResultsState.Error("No preferences set — go back and try again.")
            return
        }
        _state.value = ResultsState.Loading
        viewModelScope.launch {
            repo.recommend(prefs)
                .onSuccess { cards ->
                    _state.value = if (cards.isEmpty()) ResultsState.NoMatches
                    else ResultsState.Loaded(cards)
                }
                .onFailure { _state.value = ResultsState.Error(friendlyError(it)) }
        }
    }

    private fun friendlyError(t: Throwable): String = when (t) {
        is UnknownHostException -> "No internet connection. Check your connection and try again."
        is SocketTimeoutException -> "The request timed out. Try again."
        is IOException -> "Network error. Try again."
        else -> "Couldn't fetch picks. Try again."
    }
}
