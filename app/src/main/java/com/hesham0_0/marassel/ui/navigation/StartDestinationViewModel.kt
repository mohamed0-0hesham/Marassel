package com.hesham0_0.marassel.ui.navigation

import com.hesham0_0.marassel.domain.repository.AuthRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StartDestinationViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    /**
     * Emits:
     * - null                   → still resolving (show nothing)
     * - Screen.Auth.route      → not signed in
     * - Screen.ChatRoom.route  → signed in and has profile
     *
     * Note: New users who are signed in but have no profile are routed
     * to ChatRoom here — AuthViewModel handles the UsernameScreen redirect
     * after sign-in. This keeps the start destination simple: only two
     * states matter on cold launch (signed in vs not).
     */
    val startDestination = authRepository
        .observeAuthState()
        .map { user ->
            if (user != null) Screen.ChatRoom.route
            else Screen.Auth.route
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}