package com.hesham0_0.marassel.ui.navigation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hesham0_0.marassel.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StartDestinationViewModel @Inject constructor(
    userRepository: UserRepository
) : ViewModel() {

    val startDestination = userRepository.observeUser()
        .map { user ->
            if (user != null) Screen.ChatRoom.route
            else Screen.Username.route
        }
        .stateIn(
            scope = viewModelScope,
            // WhileSubscribed(5000) keeps the flow alive for 5 seconds during
            // recompositions so a config change doesn't re-trigger a DataStore read
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null  // null = still resolving
        )
}