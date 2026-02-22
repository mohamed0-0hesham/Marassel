package com.hesham0_0.marassel.domain.usecase.user

object UsernameValidator {

    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 20

    private val ALLOWED_CHARS_REGEX = Regex("""^[\p{L}\p{N} \-_.']+$""")
    private val MULTIPLE_SPACES_REGEX = Regex("""\s{2,}""")

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data object Blank : ValidationResult()
        data class TooShort(val currentLength: Int) : ValidationResult()
        data class TooLong(val currentLength: Int) : ValidationResult()
        data class InvalidCharacters(val invalidChars: Set<Char>) : ValidationResult()
        data object ConsecutiveSpaces : ValidationResult()
        val isValid: Boolean get() = this is Valid
    }

    fun validate(raw: String): ValidationResult {
        val trimmed = raw.trim()

        if (trimmed.isBlank()) return ValidationResult.Blank

        if (trimmed.length < MIN_LENGTH) {
            return ValidationResult.TooShort(trimmed.length)
        }

        if (trimmed.length > MAX_LENGTH) {
            return ValidationResult.TooLong(trimmed.length)
        }

        if (MULTIPLE_SPACES_REGEX.containsMatchIn(trimmed)) {
            return ValidationResult.ConsecutiveSpaces
        }

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