package com.hesham0_0.marassel.domain.model

data class UserEntity(
    val username: String,
    val deviceId: String,
    val initials: String,
) {
    companion object {

        fun create(username: String, deviceId: String): UserEntity {
            val trimmed = username.trim()
            return UserEntity(
                username = trimmed,
                deviceId = deviceId,
                initials = computeInitials(trimmed),
            )
        }

        fun computeInitials(username: String): String {
            // Edge case: empty or blank username
            if (username.isBlank()) return "?"

            // Split into words by any whitespace
            val words = username.trim().split(Regex("\\s+"))

            // Take first letter of each word, uppercase, join, take first 2
            val fromWords = words
                .filter { it.isNotEmpty() }
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString(separator = "")

            // If we got something from the words approach, use it
            if (fromWords.isNotEmpty()) return fromWords

            // Fallback: take first 2 characters of the username itself
            return username.take(2).uppercase()
        }
    }
}