package com.topdawg.focusmaxxing.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.topdawg.focusmaxxing.data.UserProfile
import com.topdawg.focusmaxxing.solo.SoloScoring
import com.topdawg.focusmaxxing.ui.components.*
import com.topdawg.focusmaxxing.ui.theme.*
import com.topdawg.focusmaxxing.viewmodels.FriendsViewModel
import com.topdawg.focusmaxxing.ui.theme.FocusmaxxingTheme

@Composable
fun FriendsScreen(viewModel: FriendsViewModel, requestsOnly: Boolean, onBack: () -> Unit, onUpgrade: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var showLeaderboard by remember { mutableStateOf(false) }
    PixelScaffold(title = if (requestsOnly) "Friend requests" else "Friends", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (viewModel.isGuest) {
                PixelCard(Modifier.fillMaxWidth().padding(top = 12.dp), accent = ArcadeColors.Accent) {
                    Text("SQUAD UP", color = ArcadeColors.Accent, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp)); Text("Create an account to add friends and see their stats.", color = ArcadeColors.TextMuted)
                    Spacer(Modifier.height(12.dp)); PixelButton("UPGRADE ACCOUNT", onUpgrade, Modifier.fillMaxWidth())
                }
            } else if (!requestsOnly) {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(if (!showLeaderboard) "FRIENDS" else "FRIENDS", { showLeaderboard = false }, Modifier.weight(1f), kind = if (!showLeaderboard) PixelButtonKind.Primary else PixelButtonKind.Secondary)
                    PixelButton("LEADERBOARD", { showLeaderboard = true }, Modifier.weight(1f), kind = if (showLeaderboard) PixelButtonKind.Primary else PixelButtonKind.Secondary)
                }
                if (showLeaderboard) {
                    val leaderboard = (listOfNotNull(state.me) + state.friends).distinctBy { it.uid }.sortedByDescending { it.xp }
                    Text("XP LEADERBOARD", Modifier.padding(vertical = 8.dp), color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium)
                    if (state.meLoading && leaderboard.isEmpty()) PixelLoadingIndicator(contentDescription = "Loading leaderboard")
                    if (!state.meLoading && state.me == null) InlineError(state.error ?: "Your stats are unavailable.")
                    if (!state.meLoading && state.me != null && state.friends.isEmpty()) Text("Add a few friends and make this leaderboard interesting.", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(leaderboard, key = { it.uid }) { profile ->
                            val place = leaderboard.indexOfFirst { it.uid == profile.uid } + 1
                            LeaderboardRow(profile, place, profile.uid == state.me?.uid) { viewModel.openProfile(profile) }
                        }
                    }
                } else {
                    PixelTextField(query, { query = it.lowercase().filter { c -> c in 'a'..'z' || c in '0'..'9' || c == '_' }.take(20) }, "SEARCH PLAYERS", placeholder = "username", singleLine = true)
                    Spacer(Modifier.height(8.dp)); PixelButton("SEARCH", { viewModel.search(query) }, Modifier.fillMaxWidth(), enabled = query.length >= 3 && !state.loading, loading = state.loading)
                    state.selectedProfile?.let { profile ->
                        PixelCard(Modifier.fillMaxWidth().padding(top = 10.dp), accent = ArcadeColors.Info) {
                            Text("PLAYER FOUND", color = ArcadeColors.Info, style = MaterialTheme.typography.labelLarge)
                            FriendIdentity(profile)
                            PixelButton("SEND FRIEND REQUEST", { viewModel.sendRequest(profile.username) }, Modifier.fillMaxWidth(), enabled = !state.loading, kind = PixelButtonKind.Secondary)
                        }
                    }
                    Text("YOUR FRIENDS", Modifier.padding(top = 18.dp, bottom = 8.dp), color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium)
                    if (!state.loading && state.friends.isEmpty()) Text("No friends yet. Search for someone by username.", color = ArcadeColors.TextMuted)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(state.friends, key = { it.uid }) { FriendRow(it) { viewModel.openProfile(it) } } }
                }
            } else {
                Text("INCOMING REQUESTS", Modifier.padding(vertical = 12.dp), color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium)
                if (!state.loading && state.requests.isEmpty()) PixelCard(Modifier.fillMaxWidth()) { Text("No incoming requests. Your inbox is clear.", color = ArcadeColors.TextMuted) }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.requests, key = { it.id }) { request ->
                        PixelCard(Modifier.fillMaxWidth()) {
                            Text("@${request.fromUsername}", color = ArcadeColors.Text, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Wants to join your squad.", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PixelButton("ACCEPT", { viewModel.respond(request.id, true) }, Modifier.weight(1f), enabled = !state.loading)
                                PixelButton("DECLINE", { viewModel.respond(request.id, false) }, Modifier.weight(1f), kind = PixelButtonKind.Secondary, enabled = !state.loading)
                            }
                        }
                    }
                }
            }
            if (state.loading && !requestsOnly && !showLeaderboard) LinearProgressIndicator(Modifier.fillMaxWidth(), color = ArcadeColors.Accent)
            state.error?.let { Spacer(Modifier.height(8.dp)); InlineError(it) }
        }
    }
    state.selectedProfile?.let { ProfileDialog(it, onDismiss = viewModel::closeProfile) }
}

@Composable private fun FriendIdentity(profile: UserProfile) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RankBadge(SoloScoring.rankForXp(profile.xp).label, size = RankBadgeSize.Small)
        Column(Modifier.weight(1f)) { Text("@${profile.username}", color = ArcadeColors.Text, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${profile.xp} XP", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable private fun FriendRow(profile: UserProfile, onClick: () -> Unit) {
    PixelCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) { FriendIdentity(profile) }
}

@Composable private fun LeaderboardRow(profile: UserProfile, place: Int, isMe: Boolean, onClick: () -> Unit) {
    val medal = when (place) { 1 -> ArcadeColors.Gold; 2 -> ArcadeColors.Silver; 3 -> ArcadeColors.Bronze; else -> null }
    PixelCard(Modifier.fillMaxWidth().clickable(onClick = onClick), accent = if (isMe) ArcadeColors.Accent else medal) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("#$place", color = medal ?: ArcadeColors.TextMuted, style = MaterialTheme.typography.titleMedium)
            RankBadge(SoloScoring.rankForXp(profile.xp).label, size = RankBadgeSize.Small)
            Column(Modifier.weight(1f)) { Text("@${profile.username}${if (isMe) " (you)" else ""}", color = if (isMe) ArcadeColors.Accent else ArcadeColors.Text, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${profile.xp} XP", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable private fun ProfileDialog(profile: UserProfile, onDismiss: () -> Unit) {
    PixelDialog(onDismissRequest = onDismiss, title = "PLAYER PROFILE", confirmButton = { PixelButton("CLOSE", onDismiss, kind = PixelButtonKind.Secondary) }) { StatsCard(profile) }
}

@Composable fun FriendProfileScreen(profile: UserProfile?, onBack: () -> Unit) {
    PixelScaffold(title = profile?.let { "@${it.username}" } ?: "Profile", onBack = onBack) { padding ->
        if (profile == null) Box(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { InlineError("Profile is unavailable.") }
        else Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { StatsCard(profile) }
    }
}

@Composable fun StatsCard(profile: UserProfile, modifier: Modifier = Modifier) {
    val rankProgress = SoloScoring.rankProgress(profile.xp)
    PixelCard(modifier.fillMaxWidth(), accent = ArcadeColors.Accent) {
        Text("PLAYER CARD", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp)); Text("@${profile.username}", color = ArcadeColors.Text, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(12.dp)); RankBadge(rankProgress.rank.label, size = RankBadgeSize.Medium)
        Text("${profile.xp} XP", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium)
        if (rankProgress.nextRank != null) {
            PixelProgressBar(rankProgress.progressWithinRank, Modifier.fillMaxWidth().padding(vertical = 8.dp), contentDescription = "Rank progress")
            Text("${rankProgress.xpToNextRank} XP to ${rankProgress.nextRank.label}", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        } else Text("TOP RANK", color = ArcadeColors.Accent, style = MaterialTheme.typography.labelLarge)
        HorizontalDivider(Modifier.padding(vertical = 14.dp), color = ArcadeColors.Border)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ProfileStat("STREAK", "${profile.streak} days"); ProfileStat("SESSIONS", "${profile.sessionsPlayed}"); ProfileStat("BATTLES", "${profile.battlesPlayed}")
        }
    }
}

@Composable private fun ProfileStat(label: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = ArcadeColors.TextMuted, style = MaterialTheme.typography.labelSmall); Text(value, color = ArcadeColors.Text, style = MaterialTheme.typography.bodyMedium) } }

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable private fun FriendsPreview() = FocusmaxxingTheme {
    PixelScaffold(title = "Friends", onBack = {}) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PixelButton("FRIENDS", {}, Modifier.weight(1f)); PixelButton("LEADERBOARD", {}, Modifier.weight(1f), kind = PixelButtonKind.Secondary) }
            PixelTextField("pixel", {}, "SEARCH PLAYERS")
            FriendRow(UserProfile("1", "top_dawg", false, xp = 2840, streak = 8, sessionsPlayed = 24, battlesPlayed = 11)) {}
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable private fun FriendRequestsPreview() = FocusmaxxingTheme {
    PixelScaffold(title = "Friend requests", onBack = {}) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) { Text("INCOMING REQUESTS", style = MaterialTheme.typography.titleMedium); PixelCard(Modifier.fillMaxWidth()) { Text("@pixelpilot"); Text("Wants to join your squad.", color = ArcadeColors.TextMuted); Row { PixelButton("ACCEPT", {}, Modifier.weight(1f)); PixelButton("DECLINE", {}, Modifier.weight(1f), kind = PixelButtonKind.Secondary) } } }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable private fun FriendEmptyPreview() = FocusmaxxingTheme { PixelScaffold(title = "Friends", onBack = {}) { padding -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { PixelTextField("", {}, "SEARCH PLAYERS", placeholder = "username"); PixelButton("SEARCH", {}, enabled = false); PixelCard(Modifier.fillMaxWidth()) { Text("NO FRIENDS YET", color = ArcadeColors.Text); Text("Search for someone by username to start your squad.", color = ArcadeColors.TextMuted) } } } }

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable private fun LeaderboardPreview() = FocusmaxxingTheme {
    PixelScaffold(title = "Friends", onBack = {}) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("XP LEADERBOARD", style = MaterialTheme.typography.titleMedium); LeaderboardRow(UserProfile("1", "pixelpilot", false, xp = 4200), 1, false) {}; LeaderboardRow(UserProfile("2", "top_dawg", false, xp = 2840), 2, true) {}; LeaderboardRow(UserProfile("3", "studybeast", false, xp = 1900), 3, false) {} }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable private fun FriendProfilePreview() = FocusmaxxingTheme { FriendProfileScreen(UserProfile("1", "top_dawg", false, xp = 2840, streak = 8, sessionsPlayed = 24, battlesPlayed = 11), {}) }
