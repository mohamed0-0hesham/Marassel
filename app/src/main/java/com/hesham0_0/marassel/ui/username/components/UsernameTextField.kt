package com.hesham0_0.marassel.ui.username.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hesham0_0.marassel.ui.theme.MarasselTheme

@Composable
fun UsernameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    charCount: String,
    isValid: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val hasInput = value.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {

        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier
                .fillMaxWidth()
                .then(
                    if (focusRequester != null)
                        Modifier.focusRequester(focusRequester)
                    else Modifier
                ),
            label = { Text("Display name") },
            placeholder = { Text("e.g. Alex, Sam, Jordan...") },
            leadingIcon = {
                Icon(
                    imageVector        = Icons.Default.Badge,
                    contentDescription = null,
                    tint               = if (isValid)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = {
                // Show check or error icon only when user has typed something
                if (hasInput) {
                    if (isValid) {
                        Icon(
                            imageVector        = Icons.Default.CheckCircle,
                            contentDescription = "Valid name",
                            tint               = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            imageVector        = Icons.Default.Error,
                            contentDescription = "Invalid name",
                            tint               = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            isError = error != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType    = KeyboardType.Text,
                imeAction       = ImeAction.Done,
                capitalization  = KeyboardCapitalization.Words,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone() },
            ),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = if (isValid && hasInput)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = if (isValid && hasInput)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.outline,
            ),
        )

        // ── Footer row: error message + character counter ──────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Error message — animated appear/disappear
            AnimatedVisibility(
                visible = error != null,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut(),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text  = error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (error == null) Spacer(Modifier.weight(1f))

            // Character counter — always visible
            Text(
                text  = charCount,
                style = MaterialTheme.typography.labelSmall,
                color = if (isValid)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UsernameTextFieldEmptyPreview() {
    MarasselTheme {
        UsernameTextField(
            value        = "",
            onValueChange = {},
            error        = null,
            charCount    = "0 / 20",
            isValid      = false,
            onDone       = {},
            modifier     = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UsernameTextFieldValidPreview() {
    MarasselTheme {
        UsernameTextField(
            value        = "Alice",
            onValueChange = {},
            error        = null,
            charCount    = "5 / 20",
            isValid      = true,
            onDone       = {},
            modifier     = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UsernameTextFieldErrorPreview() {
    MarasselTheme {
        UsernameTextField(
            value        = "Al",
            onValueChange = {},
            error        = "Display name must be at least 3 characters (2/3)",
            charCount    = "2 / 20",
            isValid      = false,
            onDone       = {},
            modifier     = Modifier.padding(16.dp),
        )
    }
}