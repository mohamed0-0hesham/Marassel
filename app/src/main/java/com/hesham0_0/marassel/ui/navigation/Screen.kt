package com.hesham0_0.marassel.ui.navigation

sealed class Screen(val route: String) {

    companion object ArgKeys {
        const val ARG_MEDIA_URL      = "mediaUrl"
        const val ARG_SUGGESTED_NAME = "suggestedName"
    }

    data object Auth       : Screen("auth")
    data object Username   : Screen("username/{$ARG_SUGGESTED_NAME}") {
        fun createRoute(suggestedName: String) =
            "username/${java.net.URLEncoder.encode(suggestedName, "UTF-8")}"
    }
    data object ChatRoom   : Screen("chat_room")
    data object MediaViewer : Screen("media_viewer/{$ARG_MEDIA_URL}") {
        fun createRoute(mediaUrl: String) =
            "media_viewer/${java.net.URLEncoder.encode(mediaUrl, "UTF-8")}"
    }
}