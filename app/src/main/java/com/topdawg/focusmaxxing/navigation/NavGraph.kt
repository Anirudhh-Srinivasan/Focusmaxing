package com.topdawg.focusmaxxing.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.topdawg.focusmaxxing.screens.CreateLobbyScreen
import com.topdawg.focusmaxxing.screens.HomeScreen
import com.topdawg.focusmaxxing.screens.JoinLobbyScreen
import com.topdawg.focusmaxxing.screens.LobbyJoinedScreen
import com.topdawg.focusmaxxing.screens.LobbyWaitingScreen
import com.topdawg.focusmaxxing.viewmodels.HomeViewModel
import com.topdawg.focusmaxxing.viewmodels.LobbyViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateLobby : Screen("createLobby")
    object JoinLobby : Screen("joinLobby")
    object LobbyWaiting : Screen("lobbyWaiting/{roomCode}") {
        fun createRoute(roomCode: String) = "lobbyWaiting/$roomCode"
    }
    object LobbyJoined : Screen("lobbyJoined/{roomCode}") {
        fun createRoute(roomCode: String) = "lobbyJoined/$roomCode"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    lobbyViewModel: LobbyViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToCreate = { navController.navigate(Screen.CreateLobby.route) },
                onNavigateToJoin = { navController.navigate(Screen.JoinLobby.route) }
            )
        }
        composable(Screen.CreateLobby.route) {
            CreateLobbyScreen(
                viewModel = lobbyViewModel,
                onLobbyCreated = { code ->
                    navController.navigate(Screen.LobbyWaiting.createRoute(code))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.JoinLobby.route) {
            JoinLobbyScreen(
                viewModel = lobbyViewModel,
                onJoined = { code ->
                    navController.navigate(Screen.LobbyJoined.createRoute(code))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LobbyWaiting.route) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            LobbyWaitingScreen(
                roomCode = roomCode,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LobbyJoined.route) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            LobbyJoinedScreen(
                roomCode = roomCode,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
