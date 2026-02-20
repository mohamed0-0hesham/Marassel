package com.hesham0_0.marassel.domain.usecase.message

object MessageValidator {

    const val MAX_TEXT_LENGTH = 2000

    const val MAX_MEDIA_BYTES = 25 * 1024 * 1024L // 25 MB

    val ALLOWED_IMAGE_TYPES = setOf(
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp",
    )

    val ALLOWED_VIDEO_TYPES = setOf(
        "video/mp4",
        "video/3gpp",
        "video/quicktime",
        "video/webm",
    )

    sealed class ValidationResult {

        data object Valid : ValidationResult()

        /** Text is empty or contains only whitespace */
        data object BlankText : ValidationResult()

        /**
         * Text exceeds [MAX_TEXT_LENGTH] characters.
         * @param length Actual trimmed character count
         */
        data class TextTooLong(val length: Int) : ValidationResult()

        /**
         * Media file is too large.
         * @param bytes Actual file size in bytes
         */
        data class MediaTooLarge(val bytes: Long) : ValidationResult()

        /**
         * MIME type is not in the allowed set.
         * @param mimeType The disallowed type that was provided
         */
        data class UnsupportedMediaType(val mimeType: String) : ValidationResult()

        /** Media file size is zero — the file is empty or unreadable */
        data object EmptyMediaFile : ValidationResult()

        val isValid: Boolean get() = this is Valid
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Validates raw text message content typed by the user.
     *
     * Rules (first failure wins):
     * 1. Trim whitespace — result must be non-blank
     * 2. Trimmed length must not exceed [MAX_TEXT_LENGTH]
     *
     * @param raw The text as typed, including leading/trailing whitespace
     */
    fun validateText(raw: String): ValidationResult {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ValidationResult.BlankText
        if (trimmed.length > MAX_TEXT_LENGTH) {
            return ValidationResult.TextTooLong(trimmed.length)
        }
        return ValidationResult.Valid
    }

    /**
     * Validates a media file before it is attached to a message.
     *
     * Rules (first failure wins):
     * 1. File size must be > 0 bytes
     * 2. File size must not exceed [MAX_MEDIA_BYTES]
     * 3. MIME type must be in [ALLOWED_IMAGE_TYPES] or [ALLOWED_VIDEO_TYPES]
     *
     * @param mimeType The MIME type reported by the OS media picker
     * @param fileSizeBytes Size of the selected file in bytes
     */
    fun validateMedia(mimeType: String, fileSizeBytes: Long): ValidationResult {
        if (fileSizeBytes == 0L) return ValidationResult.EmptyMediaFile
        if (fileSizeBytes > MAX_MEDIA_BYTES) {
            return ValidationResult.MediaTooLarge(fileSizeBytes)
        }
        val allowed = ALLOWED_IMAGE_TYPES + ALLOWED_VIDEO_TYPES
        if (mimeType !in allowed) {
            return ValidationResult.UnsupportedMediaType(mimeType)
        }
        return ValidationResult.Valid
    }

    /**
     * Converts a [ValidationResult] to a human-readable error string
     * for display in the chat input area or a snackbar.
     *
     * Returns null for [ValidationResult.Valid].
     */
    fun toErrorMessage(result: ValidationResult): String? = when (result) {
        is ValidationResult.Valid              -> null
        is ValidationResult.BlankText          ->
            "Message cannot be empty"
        is ValidationResult.TextTooLong        ->
            "Message is too long (${result.length}/$MAX_TEXT_LENGTH characters)"
        is ValidationResult.MediaTooLarge      -> {
            val mb = result.bytes / (1024 * 1024)
            "File is too large (${mb}MB). Maximum size is ${MAX_MEDIA_BYTES / (1024 * 1024)}MB"
        }
        is ValidationResult.UnsupportedMediaType ->
            "Unsupported file type: ${result.mimeType}"
        is ValidationResult.EmptyMediaFile     ->
            "The selected file appears to be empty"
    }
}