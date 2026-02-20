package com.hesham0_0.marassel.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hesham0_0.marassel.ui.auth.AuthScreen
import com.hesham0_0.marassel.ui.navigation.Screen.ArgKeys.ARG_MEDIA_URL
import com.hesham0_0.marassel.ui.navigation.Screen.ArgKeys.ARG_SUGGESTED_NAME
import com.hesham0_0.marassel.ui.username.ui.UsernameScreen

private const val TRANSITION_DURATION_MS = 350

@Composable
fun ChatNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val startViewModel: StartDestinationViewModel = hiltViewModel()
    val startDestination by startViewModel.startDestination.collectAsStateWithLifecycle()
    val resolvedStart = startDestination ?: return

    NavHost(
        navController    = navController,
        startDestination = resolvedStart,
        modifier         = modifier,
        enterTransition  = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(TRANSITION_DURATION_MS),
            ) + fadeIn(tween(TRANSITION_DURATION_MS))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(TRANSITION_DURATION_MS),
            ) + fadeOut(tween(TRANSITION_DURATION_MS))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(TRANSITION_DURATION_MS),
            ) + fadeIn(tween(TRANSITION_DURATION_MS))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(TRANSITION_DURATION_MS),
            ) + fadeOut(tween(TRANSITION_DURATION_MS))
        },
    ) {

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(
            route = Screen.Auth.route,
            enterTransition = { fadeIn(tween(TRANSITION_DURATION_MS)) },
            exitTransition  = { fadeOut(tween(TRANSITION_DURATION_MS)) },
        ) {
            AuthScreen(
                onNavigateToUsername = { suggestedName ->
                    navController.navigateToUsername(suggestedName)
                },
                onNavigateToChatRoom = {
                    navController.navigateToChatRoom()
                },
            )
        }

        // ── Username ──────────────────────────────────────────────────────────
        composable(
            route = Screen.Username.route,
            arguments = listOf(
                navArgument(ARG_SUGGESTED_NAME) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString(ARG_SUGGESTED_NAME) ?: ""
            val suggestedName = java.net.URLDecoder.decode(encoded, "UTF-8")
            // Full implementation in CHAT-015
            UsernameScreen(
                onNavigateToChatRoom = { navController.navigateToChatRoom() },
                onNavigateToAuth     = { navController.navigateToAuth() },
            )
        }

        // ── Chat Room ─────────────────────────────────────────────────────────
        composable(route = Screen.ChatRoom.route) {
            ChatRoomScreenStub(
                onNavigateToMediaViewer = { navController.navigateToMediaViewer(it) },
            )
        }

        // ── Media Viewer ──────────────────────────────────────────────────────
        composable(
            route = Screen.MediaViewer.route,
            arguments = listOf(
                navArgument(ARG_MEDIA_URL) { type = NavType.StringType },
            ),
            enterTransition = { fadeIn(tween(TRANSITION_DURATION_MS)) },
            exitTransition  = { fadeOut(tween(TRANSITION_DURATION_MS)) },
        ) { backStackEntry ->
            val encoded  = backStackEntry.arguments?.getString(ARG_MEDIA_URL) ?: return@composable
            val mediaUrl = java.net.URLDecoder.decode(encoded, "UTF-8")
            MediaViewerScreenStub(mediaUrl = mediaUrl, onBack = { navController.popBackStack() })
        }
    }
}