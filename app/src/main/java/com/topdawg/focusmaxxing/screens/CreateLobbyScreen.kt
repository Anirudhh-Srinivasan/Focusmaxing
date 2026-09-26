package com.topdawg.focusmaxxing.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topdawg.focusmaxxing.navigation.Screen
import com.topdawg.focusmaxxing.viewmodels.LobbyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLobbyScreen(
    viewModel: LobbyViewModel,
    onLobbyCreated: (String) -> Unit,
    onBack: () -> Unit
) {
    var roomCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        roomCode = viewModel.generateRoomCode()
        // Slight delay for visual polish, then notify
        onLobbyCreated(roomCode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Lobby") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Lobby Created",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Lobby created:",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Show the room code
            Text(
                text = roomCode,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "waiting for players...",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Back to Home")
            }
        }
    }
}
