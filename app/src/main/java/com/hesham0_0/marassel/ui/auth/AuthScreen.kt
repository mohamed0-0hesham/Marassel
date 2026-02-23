package com.hesham0_0.marassel.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hesham0_0.marassel.ui.theme.ChatSizes
import com.hesham0_0.marassel.ui.theme.spacing
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onNavigateToUsername: (suggestedName: String) -> Unit,
    onNavigateToChatRoom: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val googleHelper = remember { GoogleSignInHelper() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AuthUiEffect.NavigateToUsername ->
                    onNavigateToUsername(effect.suggestedName)

                AuthUiEffect.NavigateToChatRoom ->
                    onNavigateToChatRoom()

                AuthUiEffect.LaunchGoogleSignIn -> {
                    googleHelper.getGoogleIdToken(context).fold(
                        onSuccess = { token -> viewModel.handleGoogleIdToken(token) },
                        onFailure = { error ->
                            if (error.message?.contains("Cancel") == false) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Google Sign-In failed. Please try again."
                                    )
                                }
                            }
                        },
                    )
                }

                is AuthUiEffect.ShowSnackbar ->
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                Spacer(Modifier.height(MaterialTheme.spacing.xl))

                // ── Header ────────────────────────────────────────────────────
                AnimatedContent(
                    targetState = state.mode,
                    transitionSpec = {
                        (slideInVertically { -it } + fadeIn()) togetherWith
                                (slideOutVertically { it } + fadeOut())
                    },
                    label = "auth_header",
                ) { mode ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (mode) {
                                AuthMode.SIGN_IN        -> "Welcome back 👋"
                                AuthMode.SIGN_UP        -> "Create account"
                                AuthMode.FORGOT_PASSWORD -> "Reset password"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(Modifier.height(MaterialTheme.spacing.xs))
                        Text(
                            text = when (mode) {
                                AuthMode.SIGN_IN        -> "Sign in to continue chatting"
                                AuthMode.SIGN_UP        -> "Join the conversation"
                                AuthMode.FORGOT_PASSWORD ->
                                    "Enter your email and we'll send a reset link"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(MaterialTheme.spacing.md))

                // ── Reset email sent confirmation ─────────────────────────────
                AnimatedVisibility(visible = state.isResetEmailSent) {
                    Text(
                        text = "✅ Reset email sent! Check your inbox.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }

                // ── Email field ───────────────────────────────────────────────
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.onEvent(AuthUiEvent.EmailChanged(it)) },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = if (state.mode == AuthMode.FORGOT_PASSWORD)
                            ImeAction.Done else ImeAction.Next,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Password field (hidden on Forgot Password mode) ────────────
                AnimatedVisibility(visible = state.mode != AuthMode.FORGOT_PASSWORD) {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = { viewModel.onEvent(AuthUiEvent.PasswordChanged(it)) },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.onEvent(AuthUiEvent.TogglePasswordVisibility)
                                }) {
                                    Icon(
                                        imageVector = if (state.isPasswordVisible)
                                            Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = if (state.isPasswordVisible)
                                            "Hide password" else "Show password",
                                    )
                                }
                            },
                            visualTransformation = if (state.isPasswordVisible)
                                VisualTransformation.None
                            else PasswordVisualTransformation(),
                            isError = state.passwordError != null,
                            supportingText = state.passwordError?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (state.isSignUpMode)
                                    ImeAction.Next else ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { viewModel.onEvent(AuthUiEvent.SubmitClicked) },
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // ── Confirm password (Sign Up only) ───────────────────
                        AnimatedVisibility(visible = state.isSignUpMode) {
                            OutlinedTextField(
                                value = state.confirmPassword,
                                onValueChange = {
                                    viewModel.onEvent(AuthUiEvent.ConfirmPasswordChanged(it))
                                },
                                label = { Text("Confirm password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                isError = state.confirmPasswordError != null,
                                supportingText = state.confirmPasswordError?.let { { Text(it) } },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { viewModel.onEvent(AuthUiEvent.SubmitClicked) },
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // ── Forgot password link (Sign In mode only) ──────────────────
                AnimatedVisibility(visible = state.isSignInMode) {
                    TextButton(
                        onClick = { viewModel.onEvent(AuthUiEvent.ForgotPasswordClicked) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Forgot password?")
                    }
                }

                // ── General error ─────────────────────────────────────────────
                AnimatedVisibility(visible = state.generalError != null) {
                    Text(
                        text = state.generalError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(MaterialTheme.spacing.xs))

                // ── Primary submit button ─────────────────────────────────────
                Button(
                    onClick = { viewModel.onEvent(AuthUiEvent.SubmitClicked) },
                    enabled = state.isSubmitEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ChatSizes.IconMedium),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = when (state.mode) {
                                AuthMode.SIGN_IN        -> "Sign In"
                                AuthMode.SIGN_UP        -> "Create Account"
                                AuthMode.FORGOT_PASSWORD -> "Send Reset Email"
                            },
                        )
                    }
                }

                // ── Back from forgot password ─────────────────────────────────
                AnimatedVisibility(visible = state.mode == AuthMode.FORGOT_PASSWORD) {
                    TextButton(
                        onClick = { viewModel.onEvent(AuthUiEvent.BackFromForgotPassword) },
                    ) {
                        Text("← Back to Sign In")
                    }
                }

                // ── Divider (hidden on Forgot Password) ───────────────────────
                AnimatedVisibility(visible = state.mode != AuthMode.FORGOT_PASSWORD) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                }

                // ── Google Sign-In button ─────────────────────────────────────
                AnimatedVisibility(visible = state.mode != AuthMode.FORGOT_PASSWORD) {
                    OutlinedButton(
                        onClick = { viewModel.onEvent(AuthUiEvent.GoogleSignInClicked) },
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text("Continue with Google")
                    }
                }

                // ── Mode toggle ───────────────────────────────────────────────
                AnimatedVisibility(visible = state.mode != AuthMode.FORGOT_PASSWORD) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (state.isSignInMode)
                                "Don't have an account?"
                            else
                                "Already have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(
                            onClick = { viewModel.onEvent(AuthUiEvent.ToggleAuthMode) },
                        ) {
                            Text(if (state.isSignInMode) "Sign Up" else "Sign In")
                        }
                    }
                }

                Spacer(Modifier.height(MaterialTheme.spacing.xl))
            }
        }
    }
}