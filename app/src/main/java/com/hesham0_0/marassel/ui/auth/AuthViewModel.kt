package com.hesham0_0.marassel.ui.auth

import com.hesham0_0.marassel.core.mvi.BaseViewModel
import com.hesham0_0.marassel.domain.repository.AuthRepository
import com.hesham0_0.marassel.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * MVI ViewModel for the Authentication screen.
 *
 * Responsibilities:
 * 1. Handles email/password sign-in + sign-up with field-level validation
 * 2. Delegates Google Sign-In credential retrieval to the UI (via effect)
 *    then exchanges the ID token with Firebase Auth
 * 3. After successful auth checks whether the user has a chat profile
 *    to decide whether to route to UsernameScreen or directly to ChatRoom
 *
 * Why does routing logic live here?
 * ───────────────────────────────────
 * The ViewModel knows both the auth result ([AuthRepository]) AND whether
 * a profile exists ([UserRepository]). Keeping this logic here avoids
 * putting business decisions in the NavGraph or the UI layer.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : BaseViewModel<AuthUiState, AuthUiEvent, AuthUiEffect>(AuthUiState()) {

    override fun onEvent(event: AuthUiEvent) {
        when (event) {
            // ── Field updates ─────────────────────────────────────────────────
            is AuthUiEvent.EmailChanged ->
                setState { copy(email = event.value, emailError = null, generalError = null) }

            is AuthUiEvent.PasswordChanged ->
                setState { copy(password = event.value, passwordError = null, generalError = null) }

            is AuthUiEvent.ConfirmPasswordChanged ->
                setState { copy(confirmPassword = event.value, confirmPasswordError = null) }

            AuthUiEvent.TogglePasswordVisibility ->
                setState { copy(isPasswordVisible = !isPasswordVisible) }

            // ── Mode switching ────────────────────────────────────────────────
            AuthUiEvent.ToggleAuthMode -> setState {
                copy(
                    mode = if (mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN,
                    emailError = null, passwordError = null,
                    confirmPasswordError = null, generalError = null,
                    password = "", confirmPassword = "",
                )
            }

            AuthUiEvent.ForgotPasswordClicked ->
                setState { copy(mode = AuthMode.FORGOT_PASSWORD, generalError = null) }

            AuthUiEvent.BackFromForgotPassword ->
                setState { copy(mode = AuthMode.SIGN_IN, isResetEmailSent = false) }

            // ── Submit ────────────────────────────────────────────────────────
            AuthUiEvent.SubmitClicked -> handleSubmit()

            // ── Google ────────────────────────────────────────────────────────
            AuthUiEvent.GoogleSignInClicked ->
                setEffect(AuthUiEffect.LaunchGoogleSignIn)

            // ── Dismiss error ─────────────────────────────────────────────────
            AuthUiEvent.DismissError ->
                setState { copy(generalError = null) }
        }
    }

    // ── Submit routing ────────────────────────────────────────────────────────

    private fun handleSubmit() {
        when (currentState.mode) {
            AuthMode.SIGN_IN        -> signIn()
            AuthMode.SIGN_UP        -> signUp()
            AuthMode.FORGOT_PASSWORD -> sendPasswordReset()
        }
    }

    // ── Email / Password ──────────────────────────────────────────────────────

    private fun signIn() {
        if (!validateEmailAndPassword()) return
        setState { copy(isLoading = true, generalError = null) }
        launch(onError = { handleAuthError(it) }) {
            authRepository.signInWithEmail(
                email    = currentState.email.trim(),
                password = currentState.password,
            ).fold(
                onSuccess = { authUser -> handleAuthSuccess(authUser.uid, authUser.suggestedUsername) },
                onFailure = { handleAuthError(it) },
            )
        }
    }

    private fun signUp() {
        if (!validateEmailAndPassword()) return
        if (!validateConfirmPassword()) return
        setState { copy(isLoading = true, generalError = null) }
        launch(onError = { handleAuthError(it) }) {
            authRepository.signUpWithEmail(
                email    = currentState.email.trim(),
                password = currentState.password,
            ).fold(
                onSuccess = { authUser -> handleAuthSuccess(authUser.uid, authUser.suggestedUsername) },
                onFailure = { handleAuthError(it) },
            )
        }
    }

    private fun sendPasswordReset() {
        if (currentState.email.isBlank()) {
            setState { copy(emailError = "Enter your email to reset your password") }
            return
        }
        setState { copy(isLoading = true) }
        launch(onError = { handleAuthError(it) }) {
            authRepository.sendPasswordResetEmail(currentState.email.trim())
                .fold(
                    onSuccess = { setState { copy(isLoading = false, isResetEmailSent = true) } },
                    onFailure = { handleAuthError(it) },
                )
        }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────

    /**
     * Called from the UI after Credential Manager returns an ID token.
     * The UI is responsible for launching the Credential Manager bottom
     * sheet (via the [AuthUiEffect.LaunchGoogleSignIn] effect) and passing
     * the resulting token back here.
     */
    fun handleGoogleIdToken(idToken: String) {
        setState { copy(isLoading = true, generalError = null) }
        launch(onError = { handleAuthError(it) }) {
            authRepository.signInWithGoogle(idToken).fold(
                onSuccess = { authUser -> handleAuthSuccess(authUser.uid, authUser.suggestedUsername) },
                onFailure = { handleAuthError(it) },
            )
        }
    }

    // ── Post-auth routing ─────────────────────────────────────────────────────

    /**
     * Called after any successful authentication.
     * Checks whether this user already has a chat profile to decide
     * whether to go to UsernameScreen or straight to ChatRoom.
     */
    private suspend fun handleAuthSuccess(uid: String, suggestedName: String) {
        setState { copy(isLoading = false) }
        val hasProfile = userRepository.hasProfile(uid)
        if (hasProfile) {
            setEffect(AuthUiEffect.NavigateToChatRoom)
        } else {
            setEffect(AuthUiEffect.NavigateToUsername(suggestedName))
        }
    }

    // ── Error handling ────────────────────────────────────────────────────────

    private fun handleAuthError(error: Throwable) {
        setState { copy(isLoading = false) }
        val message = when {
            error.message?.contains("email address is already in use") == true ->
                "This email is already registered. Try signing in instead."
            error.message?.contains("password is invalid") == true ||
                    error.message?.contains("INVALID_PASSWORD") == true ->
                "Incorrect password. Please try again."
            error.message?.contains("no user record") == true ||
                    error.message?.contains("USER_NOT_FOUND") == true ->
                "No account found with this email."
            error.message?.contains("badly formatted") == true ->
                "Please enter a valid email address."
            error.message?.contains("weak-password") == true ->
                "Password must be at least 6 characters."
            error.message?.contains("network error") == true ->
                "Network error. Check your connection and try again."
            error.message?.contains("too-many-requests") == true ->
                "Too many attempts. Please try again later."
            else -> error.message ?: "Authentication failed. Please try again."
        }
        setState { copy(generalError = message) }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun validateEmailAndPassword(): Boolean {
        var valid = true
        if (currentState.email.isBlank()) {
            setState { copy(emailError = "Email is required") }
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(currentState.email.trim()).matches()) {
            setState { copy(emailError = "Enter a valid email address") }
            valid = false
        }
        if (currentState.password.isBlank()) {
            setState { copy(passwordError = "Password is required") }
            valid = false
        } else if (currentState.password.length < 6) {
            setState { copy(passwordError = "Password must be at least 6 characters") }
            valid = false
        }
        return valid
    }

    private fun validateConfirmPassword(): Boolean {
        if (currentState.password != currentState.confirmPassword) {
            setState { copy(confirmPasswordError = "Passwords do not match") }
            return false
        }
        return true
    }
}