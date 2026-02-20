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
import com.hesham0_0.marassel.ui.navigation.Screen.ArgKeys.ARG_MEDIA_URL

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
        navController = navController,
        startDestination = resolvedStart,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(TRANSITION_DURATION_MS)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(TRANSITION_DURATION_MS)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(TRANSITION_DURATION_MS)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(TRANSITION_DURATION_MS)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
        },
    ) {

        composable(
            route = Screen.Username.route,
            enterTransition = {
                fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
            },
        ) {
            UsernameScreenStub(
                onNavigateToChatRoom = { navController.navigateToChatRoom() }
            )
        }

        composable(
            route = Screen.ChatRoom.route,
            enterTransition = {
                if (initialState.destination.route == Screen.Username.route) {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(TRANSITION_DURATION_MS)
                    ) + fadeIn(tween(TRANSITION_DURATION_MS))
                } else {
                    fadeIn(tween(TRANSITION_DURATION_MS))
                }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
            },
        ) {
            ChatRoomScreenStub(
                onNavigateToMediaViewer = { url ->
                    navController.navigateToMediaViewer(url)
                }
            )
        }

        composable(
            route = Screen.MediaViewer.route,
            arguments = listOf(
                navArgument(ARG_MEDIA_URL) {
                    type = NavType.StringType
                    nullable = false
                }
            ),
            enterTransition = {
                fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
            },
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString(ARG_MEDIA_URL) ?: return@composable
            val mediaUrl = java.net.URLDecoder.decode(encodedUrl, "UTF-8")

            // Stub — full implementation in CHAT-048
            MediaViewerScreenStub(
                mediaUrl = mediaUrl,
                onBack = { navController.popBackStack() }
            )
        }
    }
}