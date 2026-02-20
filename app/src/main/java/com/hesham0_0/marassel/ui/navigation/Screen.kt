package com.hesham0_0.marassel.ui.navigation

sealed class Screen(val route: String) {

    companion object ArgKeys {
        const val ARG_MEDIA_URL = "mediaUrl"
    }

    data object Username : Screen("username")

    data object ChatRoom : Screen("chat_room")

    data object MediaViewer : Screen("media_viewer/{$ARG_MEDIA_URL}") {
        fun createRoute(mediaUrl: String): String {
            // URL-encode the mediaUrl so slashes/special chars don't break routing
            val encoded = java.net.URLEncoder.encode(mediaUrl, "UTF-8")
            return "media_viewer/$encoded"
        }
    }
}