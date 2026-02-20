package com.hesham0_0.marassel.ui.auth

import com.hesham0_0.marassel.core.mvi.UiEffect
import com.hesham0_0.marassel.core.mvi.UiEvent
import com.hesham0_0.marassel.core.mvi.UiState

// ── State ─────────────────────────────────────────────────────────────────────

data class AuthUiState(
    val mode: AuthMode             = AuthMode.SIGN_IN,
    val email: String              = "",
    val password: String           = "",
    val confirmPassword: String    = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean         = false,
    val emailError: String?        = null,
    val passwordError: String?     = null,
    val confirmPasswordError: String? = null,
    val generalError: String?      = null,
    val isResetEmailSent: Boolean  = false,
) : UiState {
    val isSignInMode get() = mode == AuthMode.SIGN_IN
    val isSignUpMode get() = mode == AuthMode.SIGN_UP

    val isSubmitEnabled get() = !isLoading &&
            email.isNotBlank() &&
            password.isNotBlank() &&
            (isSignInMode || confirmPassword.isNotBlank())
}

enum class AuthMode { SIGN_IN, SIGN_UP, FORGOT_PASSWORD }

// ── Events ────────────────────────────────────────────────────────────────────

sealed interface AuthUiEvent : UiEvent {
    data class EmailChanged(val value: String)           : AuthUiEvent
    data class PasswordChanged(val value: String)        : AuthUiEvent
    data class ConfirmPasswordChanged(val value: String) : AuthUiEvent
    data object TogglePasswordVisibility                 : AuthUiEvent
    data object SubmitClicked                            : AuthUiEvent
    data object GoogleSignInClicked                      : AuthUiEvent
    data object ToggleAuthMode                           : AuthUiEvent
    data object ForgotPasswordClicked                    : AuthUiEvent
    data object BackFromForgotPassword                   : AuthUiEvent
    data object DismissError                             : AuthUiEvent
}

// ── Effects ───────────────────────────────────────────────────────────────────

sealed interface AuthUiEffect : UiEffect {
    /** Auth succeeded — navigate to UsernameScreen (new user) */
    data class NavigateToUsername(val suggestedName: String) : AuthUiEffect
    /** Auth succeeded — navigate directly to ChatRoom (returning user) */
    data object NavigateToChatRoom                           : AuthUiEffect
    /** Trigger Google Sign-In bottom sheet (needs Activity context) */
    data object LaunchGoogleSignIn                           : AuthUiEffect
    /** Show a snackbar message */
    data class ShowSnackbar(val message: String)             : AuthUiEffect
}