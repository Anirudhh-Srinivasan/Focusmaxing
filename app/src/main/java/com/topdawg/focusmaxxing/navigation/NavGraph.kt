package com.topdawg.focusmaxxing.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.topdawg.focusmaxxing.screens.CreateLobbyScreen
import com.topdawg.focusmaxxing.screens.FriendsScreen
import com.topdawg.focusmaxxing.screens.HomeScreen
import com.topdawg.focusmaxxing.screens.JoinLobbyScreen
import com.topdawg.focusmaxxing.screens.LobbyScreen
import com.topdawg.focusmaxxing.screens.UpgradeAccountDialog
import com.topdawg.focusmaxxing.viewmodels.AuthViewModel
import com.topdawg.focusmaxxing.viewmodels.FriendsViewModel
import com.topdawg.focusmaxxing.viewmodels.HomeViewModel
import com.topdawg.focusmaxxing.viewmodels.LobbyViewModel
import com.topdawg.focusmaxxing.solo.SoloViewModel
import com.topdawg.focusmaxxing.screens.SoloStudyScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object CreateLobby : Screen("createLobby")
    data object JoinLobby : Screen("joinLobby")
    data object Lobby : Screen("lobby/{roomCode}") { fun createRoute(code: String) = "lobby/$code" }
    data object Friends : Screen("friends")
    data object Requests : Screen("requests")
    data object Solo : Screen("solo")
}

@Composable
fun NavGraph(navController: NavHostController, homeViewModel: HomeViewModel, lobbyViewModel: LobbyViewModel, friendsViewModel: FriendsViewModel, authViewModel: AuthViewModel, soloViewModel: SoloViewModel, isGuest: Boolean) {
    var showUpgrade by remember { mutableStateOf(false) }
    val lobbyState by lobbyViewModel.uiState.collectAsState()
    Box(Modifier.fillMaxSize()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(homeViewModel, isGuest, lobbyState.loading, lobbyState.error, lobbyViewModel::clearError,
                onNavigateToCreate = { navController.navigate(Screen.CreateLobby.route) },
                onNavigateToJoin = { navController.navigate(Screen.JoinLobby.route) },
                onJoinPublic = { code -> lobbyViewModel.joinRoom(code) { navController.navigate(Screen.Lobby.createRoute(it)) } },
                onFriends = { navController.navigate(Screen.Friends.route) },
                onRequests = { navController.navigate(Screen.Requests.route) },
                onUpgrade = { showUpgrade = true },
                onSolo = { soloViewModel.openSolo(); navController.navigate(Screen.Solo.route) },
                onSignOut = { authViewModel.signOut() })
        }
        composable(Screen.CreateLobby.route) { CreateLobbyScreen(lobbyViewModel, { navController.navigate(Screen.Lobby.createRoute(it)) }, { navController.popBackStack() }) }
        composable(Screen.JoinLobby.route) { JoinLobbyScreen(lobbyViewModel, { navController.navigate(Screen.Lobby.createRoute(it)) }, { navController.popBackStack() }) }
        composable(Screen.Lobby.route, arguments = listOf(navArgument("roomCode") { type = NavType.StringType })) { entry ->
            LobbyScreen(entry.arguments?.getString("roomCode").orEmpty(), lobbyViewModel) { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = false }; launchSingleTop = true } }
        }
        composable(Screen.Friends.route) { FriendsScreen(friendsViewModel, false, { navController.popBackStack() }, { showUpgrade = true }) }
        composable(Screen.Requests.route) { FriendsScreen(friendsViewModel, true, { navController.popBackStack() }, { showUpgrade = true }) }
        composable(Screen.Solo.route) {
            SoloStudyScreen(soloViewModel, onBack = { navController.popBackStack() })
        }
    }
        if (showUpgrade) UpgradeAccountDialog(authViewModel) { showUpgrade = false }
    }
}
