package com.topdawg.focusmaxxing.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.topdawg.focusmaxxing.data.UserProfile
import com.topdawg.focusmaxxing.viewmodels.FriendsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(viewModel: FriendsViewModel, requestsOnly: Boolean, onBack: () -> Unit, onUpgrade: () -> Unit) {
    val state by viewModel.uiState.collectAsState(); var query by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text(if (requestsOnly) "Friend requests" else "Friends") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (viewModel.isGuest) {
                Text("Create an account to add friends and see their stats.", modifier = Modifier.padding(vertical = 18.dp))
                Button(onClick = onUpgrade) { Text("Upgrade account") }
            } else if (!requestsOnly) {
                OutlinedTextField(query, { query = it.lowercase().filter { c -> c in 'a'..'z' || c in '0'..'9' || c == '_' }.take(20) }, label = { Text("Search by username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.search(query) }, enabled = query.length >= 3 && !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Search") }
                state.selectedProfile?.let { profile ->
                    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) { Column(Modifier.padding(12.dp)) { Text("@${profile.username}"); OutlinedButton(onClick = { viewModel.sendRequest(profile.username) }, enabled = !state.loading) { Text("Send friend request") } } }
                }
                Text("Your friends", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                if (!state.loading && state.friends.isEmpty()) Text("No friends yet. Search for someone by username.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn { items(state.friends, key = { it.uid }) { profile -> FriendRow(profile) { viewModel.openProfile(profile) } } }
            } else {
                Text("Incoming requests", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
                if (!state.loading && state.requests.isEmpty()) Text("No incoming requests.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn { items(state.requests, key = { it.id }) { request -> ListItem(headlineContent = { Text("@${request.fromUsername}") }, supportingContent = { Text("Wants to be your friend") }, trailingContent = { Row { TextButton(onClick = { viewModel.respond(request.id, true) }, enabled = !state.loading) { Text("Accept") }; TextButton(onClick = { viewModel.respond(request.id, false) }, enabled = !state.loading) { Text("Decline") } } }) } }
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }
        }
    }
    state.selectedProfile?.let { profile -> ProfileDialog(profile, onDismiss = viewModel::closeProfile) }
}

@Composable private fun FriendRow(profile: UserProfile, onClick: () -> Unit) { ListItem(headlineContent = { Text("@${profile.username}") }, supportingContent = { Text("${profile.rank} · ${profile.rp} RP") }, modifier = Modifier.clickable(onClick = onClick)) }

@Composable private fun ProfileDialog(profile: UserProfile, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("@${profile.username}") }, text = { StatsCard(profile) }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendProfileScreen(profile: UserProfile?, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(profile?.let { "@${it.username}" } ?: "Profile") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        if (profile == null) Box(Modifier.fillMaxSize().padding(padding)) { Text("Profile is unavailable.", modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error) }
        else Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) { StatsCard(profile) }
    }
}
