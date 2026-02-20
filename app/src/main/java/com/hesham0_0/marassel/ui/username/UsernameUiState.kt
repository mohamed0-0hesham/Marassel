package com.hesham0_0.marassel.ui.username

import com.hesham0_0.marassel.core.mvi.UiEffect
import com.hesham0_0.marassel.core.mvi.UiEvent
import com.hesham0_0.marassel.core.mvi.UiState
import com.hesham0_0.marassel.domain.usecase.user.UsernameValidator

data class UsernameUiState(
    val username: String                                    = "",
    val suggestedName: String                               = "",
    val error: String?                                      = null,
    val isLoading: Boolean                                  = false,
    val validationResult: UsernameValidator.ValidationResult =
        UsernameValidator.ValidationResult.Blank,
) : UiState {

    /**
     * True when the username passes all validation rules.
     * Drives the enabled/disabled state of the submit button.
     */
    val isUsernameValid: Boolean
        get() = validationResult.isValid

    /**
     * Submit button is enabled only when input is valid and not loading.
     */
    val isSubmitEnabled: Boolean
        get() = isUsernameValid && !isLoading

    /**
     * Character count display: "12 / 20"
     * Shown below the text field to guide the user.
     */
    val charCount: String
        get() = "${username.trim().length} / ${UsernameValidator.MAX_LENGTH}"

    /**
     * True if the username has been modified from its initial suggested value.
     * Used to decide whether to show a "Reset to suggested" option.
     */
    val isModifiedFromSuggestion: Boolean
        get() = username.trim() != suggestedName.trim() && suggestedName.isNotBlank()
}

// ── Events ────────────────────────────────────────────────────────────────────

sealed interface UsernameUiEvent : UiEvent {
    /** User typed in the text field */
    data class UsernameChanged(val value: String)   : UsernameUiEvent
    /** User tapped the confirm / join button */
    data object SubmitClicked                       : UsernameUiEvent
    /** User tapped "reset to suggested" option */
    data object ResetToSuggested                    : UsernameUiEvent
    /** User dismissed the error snackbar or cleared the error */
    data object DismissError                        : UsernameUiEvent
}

// ── Effects ───────────────────────────────────────────────────────────────────

sealed interface UsernameUiEffect : UiEffect {
    /** Profile saved — go to chat room */
    data object NavigateToChatRoom                          : UsernameUiEffect
    /** Storage failure — not authentication, so we stay on screen */
    data class ShowSnackbar(val message: String)            : UsernameUiEffect
    /** Auth session lost mid-screen — redirect to auth */
    data object NavigateToAuth                              : UsernameUiEffect
}