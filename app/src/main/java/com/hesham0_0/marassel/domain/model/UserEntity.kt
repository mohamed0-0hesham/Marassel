package com.hesham0_0.marassel.domain.model

data class UserEntity(
    val uid: String,
    val username: String,
    val email: String?,
    val photoUrl: String?,
    val initials: String,
) {
    companion object {
        fun create(
            uid: String,
            username: String,
            email: String? = null,
            photoUrl: String? = null,
        ): UserEntity = UserEntity(
            uid = uid,
            username = username.trim(),
            email = email,
            photoUrl = photoUrl,
            initials = computeInitials(username.trim()),
        )

        /**
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