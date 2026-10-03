package com.topdawg.focusmaxxing.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.topdawg.focusmaxxing.data.Room
import com.topdawg.focusmaxxing.data.UserProfile
import com.topdawg.focusmaxxing.viewmodels.HomeViewModel
import com.topdawg.focusmaxxing.solo.SoloScoring
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, isGuest: Boolean, roomActionLoading: Boolean, roomActionError: String?, clearRoomActionError: () -> Unit, onNavigateToCreate: () -> Unit, onNavigateToJoin: () -> Unit, onJoinPublic: (String) -> Unit, onFriends: () -> Unit, onRequests: () -> Unit, onUpgrade: () -> Unit, onSolo: () -> Unit, onSignOut: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Focusmaxxing", fontWeight = FontWeight.Bold) }, actions = {
        IconButton(onClick = onFriends) { Icon(Icons.Default.People, contentDescription = "Friends") }
        if (!isGuest) IconButton(onClick = onRequests) { Icon(Icons.Default.PersonAdd, contentDescription = "Friend requests") }
        TextButton(onClick = onSignOut) { Text("Sign out") }
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { state.profile?.let { StatsCard(it) } ?: Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { if (state.profileLoading) CircularProgressIndicator() else Text(state.error ?: "Your stats are unavailable.", color = MaterialTheme.colorScheme.error); if (!state.profileLoading) TextButton(onClick = viewModel::refreshProfile) { Text("Retry") } } } }
            if (isGuest) item { TextButton(onClick = onUpgrade) { Text("Upgrade account to keep your progress") } }
            item { Button(onClick = onSolo, modifier = Modifier.fillMaxWidth()) { Text("Solo Battle") } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onNavigateToCreate, modifier = Modifier.weight(1f)) { Text("Create Lobby") }
                OutlinedButton(onClick = onNavigateToJoin, modifier = Modifier.weight(1f)) { Text("Join with Code") }
            } }
            item { Text("Public lobbies", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
            roomActionError?.let { message -> item { Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f)); TextButton(onClick = clearRoomActionError) { Text("Dismiss") } } } } }
            if (state.roomsLoading && state.rooms.isEmpty()) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (!state.roomsLoading && state.rooms.isEmpty()) item { Text("No public lobbies right now. Create one and invite your friends.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (roomActionLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error); if (state.rooms.isEmpty()) TextButton(onClick = viewModel::retryPublicRooms) { Text("Retry") } } }
            items(state.rooms, key = { it.code }) { room -> PublicRoomCard(room, onClick = { onJoinPublic(room.code) }) }
        }
    }
}

@Composable
fun StatsCard(profile: UserProfile) {
    val rank = SoloScoring.rankForXp(profile.xp).label
    val streak = profile.lastCountedSessionDate?.let { key ->
        val last = runCatching { LocalDate.parse(key) }.getOrNull()
        if (last != null && last.isBefore(LocalDate.now().minusDays(1))) 0 else profile.streak
    } ?: profile.streak
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
        Text("Your stats", style = MaterialTheme.typography.titleLarge)
        Text("@${profile.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("DawgRank", rank); Stat("XP", profile.xp.toString())
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Stat("Streak", "$streak days"); Stat("Sessions", profile.sessionsPlayed.toString()) }
    } }
}

@Composable private fun Stat(label: String, value: String) { Column { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } }

@Composable
private fun PublicRoomCard(room: Room, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(room.name, style = MaterialTheme.typography.titleMedium); Text("${room.playerCount}/${room.capacity} players · ${room.code}", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("Join", color = MaterialTheme.colorScheme.primary) } } }
