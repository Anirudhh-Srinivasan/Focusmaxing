package com.topdawg.focusmaxxing.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.topdawg.focusmaxxing.data.Room
import com.topdawg.focusmaxxing.data.RoomPlayer
import com.topdawg.focusmaxxing.data.UserProfile
import com.topdawg.focusmaxxing.solo.SoloScoring
import com.topdawg.focusmaxxing.ui.components.ArcadeSprites
import com.topdawg.focusmaxxing.ui.components.PixelButton
import com.topdawg.focusmaxxing.ui.components.PixelButtonKind
import com.topdawg.focusmaxxing.ui.components.PixelCard
import com.topdawg.focusmaxxing.ui.components.PixelEmptyState
import com.topdawg.focusmaxxing.ui.components.PixelLoadingIndicator
import com.topdawg.focusmaxxing.ui.components.RankBadge
import com.topdawg.focusmaxxing.ui.components.RankBadgeSize
import com.topdawg.focusmaxxing.ui.components.RankProgress
import com.topdawg.focusmaxxing.ui.components.StatChip
import com.topdawg.focusmaxxing.ui.components.bottomWindowInsets
import com.topdawg.focusmaxxing.ui.components.topWindowInsets
import com.topdawg.focusmaxxing.ui.theme.ArcadeColors
import com.topdawg.focusmaxxing.ui.theme.ArcadeDimens
import com.topdawg.focusmaxxing.ui.theme.FocusmaxxingTheme
import com.topdawg.focusmaxxing.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    isGuest: Boolean,
    roomActionLoading: Boolean,
    roomActionError: String?,
    clearRoomActionError: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToJoin: () -> Unit,
    onJoinPublic: (String) -> Unit,
    onFriends: () -> Unit,
    onRequests: () -> Unit,
    onUpgrade: () -> Unit,
    onSolo: () -> Unit,
    onSignOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    HomeUi(
        profile = state.profile,
        rooms = state.rooms,
        roomsLoading = state.roomsLoading,
        error = state.error,
        isGuest = isGuest,
        roomActionLoading = roomActionLoading,
        roomActionError = roomActionError,
        clearRoomActionError = clearRoomActionError,
        onNavigateToCreate = onNavigateToCreate,
        onNavigateToJoin = onNavigateToJoin,
        onJoinPublic = onJoinPublic,
        onSolo = onSolo,
        onFriends = onFriends,
        onRequests = onRequests,
        onUpgrade = onUpgrade,
        onSignOut = onSignOut,
        onRetryPublicRooms = { viewModel.retryPublicRooms() }
    )
}

@Composable
private fun HomeUi(
    profile: UserProfile?,
    rooms: List<Room>,
    roomsLoading: Boolean,
    error: String?,
    isGuest: Boolean,
    roomActionLoading: Boolean,
    roomActionError: String?,
    clearRoomActionError: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToJoin: () -> Unit,
    onJoinPublic: (String) -> Unit,
    onSolo: () -> Unit,
    onFriends: () -> Unit,
    onRequests: () -> Unit,
    onUpgrade: () -> Unit,
    onSignOut: () -> Unit,
    onRetryPublicRooms: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        HomeTopBar(onFriends, onRequests, onSignOut)
        // Reserve room at the end of the list so the last item can scroll clear of the
        // navigation/gesture bar (inset value read from the window, not hardcoded).
        val listBottomInset = bottomWindowInsets().asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp + listBottomInset),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Player card
            item {
                profile?.let {
                    PlayerCard(profile = it, xpProgress = SoloScoring.rankProgress(it.xp), onSolo = onSolo)
                } ?: run {
                    PixelCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(ArcadeDimens.Space4)) {
                            Text(
                                text = "Guest",
                                color = ArcadeColors.Text,
                                style = MaterialTheme.typography.displaySmall
                            )
                            Text(
                                text = "Complete your profile to play.",
                                color = ArcadeColors.TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Guest upgrade prompt
            if (isGuest) {
                item {
                    TextButton(onClick = onUpgrade) {
                        Text("Upgrade account to keep your progress", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 2. SOLO BATTLE (primary)
            item {
                PixelButton(
                    text = "SOLO BATTLE",
                    onClick = onSolo,
                    kind = PixelButtonKind.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. CREATE LOBBY + JOIN WITH CODE (secondary)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ArcadeDimens.Space2)
                ) {
                    PixelButton(
                        text = "CREATE LOBBY",
                        onClick = onNavigateToCreate,
                        kind = PixelButtonKind.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    PixelButton(
                        text = "JOIN WITH CODE",
                        onClick = onNavigateToJoin,
                        kind = PixelButtonKind.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. PUBLIC LOBBIES section
            item {
                val showSectionTitle = rooms.isNotEmpty() || roomsLoading || error != null
                if (showSectionTitle) {
                    Text(
                        text = "PUBLIC LOBBIES",
                        color = ArcadeColors.Text,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(top = ArcadeDimens.Space2)
                    )
                }
            }
            if (roomsLoading && rooms.isEmpty()) {
                item {
                    PixelLoadingIndicator(Modifier.fillMaxWidth(), contentDescription = "Loading public lobbies")
                }
            }
            if (error != null) {
                item {
                    PixelCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(ArcadeDimens.Space3),
                            horizontalArrangement = Arrangement.spacedBy(ArcadeDimens.Space2),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = ArcadeColors.Text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            PixelButton(
                                text = "RETRY",
                                onClick = onRetryPublicRooms,
                                kind = PixelButtonKind.Secondary,
                                modifier = Modifier.heightIn(min = ArcadeDimens.MinimumTouchTarget)
                            )
                        }
                    }
                }
            }
            if (rooms.isEmpty()) {
                item {
                    PixelEmptyState(
                        title = "NO LOBBIES",
                        message = "no lobbies right now, start one!",
                        sprite = ArcadeSprites.Controller,
                        action = {
                            PixelButton(
                                text = "CREATE LOBBY",
                                onClick = onNavigateToCreate,
                                kind = PixelButtonKind.Primary,
                                modifier = Modifier.heightIn(min = ArcadeDimens.MinimumTouchTarget)
                            )
                        }
                    )
                }
            }
            if (rooms.isNotEmpty()) {
                items(rooms, key = { it.code }) { room ->
                    PublicRoomCard(room, onClick = { onJoinPublic(room.code) })
                }
            }

            // Room action feedback (legacy lobby create/join)
            if (roomActionLoading) {
                item {
                    PixelLoadingIndicator(Modifier.fillMaxWidth(), contentDescription = "Loading")
                }
            }
            if (roomActionError != null) {
                item {
                    PixelCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(ArcadeDimens.Space3),
                            horizontalArrangement = Arrangement.spacedBy(ArcadeDimens.Space2),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = roomActionError,
                                color = ArcadeColors.Text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            PixelButton(
                                text = "DISMISS",
                                onClick = clearRoomActionError,
                                kind = PixelButtonKind.Secondary,
                                modifier = Modifier.heightIn(min = ArcadeDimens.MinimumTouchTarget)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    onFriends: () -> Unit,
    onRequests: () -> Unit,
    onSignOut: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Background first so it extends behind the status bar / camera cutout, then the
            // inset so the bar content starts below them.
            .background(ArcadeColors.Background)
            .windowInsetsPadding(topWindowInsets())
            .heightIn(min = 56.dp)
            .padding(horizontal = ArcadeDimens.Space3, vertical = ArcadeDimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ArcadeDimens.Space2)
    ) {
        Text(
            text = "FOCUSMAXXING",
            color = ArcadeColors.Text,
            style = MaterialTheme.typography.displaySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(ArcadeDimens.Space2)
        ) {
            PixelButton("Friends", onFriends, kind = PixelButtonKind.Secondary, modifier = Modifier.heightIn(min = ArcadeDimens.MinimumTouchTarget))
            PixelButton("Requests", onRequests, kind = PixelButtonKind.Secondary, modifier = Modifier.heightIn(min = ArcadeDimens.MinimumTouchTarget))
            PixelButton("Sign out", onSignOut, kind = PixelButtonKind.Danger, modifier = Modifier.heightIn(min = ArcadeDimens.MinimumTouchTarget))
        }
    }
}

@Composable
private fun PlayerCard(
    profile: UserProfile,
    xpProgress: com.topdawg.focusmaxxing.solo.RankProgress,
    onSolo: () -> Unit
) {
    PixelCard {
        Column(Modifier.padding(ArcadeDimens.Space4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ArcadeDimens.Space3)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = profile.username,
                        color = ArcadeColors.Text,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(ArcadeDimens.Space2)) {
                    StatChip(sprite = ArcadeSprites.Flame, value = profile.streak.toString(), label = "STR")
                    StatChip(sprite = ArcadeSprites.Bolt, value = profile.sessionsPlayed.toString(), label = "SES")
                }
            }
            Spacer(Modifier.height(ArcadeDimens.Space3))
            RankProgress(
                rank = xpProgress.rank.label,
                xp = profile.xp,
                xpToNext = xpProgress.xpToNextRank,
                progress = xpProgress.progressWithinRank,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PublicRoomCard(room: Room, onClick: () -> Unit) {
    val hostPlayer = room.players.firstOrNull { it.uid == room.hostId }
    PixelCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ArcadeDimens.Space3)
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = ArcadeDimens.Space2)) {
                Text(
                    text = room.name,
                    color = ArcadeColors.Text,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(ArcadeDimens.Space1))
                Row(horizontalArrangement = Arrangement.spacedBy(ArcadeDimens.Space2)) {
                    Text(
                        text = hostPlayer?.username ?: "Unknown",
                        color = ArcadeColors.TextMuted,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${room.playerCount}/${room.capacity} players",
                        color = ArcadeColors.TextMuted,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            RankBadge(hostPlayer?.rank ?: "Bronze", size = RankBadgeSize.Small)
        }
    }
}

@Composable
private fun PreviewFrame(content: @Composable () -> Unit) = FocusmaxxingTheme {
    Box(
        modifier = Modifier.size(360.dp, 640.dp).background(ArcadeColors.Background),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, widthDp = 360, heightDp = 640)
@Composable
private fun HomePreviewFilled() {
    PreviewFrame {
        HomeUi(
            profile = UserProfile(
                uid = "u1",
                username = "top_dawg",
                isGuest = false,
                rank = "Gold",
                xp = 3_250,
                streak = 12,
                sessionsPlayed = 42
            ),
            rooms = listOf(
                Room(
                    code = "A1B2C3",
                    name = "Alpha Lobby",
                    hostId = "u1",
                    isPublic = true,
                    capacity = 3,
                    players = listOf(RoomPlayer("u1", "top_dawg", "Gold", 1L))
                ),
                Room(
                    code = "Z9Y8X7",
                    name = "Zeta Battle Room",
                    hostId = "u2",
                    isPublic = true,
                    capacity = 3,
                    players = listOf(
                        RoomPlayer("u2", "dawg_master", "Grandmaster", 2L),
                        RoomPlayer("u1", "top_dawg", "Gold", 3L)
                    )
                )
            ),
            roomsLoading = false,
            error = null,
            isGuest = false,
            roomActionLoading = false,
            roomActionError = null,
            clearRoomActionError = {},
            onNavigateToCreate = {},
            onNavigateToJoin = {},
            onJoinPublic = {},
            onSolo = {},
            onFriends = {},
            onRequests = {},
            onUpgrade = {},
            onSignOut = {},
            onRetryPublicRooms = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, widthDp = 360, heightDp = 640)
@Composable
private fun HomePreviewEmpty() {
    PreviewFrame {
        HomeUi(
            profile = UserProfile(
                uid = "u1",
                username = "top_dawg",
                isGuest = false,
                rank = "Gold",
                xp = 3_250,
                streak = 12,
                sessionsPlayed = 42
            ),
            rooms = emptyList(),
            roomsLoading = false,
            error = null,
            isGuest = false,
            roomActionLoading = false,
            roomActionError = null,
            clearRoomActionError = {},
            onNavigateToCreate = {},
            onNavigateToJoin = {},
            onJoinPublic = {},
            onSolo = {},
            onFriends = {},
            onRequests = {},
            onUpgrade = {},
            onSignOut = {},
            onRetryPublicRooms = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, widthDp = 360, heightDp = 640)
@Composable
private fun HomePreviewLoading() {
    PreviewFrame {
        HomeUi(
            profile = UserProfile(
                uid = "u1",
                username = "top_dawg",
                isGuest = false,
                rank = "Gold",
                xp = 3_250,
                streak = 12,
                sessionsPlayed = 42
            ),
            rooms = emptyList(),
            roomsLoading = true,
            error = null,
            isGuest = false,
            roomActionLoading = false,
            roomActionError = null,
            clearRoomActionError = {},
            onNavigateToCreate = {},
            onNavigateToJoin = {},
            onJoinPublic = {},
            onSolo = {},
            onFriends = {},
            onRequests = {},
            onUpgrade = {},
            onSignOut = {},
            onRetryPublicRooms = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, widthDp = 360, heightDp = 640)
@Composable
private fun HomePreviewError() {
    PreviewFrame {
        HomeUi(
            profile = UserProfile(
                uid = "u1",
                username = "top_dawg",
                isGuest = false,
                rank = "Gold",
                xp = 3_250,
                streak = 12,
                sessionsPlayed = 42
            ),
            rooms = emptyList(),
            roomsLoading = false,
            error = "Could not load public rooms.",
            isGuest = false,
            roomActionLoading = false,
            roomActionError = null,
            clearRoomActionError = {},
            onNavigateToCreate = {},
            onNavigateToJoin = {},
            onJoinPublic = {},
            onSolo = {},
            onFriends = {},
            onRequests = {},
            onUpgrade = {},
            onSignOut = {},
            onRetryPublicRooms = {}
        )
    }
}
