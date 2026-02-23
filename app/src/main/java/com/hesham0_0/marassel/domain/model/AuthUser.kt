package com.hesham0_0.marassel.domain.model

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val provider: AuthProvider,
) {

    val suggestedUsername: String
        get() = when {
            !displayName.isNullOrBlank() -> displayName
            !email.isNullOrBlank() -> email.substringBefore("@")
            else -> ""
        }
}

enum class AuthProvider {
    EMAIL_PASSWORD,
    GOOGLE,
}