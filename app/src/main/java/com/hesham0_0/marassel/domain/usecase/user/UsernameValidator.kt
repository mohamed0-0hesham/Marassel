package com.hesham0_0.marassel.domain.usecase.user

object UsernameValidator {

    // ── Constraints ───────────────────────────────────────────────────────────

    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 20

    private val ALLOWED_CHARS_REGEX = Regex("""^[\p{L}\p{N} \-_.']+$""")
    private val MULTIPLE_SPACES_REGEX = Regex("""\s{2,}""")

    // ── Sealed result type ────────────────────────────────────────────────────
    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data object Blank : ValidationResult()
        data class TooShort(val currentLength: Int) : ValidationResult()
        data class TooLong(val currentLength: Int) : ValidationResult()
        data class InvalidCharacters(val invalidChars: Set<Char>) : ValidationResult()
        data object ConsecutiveSpaces : ValidationResult()
        val isValid: Boolean get() = this is Valid
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Validates a raw username string and returns a [ValidationResult].
     *
     * Validation steps (applied in order — first failure wins):
     * 1. Trim leading/trailing whitespace
     * 2. Check not blank
     * 3. Check minimum length
     * 4. Check maximum length
     * 5. Check for consecutive internal spaces
     * 6. Check for disallowed characters
     *
     * @param raw The username string as typed by the user (untrimmed)
     * @return [ValidationResult.Valid] if all rules pass, specific
     *         failure subclass otherwise
     */
    fun validate(raw: String): ValidationResult {
        val trimmed = raw.trim()

        // Rule 1: not blank
        if (trimmed.isBlank()) return ValidationResult.Blank

        // Rule 2: minimum length
        if (trimmed.length < MIN_LENGTH) {
            return ValidationResult.TooShort(trimmed.length)
        }

        // Rule 3: maximum length
        if (trimmed.length > MAX_LENGTH) {
            return ValidationResult.TooLong(trimmed.length)
        }

        // Rule 4: no consecutive spaces
        if (MULTIPLE_SPACES_REGEX.containsMatchIn(trimmed)) {
            return ValidationResult.ConsecutiveSpaces
        }

        // Rule 5: allowed characters only
        if (!ALLOWED_CHARS_REGEX.matches(trimmed)) {
            val invalidChars = trimmed
                .filter { char ->
                    !char.isLetter() &&
                            !char.isDigit() &&
                            char !in setOf(' ', '-', '_', '.', '\'')
                }
                .toSet()
            return ValidationResult.InvalidCharacters(invalidChars)
        }

        return ValidationResult.Valid
    }

    fun toErrorMessage(result: ValidationResult): String? = when (result) {
        is ValidationResult.Valid            -> null
        is ValidationResult.Blank           ->
            "Display name cannot be empty"
        is ValidationResult.TooShort        ->
            "Display name must be at least $MIN_LENGTH characters " +
                    "(${result.currentLength}/$MIN_LENGTH)"
        is ValidationResult.TooLong         ->
            "Display name cannot exceed $MAX_LENGTH characters " +
                    "(${result.currentLength}/$MAX_LENGTH)"
        is ValidationResult.ConsecutiveSpaces ->
            "Display name cannot contain consecutive spaces"
        is ValidationResult.InvalidCharacters -> {
            val chars = result.invalidChars.joinToString(" ") { "'$it'" }
            "Display name contains invalid characters: $chars"
        }
    }

    fun sanitizeSuggestion(raw: String): String {
        return raw
            .trim()
            .filter { char ->
                char.isLetter() || char.isDigit() ||
                        char in setOf(' ', '-', '_', '.', '\'')
            }
            .replace(MULTIPLE_SPACES_REGEX, " ")
            .take(MAX_LENGTH)
            .trim()
    }
}