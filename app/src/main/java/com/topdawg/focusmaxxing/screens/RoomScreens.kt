package com.topdawg.focusmaxxing.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.topdawg.focusmaxxing.viewmodels.LobbyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLobbyScreen(viewModel: LobbyViewModel, onLobbyCreated: (String) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }; var isPublic by remember { mutableStateOf(true) }; val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.clearError() }
    Scaffold(topBar = { TopAppBar(title = { Text("Create Lobby") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Room details", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(name, { name = it.take(40) }, label = { Text("Room name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = state.error != null)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(if (isPublic) "Public room" else "Private room"); Text(if (isPublic) "Shown in public lobbies" else "Joinable with room code", style = MaterialTheme.typography.bodySmall) }; Switch(checked = isPublic, onCheckedChange = { isPublic = it }) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { viewModel.createRoom(name, isPublic, onLobbyCreated) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Create room") }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinLobbyScreen(viewModel: LobbyViewModel, onJoined: (String) -> Unit, onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }; val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.clearError() }
    Scaffold(topBar = { TopAppBar(title = { Text("Join Lobby") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Enter room code", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(code, { code = it.uppercase().filter { c -> c in 'A'..'Z' || c in '0'..'9' }.take(6) }, label = { Text("6-character code") }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = state.error != null)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp)); Button(onClick = { if (viewModel.validateRoomCode(code)) viewModel.joinRoom(code, onJoined) else viewModel.reportError("Enter a valid six-character code.") }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Join") }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(code: String, viewModel: LobbyViewModel, onBackHome: () -> Unit) {
    val state by viewModel.uiState.collectAsState(); val clipboard = LocalClipboardManager.current
    LaunchedEffect(code) { viewModel.observeRoom(code) }
    Scaffold(topBar = { TopAppBar(title = { Text(state.room?.name ?: "Lobby") }, navigationIcon = { IconButton(onClick = onBackHome) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        val room = state.room
        if (room == null) Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            if (state.loading) CircularProgressIndicator() else { Text(state.error ?: "Room unavailable.", color = MaterialTheme.colorScheme.error); Button(onClick = onBackHome) { Text("Back home") } }
        } else Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Room code", style = MaterialTheme.typography.labelLarge); Text(room.code, style = MaterialTheme.typography.headlineMedium) }; IconButton(onClick = { clipboard.setText(AnnotatedString(room.code)) }) { Icon(Icons.Default.ContentCopy, "Copy room code") } }
            Text("${if (room.isPublic) "Public" else "Private"} · ${room.players.size}/${room.capacity} players")
            Text("Players", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            LazyColumn(Modifier.weight(1f)) { items(room.players, key = { it.uid }) { player -> ListItem(headlineContent = { Text(player.username) }, supportingContent = { Text(player.rank) }, trailingContent = { if (player.uid == room.hostId) Text("Host", color = MaterialTheme.colorScheme.primary) }) } }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { viewModel.leaveRoom(code, onBackHome) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Leave room") }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
