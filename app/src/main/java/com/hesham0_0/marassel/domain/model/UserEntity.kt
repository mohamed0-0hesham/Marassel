package com.hesham0_0.marassel.domain.model

data class UserEntity(
    val uid: String,
    val username: String,
    val email: String?,
    val photoUrl: String?,
    val initials: String,
) {
    companion object {

        /**
         * Primary factory — builds a [UserEntity] from an authenticated
         * Firebase user and the display name they chose/confirmed.
         */
        fun create(
            uid: String,
            username: String,
            email: String? = null,
            photoUrl: String? = null,
        ): UserEntity = UserEntity(
            uid      = uid,
            username = username.trim(),
            email    = email,
            photoUrl = photoUrl,
            initials = computeInitials(username.trim()),
        )

        /**
         * Computes 1–2 character uppercase initials from a display name.
         *
         * Rules (applied in order):
         * 1. Trim whitespace
         * 2. Split into words by whitespace
         * 3. Take first letter of each word, uppercase
         * 4. Limit to 2 characters
         * 5. Fallback to first 2 chars of username if result is empty
         * 6. Fallback to "?" if username is blank
         *
         * Examples:
         *   "John Doe"       → "JD"
         *   "Alice"          → "A"
         *   "mohamed jane w"    → "MJ"
         *   ""               → "?"
         */
        fun computeInitials(username: String): String {
            if (username.isBlank()) return "?"
            val words = username.trim().split(Regex("\\s+"))
            val fromWords = words
                .filter { it.isNotEmpty() }
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
            if (fromWords.isNotEmpty()) return fromWords
            return username.take(2).uppercase()
        }
    }
}