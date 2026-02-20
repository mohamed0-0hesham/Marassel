package com.hesham0_0.marassel.domain.model

/**
 * Domain representation of a Firebase authenticated user.
 *
 * This is the raw identity returned by Firebase Auth — it does NOT
 * include the chat display name the user picks on the username screen.
 * That is stored separately in DataStore as a profile on top of auth.
 *
 * @param uid           Firebase Auth UID — stable, unique per user forever
 * @param email         Email address (null for anonymous/phone auth)
 * @param displayName   Name from Google account or null for email/password
 * @param photoUrl      Profile photo URL from Google account, or null
 * @param isEmailVerified Whether the email address has been verified
 * @param provider      Which provider was used: "google.com" or "password"
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val provider: AuthProvider,
) {
    /**
     * Derives a sensible default display name from auth info.
     * Used to pre-fill the username screen:
     * - Google users: use their Google display name
     * - Email users: derive from the email prefix (before @)
     */
    val suggestedUsername: String
        get() = when {
            !displayName.isNullOrBlank() -> displayName
            !email.isNullOrBlank()       -> email.substringBefore("@")
            else                         -> ""
        }
}

/**
 * Authentication provider used to sign in.
 */
enum class AuthProvider {
    EMAIL_PASSWORD,
    GOOGLE,
}