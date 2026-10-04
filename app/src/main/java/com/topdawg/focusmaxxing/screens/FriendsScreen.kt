package com.topdawg.focusmaxxing.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.topdawg.focusmaxxing.data.UserProfile
import com.topdawg.focusmaxxing.ui.components.PixelScaffold
import com.topdawg.focusmaxxing.viewmodels.FriendsViewModel
import com.topdawg.focusmaxxing.solo.SoloScoring

@Composable
fun FriendsScreen(viewModel: FriendsViewModel, requestsOnly: Boolean, onBack: () -> Unit, onUpgrade: () -> Unit) {
    val state by viewModel.uiState.collectAsState(); var query by remember { mutableStateOf("") }; var showLeaderboard by remember { mutableStateOf(false) }
    PixelScaffold(title = if (requestsOnly) "Friend requests" else "Friends", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (viewModel.isGuest) {
                Text("Create an account to add friends and see their stats.", modifier = Modifier.padding(vertical = 18.dp))
                Button(onClick = onUpgrade) { Text("Upgrade account") }
            } else if (!requestsOnly) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showLeaderboard, onClick = { showLeaderboard = false }, label = { Text("Friends") })
                    FilterChip(selected = showLeaderboard, onClick = { showLeaderboard = true }, label = { Text("XP leaderboard") })
                }
                if (showLeaderboard) {
                    val leaderboard = (listOfNotNull(state.me) + state.friends).distinctBy { it.uid }.sortedByDescending { it.xp }
                    Text("XP leaderboard", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                    if (state.meLoading && leaderboard.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (!state.meLoading && state.me == null) Text(state.error ?: "Your stats are unavailable.", color = MaterialTheme.colorScheme.error)
                    if (!state.meLoading && state.me != null && state.friends.isEmpty()) Text("Add friends to compare XP. Your profile is shown below.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.weight(1f)) { items(leaderboard, key = { it.uid }) { profile ->
                        val place = leaderboard.indexOfFirst { it.uid == profile.uid } + 1
                        ListItem(
                            headlineContent = { Text("#$place  @${profile.username}${if (profile.uid == state.me?.uid) " (you)" else ""}") },
                            supportingContent = { Text("${SoloScoring.rankForXp(profile.xp).label} · ${profile.xp} XP") },
                            modifier = Modifier.clickable { viewModel.openProfile(profile) }
                        )
                    } }
                } else {
                OutlinedTextField(query, { query = it.lowercase().filter { c -> c in 'a'..'z' || c in '0'..'9' || c == '_' }.take(20) }, label = { Text("Search by username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.search(query) }, enabled = query.length >= 3 && !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Search") }
                state.selectedProfile?.let { profile ->
                    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) { Column(Modifier.padding(12.dp)) { Text("@${profile.username}"); OutlinedButton(onClick = { viewModel.sendRequest(profile.username) }, enabled = !state.loading) { Text("Send friend request") } } }
                }
                Text("Your friends", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                if (!state.loading && state.friends.isEmpty()) Text("No friends yet. Search for someone by username.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.weight(1f)) { items(state.friends, key = { it.uid }) { profile -> FriendRow(profile) { viewModel.openProfile(profile) } } }
                }
            } else {
                Text("Incoming requests", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
                if (!state.loading && state.requests.isEmpty()) Text("No incoming requests.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.weight(1f)) { items(state.requests, key = { it.id }) { request -> ListItem(headlineContent = { Text("@${request.fromUsername}") }, supportingContent = { Text("Wants to be your friend") }, trailingContent = { Row { TextButton(onClick = { viewModel.respond(request.id, true) }, enabled = !state.loading) { Text("Accept") }; TextButton(onClick = { viewModel.respond(request.id, false) }, enabled = !state.loading) { Text("Decline") } } }) } }
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }
        }
    }
    state.selectedProfile?.let { profile -> ProfileDialog(profile, onDismiss = viewModel::closeProfile) }
}

@Composable private fun FriendRow(profile: UserProfile, onClick: () -> Unit) { ListItem(headlineContent = { Text("@${profile.username}") }, supportingContent = { Text("${SoloScoring.rankForXp(profile.xp).label} · ${profile.xp} XP") }, modifier = Modifier.clickable(onClick = onClick)) }

@Composable private fun ProfileDialog(profile: UserProfile, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("@${profile.username}") }, text = { StatsCard(profile) }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
fun FriendProfileScreen(profile: UserProfile?, onBack: () -> Unit) {
    PixelScaffold(title = profile?.let { "@${it.username}" } ?: "Profile", onBack = onBack) { padding ->
        if (profile == null) Box(Modifier.fillMaxSize().padding(padding)) { Text("Profile is unavailable.", modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error) }
        else Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) { StatsCard(profile) }
    }
}

@Composable
fun StatsCard(profile: UserProfile, modifier: Modifier = Modifier) {
    val rankProgress = SoloScoring.rankProgress(profile.xp)
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "@${profile.username}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "${rankProgress.rank.label} · ${profile.xp} XP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { rankProgress.progressWithinRank },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${rankProgress.xpToNextRank} XP to next rank",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Streak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${profile.streak} days", style = MaterialTheme.typography.bodyLarge)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sessions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${profile.sessionsPlayed}", style = MaterialTheme.typography.bodyLarge)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Battles", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${profile.battlesPlayed}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
