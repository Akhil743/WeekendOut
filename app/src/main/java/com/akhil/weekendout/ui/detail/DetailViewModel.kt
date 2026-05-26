package com.akhil.weekendout.ui.detail

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhil.weekendout.data.auth.GoogleSignInHelper
import com.akhil.weekendout.data.auth.SignInResult
import com.akhil.weekendout.data.model.Place
import com.akhil.weekendout.data.repo.AuthRepository
import com.akhil.weekendout.data.repo.PlaceRepository
import com.akhil.weekendout.data.repo.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val place: Place? = null,
    val loadError: String? = null,
    val isSaved: Boolean = false,
    val saving: Boolean = false,
    val showSignInPrompt: Boolean = false,
    val signingIn: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val placeRepo: PlaceRepository,
    private val userData: UserDataRepository,
    private val authRepo: AuthRepository,
    private val signInHelper: GoogleSignInHelper
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state

    fun load(id: String) {
        _state.update { it.copy(loadError = null) }
        viewModelScope.launch {
            runCatching { placeRepo.getOne(id) }
                .onSuccess { place ->
                    _state.update {
                        it.copy(
                            place = place,
                            loadError = if (place == null) "We couldn't find that place." else null
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loadError = "Couldn't load this place. ${e.message ?: ""}".trim()) }
                }
        }
        viewModelScope.launch {
            userData.observeSavedIds().collect { ids ->
                _state.update { it.copy(isSaved = id in ids) }
            }
        }
    }

    /**
     * Toggle save. If saving (not unsaving) and the user is still anonymous, surface the
     * sign-in prompt first so their save persists across reinstalls. The actual write
     * still goes through afterwards regardless of their choice.
     */
    fun toggleSave() {
        val cur = _state.value
        val placeId = cur.place?.id ?: return
        if (cur.saving) return

        val isSaving = !cur.isSaved
        val anonymous = authRepo.currentUserIsAnonymous()
        if (isSaving && anonymous) {
            _state.update { it.copy(showSignInPrompt = true) }
            return
        }
        performSave(placeId, save = isSaving)
    }

    private fun performSave(placeId: String, save: Boolean) {
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching { userData.toggleSave(placeId, save) }
                .onFailure { e -> _state.update { it.copy(message = e.message) } }
            _state.update { it.copy(saving = false) }
        }
    }

    fun dismissSignInPrompt() {
        _state.update { it.copy(showSignInPrompt = false) }
    }

    fun skipSignIn() {
        val placeId = _state.value.place?.id ?: return
        _state.update { it.copy(showSignInPrompt = false) }
        performSave(placeId, save = true)
    }

    fun startGoogleSignIn(activity: Activity) {
        val placeId = _state.value.place?.id ?: return
        if (_state.value.signingIn) return
        _state.update { it.copy(signingIn = true) }
        viewModelScope.launch {
            when (val res = signInHelper.signIn(activity)) {
                is SignInResult.Success -> {
                    runCatching { authRepo.linkGoogle(res.idToken) }
                        .onSuccess {
                            _state.update {
                                it.copy(
                                    signingIn = false,
                                    showSignInPrompt = false,
                                    message = "Signed in. Saving…"
                                )
                            }
                            performSave(placeId, save = true)
                        }
                        .onFailure { e ->
                            _state.update {
                                it.copy(
                                    signingIn = false,
                                    message = "Sign-in upgrade failed: ${e.message}"
                                )
                            }
                        }
                }
                SignInResult.Cancelled -> _state.update { it.copy(signingIn = false) }
                is SignInResult.Failed -> _state.update {
                    it.copy(signingIn = false, message = res.message)
                }
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
