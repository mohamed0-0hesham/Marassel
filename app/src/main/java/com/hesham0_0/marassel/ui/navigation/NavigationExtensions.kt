package com.hesham0_0.marassel.ui.navigation

import androidx.navigation.NavController

fun NavController.navigateToChatRoom() {
    navigate(Screen.ChatRoom.route) {
        popUpTo(graph.startDestinationId) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

fun NavController.navigateToMediaViewer(mediaUrl: String) {
    navigate(Screen.MediaViewer.createRoute(mediaUrl)) {
        launchSingleTop = true
    }
}

fun NavController.backToChatRoom() {
    popBackStack(Screen.ChatRoom.route, inclusive = false)
}