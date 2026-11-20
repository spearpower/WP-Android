package com.workspace.disciplehub.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workspace.disciplehub.WpApp
import com.workspace.disciplehub.data.ApiException
import com.workspace.disciplehub.data.Me
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data class SignedOut(val notice: String? = null) : AuthState
    data class SignedIn(val me: Me, val offline: Boolean = false) : AuthState
}

/** Decides what the app shows: the sign-in screen or the signed-in app. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val wp = app as WpApp
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state

    init {
        wp.api.onSignedOut = { _state.value = AuthState.SignedOut("Your sign-in has expired. Please sign in again.") }
        viewModelScope.launch { restore() }
    }

    private suspend fun restore() {
        if (!wp.auth.isSignedIn()) {
            _state.value = AuthState.SignedOut()
            return
        }
        // Show the saved person at once, then refresh from the server.
        wp.auth.cachedMe()?.let { _state.value = AuthState.SignedIn(it, offline = true) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _state.value = AuthState.SignedIn(wp.auth.me())
            } catch (e: ApiException) {
                when {
                    e.code == "network" -> wp.auth.cachedMe()?.let { _state.value = AuthState.SignedIn(it, offline = true) }
                        ?: run { _state.value = AuthState.SignedOut(e.message) }
                    e.http == 401 -> _state.value = AuthState.SignedOut("Your sign-in has expired. Please sign in again.")
                    else -> if (_state.value !is AuthState.SignedIn) _state.value = AuthState.SignedOut(e.message)
                }
            }
        }
    }

    fun signedIn(me: Me) {
        _state.value = AuthState.SignedIn(me)
    }

    fun logout() {
        viewModelScope.launch {
            wp.auth.logout()
            _state.value = AuthState.SignedOut()
        }
    }
}
