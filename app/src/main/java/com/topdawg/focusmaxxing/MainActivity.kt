package com.topdawg.focusmaxxing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.topdawg.focusmaxxing.data.AppContainer
import com.topdawg.focusmaxxing.navigation.NavGraph
import com.topdawg.focusmaxxing.screens.SignInScreen
import com.topdawg.focusmaxxing.screens.UsernameScreen
import com.topdawg.focusmaxxing.ui.theme.FocusmaxxingTheme
import com.topdawg.focusmaxxing.viewmodels.AuthDestination
import com.topdawg.focusmaxxing.viewmodels.AuthViewModel
import com.topdawg.focusmaxxing.viewmodels.AuthViewModelFactory
import com.topdawg.focusmaxxing.viewmodels.FriendsViewModel
import com.topdawg.focusmaxxing.viewmodels.FriendsViewModelFactory
import com.topdawg.focusmaxxing.viewmodels.HomeViewModel
import com.topdawg.focusmaxxing.viewmodels.HomeViewModelFactory
import com.topdawg.focusmaxxing.viewmodels.LobbyViewModel
import com.topdawg.focusmaxxing.viewmodels.LobbyViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer(this)
        setContent {
            FocusmaxxingTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(container))
                    val authState by authViewModel.uiState.collectAsState()
                    when (authState.destination) {
                        AuthDestination.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        AuthDestination.SIGN_IN -> SignInScreen(authViewModel)
                        AuthDestination.USERNAME -> UsernameScreen(authViewModel)
                        AuthDestination.HOME -> {
                            val navController = rememberNavController()
                            val uid = authState.profile?.uid.orEmpty()
                            val homeViewModel: HomeViewModel = viewModel(key = "home_$uid", factory = HomeViewModelFactory(container))
                            val lobbyViewModel: LobbyViewModel = viewModel(key = "lobby_$uid", factory = LobbyViewModelFactory(container))
                            val friendsViewModel: FriendsViewModel = viewModel(key = "friends_$uid", factory = FriendsViewModelFactory(container))
                            NavGraph(navController, homeViewModel, lobbyViewModel, friendsViewModel, authViewModel, container.auth.isGuest)
                        }
                    }
                }
            }
        }
    }
}
