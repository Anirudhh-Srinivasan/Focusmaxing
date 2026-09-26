package com.topdawg.focusmaxxing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.topdawg.focusmaxxing.navigation.NavGraph
import com.topdawg.focusmaxxing.ui.theme.FocusmaxxingTheme
import com.topdawg.focusmaxxing.viewmodels.HomeViewModel
import com.topdawg.focusmaxxing.viewmodels.LobbyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusmaxxingTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val homeViewModel: HomeViewModel = viewModel()
                    val lobbyViewModel: LobbyViewModel = viewModel()

                    MaterialTheme {
                        NavGraph(
                            navController = navController,
                            homeViewModel = homeViewModel,
                            lobbyViewModel = lobbyViewModel
                        )
                    }
                }
            }
        }
    }
}
