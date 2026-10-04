package com.topdawg.focusmaxxing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.topdawg.focusmaxxing.solo.SoloViewModel
import com.topdawg.focusmaxxing.viewmodels.SoloViewModelFactory
import com.topdawg.focusmaxxing.solo.SoloCheckpointAlarm

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer
    private var soloViewModel: SoloViewModel? = null
    private var checkpointLaunch by mutableStateOf(false)
    companion object { const val EXTRA_OPEN_CHECKPOINT = "open_solo_checkpoint" }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkpointLaunch = intent.getBooleanExtra(EXTRA_OPEN_CHECKPOINT, false)
        container = AppContainer(this)
        setContent {
            FocusmaxxingTheme {
                // Paint the app background behind the transparent system bars and keep all
                // screens clear of a side display cutout / side navigation bar (landscape).
                // Top and bottom insets are applied by the top bars and each screen itself.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
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
                                val solo: SoloViewModel = viewModel(key = "solo_$uid", factory = SoloViewModelFactory(container))
                                soloViewModel = solo
                                NavGraph(navController, homeViewModel, lobbyViewModel, friendsViewModel, authViewModel, solo, container.auth.isGuest, checkpointLaunch) {
                                    checkpointLaunch = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        SoloCheckpointAlarm.setAppForeground(false)
        soloViewModel?.onAppBackgrounded()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        SoloCheckpointAlarm.setAppForeground(true)
        soloViewModel?.onAppForegrounded()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_CHECKPOINT, false)) checkpointLaunch = true
    }
}
