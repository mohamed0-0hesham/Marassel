package com.hesham0_0.marassel.ui.username.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hesham0_0.marassel.ui.theme.ChatSizes
import com.hesham0_0.marassel.ui.theme.MarasselTheme
import com.hesham0_0.marassel.ui.theme.spacing
import com.hesham0_0.marassel.ui.username.UsernameUiEffect
import com.hesham0_0.marassel.ui.username.UsernameUiEvent
import com.hesham0_0.marassel.ui.username.UsernameUiState
import com.hesham0_0.marassel.ui.username.UsernameViewModel
import com.hesham0_0.marassel.ui.username.components.AvatarPreview
import com.hesham0_0.marassel.ui.username.components.UsernameTextField
import kotlinx.coroutines.launch

@Composable
fun UsernameScreen(
    onNavigateToChatRoom: () -> Unit,
    onNavigateToAuth: () -> Unit,
    viewModel: UsernameViewModel = hiltViewModel(),
) {
    val state           by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState   = remember { SnackbarHostState() }
    val scope           = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester  = remember { FocusRequester() }

    // ── Effect handler ────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                UsernameUiEffect.NavigateToChatRoom -> {
                    keyboardController?.hide()
                    onNavigateToChatRoom()
                }
                UsernameUiEffect.NavigateToAuth -> {
                    keyboardController?.hide()
                    onNavigateToAuth()
                }
                is UsernameUiEffect.ShowSnackbar -> {
                    scope.launch {
                        snackbarState.showSnackbar(
                            message  = effect.message,
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }
        }
    }

    // ── Auto-focus text field on entry ────────────────────────────────────────
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarState) },
        modifier     = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Spacer(Modifier.height(MaterialTheme.spacing.xl))

                // ── Header ────────────────────────────────────────────────────
                Header()

                Spacer(Modifier.height(MaterialTheme.spacing.xl))

                // ── Live avatar preview ───────────────────────────────────────
                AvatarPreview(
                    username = state.username,
                    isValid  = state.isUsernameValid,
                    size     = 96.dp,
                )

                Spacer(Modifier.height(MaterialTheme.spacing.lg))

                // ── Username text field ───────────────────────────────────────
                UsernameTextField(
                    value         = state.username,
                    onValueChange = {
                        viewModel.onEvent(UsernameUiEvent.UsernameChanged(it))
                    },
                    error         = state.error,
                    charCount     = state.charCount,
                    isValid       = state.isUsernameValid,
                    onDone        = {
                        if (state.isSubmitEnabled) {
                            viewModel.onEvent(UsernameUiEvent.SubmitClicked)
                        }
                    },
                    focusRequester = focusRequester,
                )

                Spacer(Modifier.height(MaterialTheme.spacing.sm))

                // ── Reset to suggested button ─────────────────────────────────
                AnimatedVisibility(
                    visible = state.isModifiedFromSuggestion,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut(),
                ) {
                    TextButton(
                        onClick = {
                            viewModel.onEvent(UsernameUiEvent.ResetToSuggested)
                        },
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier           = Modifier
                                .size(ChatSizes.IconSmall)
                                .padding(end = 4.dp),
                        )
                        Text(
                            text  = "Reset to \"${state.suggestedName}\"",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Spacer(Modifier.height(MaterialTheme.spacing.md))

                // ── Submit button ─────────────────────────────────────────────
                JoinButton(
                    isLoading = state.isLoading,
                    isEnabled = state.isSubmitEnabled,
                    onClick   = { viewModel.onEvent(UsernameUiEvent.SubmitClicked) },
                )

                Spacer(Modifier.height(MaterialTheme.spacing.md))

                // ── Terms note ────────────────────────────────────────────────
                Text(
                    text      = "By joining, your display name will be visible\nto everyone in the chat room.",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(MaterialTheme.spacing.xl))
            }
        }
    }
}

// ── Private sub-composables ───────────────────────────────────────────────────

/**
 * Screen header with title and subtitle.
 * Kept in a separate composable to keep [UsernameScreen] scannable.
 */
@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text      = "Choose your display name",
            style     = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text      = "This is how others will see you in the chat room.\nYou can use your real name or a nickname.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Primary action button with loading state.
 *
 * When [isLoading] is true, shows a [CircularProgressIndicator] inside
 * the button instead of the label — the button remains the same size
 * so the layout doesn't shift.
 */
@Composable
private fun JoinButton(
    isLoading: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick  = onClick,
        enabled  = isEnabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape    = MaterialTheme.shapes.large,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation  = 2.dp,
            pressedElevation  = 0.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        Crossfade(
            targetState  = isLoading,
            animationSpec = tween(200),
            label        = "join_button_content",
        ) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(ChatSizes.IconMedium),
                    color     = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text  = "Join Chat",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Empty state", showBackground = true, showSystemUi = true)
@Composable
private fun UsernameScreenEmptyPreview() {
    MarasselTheme {
        UsernameScreenPreviewContent(
            state = UsernameUiState(
                username         = "",
                suggestedName    = "",
                validationResult = com.hesham0_0.marassel.domain.usecase.user
                    .UsernameValidator.ValidationResult.Blank,
            )
        )
    }
}

@Preview(
    name          = "Valid state — light",
    showBackground = true,
    showSystemUi  = true,
)
@Composable
private fun UsernameScreenValidLightPreview() {
    MarasselTheme(darkTheme = false) {
        UsernameScreenPreviewContent(
            state = UsernameUiState(
                username         = "Alice Wonder",
                suggestedName    = "Alice Wonder",
                validationResult = com.hesham0_0.marassel.domain.usecase.user
                    .UsernameValidator.ValidationResult.Valid,
            )
        )
    }
}

@Preview(
    name          = "Valid state — dark",
    showBackground = true,
    showSystemUi  = true,
)
@Composable
private fun UsernameScreenValidDarkPreview() {
    MarasselTheme(darkTheme = true) {
        UsernameScreenPreviewContent(
            state = UsernameUiState(
                username         = "Alice Wonder",
                suggestedName    = "Alice Wonder",
                validationResult = com.hesham0_0.marassel.domain.usecase.user
                    .UsernameValidator.ValidationResult.Valid,
            )
        )
    }
}

@Preview(name = "Error state", showBackground = true, showSystemUi = true)
@Composable
private fun UsernameScreenErrorPreview() {
    MarasselTheme {
        UsernameScreenPreviewContent(
            state = UsernameUiState(
                username = "Al",
                suggestedName = "Alice",
                error = "Display name must be at least 3 characters (2/3)",
                validationResult = com.hesham0_0.marassel.domain.usecase.user
                    .UsernameValidator.ValidationResult.TooShort(2),
            )
        )
    }
}

@Preview(name = "Modified — reset visible", showBackground = true, showSystemUi = true)
@Composable
private fun UsernameScreenModifiedPreview() {
    MarasselTheme {
        UsernameScreenPreviewContent(
            state = UsernameUiState(
                username         = "Bobby",
                suggestedName    = "Alice Wonder",
                validationResult = com.hesham0_0.marassel.domain.usecase.user
                    .UsernameValidator.ValidationResult.Valid,
            )
        )
    }
}

@Preview(name = "Loading state", showBackground = true, showSystemUi = true)
@Composable
private fun UsernameScreenLoadingPreview() {
    MarasselTheme {
        UsernameScreenPreviewContent(
            state = UsernameUiState(
                username         = "Alice",
                suggestedName    = "Alice",
                isLoading        = true,
                validationResult = com.hesham0_0.marassel.domain.usecase.user
                    .UsernameValidator.ValidationResult.Valid,
            )
        )
    }
}

/**
 * Stateless preview shell — renders the screen layout with a given state
 * without needing a real ViewModel. Used only in previews.
 */
@Composable
private fun UsernameScreenPreviewContent(
    state: UsernameUiState,
) {
    val snackbarState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(32.dp))
                Header()
                Spacer(Modifier.height(32.dp))
                AvatarPreview(
                    username = state.username,
                    isValid  = state.isUsernameValid,
                )
                Spacer(Modifier.height(24.dp))
                UsernameTextField(
                    value         = state.username,
                    onValueChange = {},
                    error         = state.error,
                    charCount     = state.charCount,
                    isValid       = state.isUsernameValid,
                    onDone        = {},
                )
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(visible = state.isModifiedFromSuggestion) {
                    TextButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text("Reset to \"${state.suggestedName}\"")
                    }
                }
                Spacer(Modifier.height(16.dp))
                JoinButton(
                    isLoading = state.isLoading,
                    isEnabled = state.isSubmitEnabled,
                    onClick   = {},
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text      = "By joining, your display name will be visible\nto everyone in the chat room.",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}