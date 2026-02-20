package com.hesham0_0.marassel.ui.username

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hesham0_0.marassel.core.mvi.BaseViewModel
import com.hesham0_0.marassel.domain.usecase.user.SaveUsernameResult
import com.hesham0_0.marassel.domain.usecase.user.SaveUsernameUseCase
import com.hesham0_0.marassel.domain.usecase.user.UsernameValidator
import com.hesham0_0.marassel.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class UsernameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val saveUsernameUseCase: SaveUsernameUseCase,
) : BaseViewModel<UsernameUiState, UsernameUiEvent, UsernameUiEffect>(
    initialState = buildInitialState(savedStateHandle),
) {

    @OptIn(FlowPreview::class)
    private val usernameInput = MutableStateFlow(currentState.username)
        .also { flow ->
            flow
                // Skip the very first emission so we don't show an error
                // immediately when the screen opens with a suggested name
                .drop(1)
                .debounce(VALIDATION_DEBOUNCE_MS)
                .onEach { raw ->
                    val result = UsernameValidator.validate(raw)
                    // Only show the error text after debounce —
                    // but validationResult is updated immediately in onEvent
                    // so the button state is always accurate
                    setState {
                        copy(
                            error = if (result.isValid) null
                            else UsernameValidator.toErrorMessage(result)
                        )
                    }
                }
                .launchIn(viewModelScope)
        }

    // ── Event handling ────────────────────────────────────────────────────────

    override fun onEvent(event: UsernameUiEvent) {
        when (event) {
            is UsernameUiEvent.UsernameChanged -> onUsernameChanged(event.value)
            UsernameUiEvent.SubmitClicked      -> onSubmitClicked()
            UsernameUiEvent.ResetToSuggested   -> onResetToSuggested()
            UsernameUiEvent.DismissError       -> setState { copy(error = null) }
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    /**
     * Handles every keystroke in the username text field.
     *
     * Updates immediately:
     * - [UsernameUiState.username] — text field content
     * - [UsernameUiState.validationResult] — drives button enabled state
     * - [UsernameUiState.charCount] — character counter display
     *
     * Updates after debounce (via [usernameInput]):
     * - [UsernameUiState.error] — error message text
     */
    private fun onUsernameChanged(raw: String) {
        val validationResult = UsernameValidator.validate(raw)
        setState {
            copy(
                username         = raw,
                validationResult = validationResult,
                // Clear error immediately on any change so it doesn't
                // linger while the user is correcting their input
                error            = null,
            )
        }
        // Push to debounced pipeline — will update error text after delay
        usernameInput.value = raw
    }


    private fun onSubmitClicked() {
        // Guard: re-validate synchronously in case debounce hasn't fired yet
        val result = UsernameValidator.validate(currentState.username)
        if (!result.isValid) {
            setState {
                copy(
                    validationResult = result,
                    error = UsernameValidator.toErrorMessage(result),
                )
            }
            return
        }

        setState { copy(isLoading = true, error = null) }

        launch(onError = { handleUnexpectedError(it) }) {
            when (val saveResult = saveUsernameUseCase(currentState.username)) {

                is SaveUsernameResult.Success -> {
                    setState { copy(isLoading = false) }
                    setEffect(UsernameUiEffect.NavigateToChatRoom)
                }

                is SaveUsernameResult.ValidationFailed -> {
                    // Shouldn't reach here (we validated above) but handle
                    // defensively in case use case has stricter rules
                    setState {
                        copy(
                            isLoading        = false,
                            validationResult = saveResult.reason,
                            error            = UsernameValidator.toErrorMessage(saveResult.reason),
                        )
                    }
                }

                is SaveUsernameResult.NotAuthenticated -> {
                    // Auth session expired mid-flow — redirect to auth screen
                    setState { copy(isLoading = false) }
                    setEffect(UsernameUiEffect.NavigateToAuth)
                }

                is SaveUsernameResult.StorageError -> {
                    setState { copy(isLoading = false) }
                    setEffect(
                        UsernameUiEffect.ShowSnackbar(
                            "Failed to save your name. Please try again."
                        )
                    )
                }
            }
        }
    }


    private fun onResetToSuggested() {
        val suggested = currentState.suggestedName
        val result    = UsernameValidator.validate(suggested)
        setState {
            copy(
                username         = suggested,
                validationResult = result,
                error            = null,
            )
        }
        usernameInput.value = suggested
    }


    private fun handleUnexpectedError(error: Throwable) {
        setState { copy(isLoading = false) }
        setEffect(
            UsernameUiEffect.ShowSnackbar(
                error.message ?: "Something went wrong. Please try again."
            )
        )
    }

    // ── Companion helpers ─────────────────────────────────────────────────────

    companion object {
        private const val VALIDATION_DEBOUNCE_MS = 300L

        private fun buildInitialState(handle: SavedStateHandle): UsernameUiState {
            val rawSuggested = handle.get<String>(Screen.ARG_SUGGESTED_NAME) ?: ""
            val sanitized    = UsernameValidator.sanitizeSuggestion(rawSuggested)
            val validation   = UsernameValidator.validate(sanitized)

            return UsernameUiState(
                username         = sanitized,
                suggestedName    = sanitized,
                validationResult = validation,
                error            = null, // no error shown on initial load
            )
        }
    }
}