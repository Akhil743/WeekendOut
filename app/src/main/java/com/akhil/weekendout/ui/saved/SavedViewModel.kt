package com.akhil.weekendout.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhil.weekendout.data.model.Place
import com.akhil.weekendout.data.repo.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SavedState {
    data object Loading : SavedState
    data class Loaded(val places: List<Place>) : SavedState
    data object Empty : SavedState
    data class Error(val message: String) : SavedState
}

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val userData: UserDataRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SavedState>(SavedState.Loading)
    val state: StateFlow<SavedState> = _state

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        runCatching { userData.getSavedPlaces() }
            .onSuccess { _state.value = if (it.isEmpty()) SavedState.Empty else SavedState.Loaded(it) }
            .onFailure { _state.value = SavedState.Error(it.message ?: "Failed to load saved places.") }
    }
}
