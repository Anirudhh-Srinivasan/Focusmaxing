package com.topdawg.focusmaxxing.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topdawg.focusmaxxing.ui.components.*
import com.topdawg.focusmaxxing.ui.theme.*
import com.topdawg.focusmaxxing.viewmodels.LobbyViewModel

@Composable
fun CreateLobbyScreen(viewModel: LobbyViewModel, onLobbyCreated: (String) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.clearError() }
    PixelScaffold(title = "Create lobby", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(ArcadeDimens.Space4).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(ArcadeDimens.Space4)) {
            Text("SET THE STAGE", color = ArcadeColors.Accent, style = MaterialTheme.typography.headlineSmall)
            PixelCard(Modifier.fillMaxWidth(), accent = ArcadeColors.Accent) {
                Text("Room details", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                PixelTextField(name, { name = it.take(40) }, "ROOM NAME", placeholder = "late night lock-in", supportingMessage = "Give your squad a name.", singleLine = true, enabled = !state.loading, errorMessage = state.error)
                Spacer(Modifier.height(16.dp))
                Text("VISIBILITY", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().clip(PixelShape()).background(ArcadeColors.SurfaceHigh).border(2.dp, ArcadeColors.Border, PixelShape()).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisibilityChoice("PUBLIC", "Anyone can find it", isPublic, Modifier.weight(1f)) { isPublic = true }
                    VisibilityChoice("PRIVATE", "Code required", !isPublic, Modifier.weight(1f)) { isPublic = false }
                }
            }
            state.error?.let { InlineError(it) }
            PixelButton("CREATE ROOM", { viewModel.createRoom(name, isPublic, onLobbyCreated) }, Modifier.fillMaxWidth(), loading = state.loading)
        }
    }
}

@Composable private fun VisibilityChoice(title: String, detail: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = PixelShape(steps = 1)
    Column(modifier.clip(shape).background(if (selected) ArcadeColors.AccentDim else ArcadeColors.Surface).border(2.dp, if (selected) ArcadeColors.Accent else ArcadeColors.Border, shape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = if (selected) ArcadeColors.Accent else ArcadeColors.Text, style = MaterialTheme.typography.labelLarge)
        Text(detail, color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
    }
}

@Composable
fun JoinLobbyScreen(viewModel: LobbyViewModel, onJoined: (String) -> Unit, onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.clearError() }
    PixelScaffold(title = "Join lobby", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(ArcadeDimens.Space4), verticalArrangement = Arrangement.Center) {
            PixelCard(Modifier.fillMaxWidth(), accent = ArcadeColors.Accent) {
                Text("GOT A CODE?", color = ArcadeColors.Accent, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Drop the code. Squad's waiting.", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(20.dp))
                PixelTextField(code, { code = it.uppercase().filter { c -> c in 'A'..'Z' || c in '0'..'9' }.take(6) }, "6-CHARACTER ROOM CODE", placeholder = "ABC123", supportingMessage = "Ask the host for their room code.", errorMessage = state.error, enabled = !state.loading, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
                Spacer(Modifier.height(16.dp))
                PixelButton("JOIN ROOM", { if (viewModel.validateRoomCode(code)) viewModel.joinRoom(code, onJoined) else viewModel.reportError("Enter a valid six-character code.") }, Modifier.fillMaxWidth(), loading = state.loading)
            }
        }
    }
}

@Composable
fun LobbyScreen(code: String, viewModel: LobbyViewModel, onBackHome: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(code) { viewModel.observeRoom(code) }
    PixelScaffold(title = state.room?.name ?: "Waiting room", onBack = onBackHome) { padding ->
        val room = state.room
        if (room == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PixelLoadingIndicator(contentDescription = "Loading room")
                if (!state.loading) { InlineError(state.error ?: "Room unavailable."); PixelButton("BACK HOME", onBackHome, Modifier.fillMaxWidth(), kind = PixelButtonKind.Secondary) }
            }
        } else Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)) {
            PixelCard(Modifier.fillMaxWidth(), accent = ArcadeColors.Accent) {
                Text("ROOM CODE", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(room.code, Modifier.weight(1f), color = ArcadeColors.Accent, fontFamily = PixelHeadingFont, fontSize = 28.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { clipboard.setText(AnnotatedString(room.code)) }, modifier = Modifier.semantics { contentDescription = "Copy room code" }) { Icon(Icons.Default.ContentCopy, null, tint = ArcadeColors.Accent) }
                }
                Text("${if (room.isPublic) "PUBLIC" else "PRIVATE"}  ·  ${room.players.size}/${room.capacity} players", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("THE SQUAD", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium)
                Text("${room.players.size} READY", color = ArcadeColors.Accent, style = MaterialTheme.typography.labelLarge)
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(room.players, key = { it.uid }) { player ->
                    PixelCard(Modifier.fillMaxWidth(), accent = if (player.uid == room.hostId) ArcadeColors.Gold else null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RankBadge(player.rank, size = RankBadgeSize.Small)
                            Text(player.username, Modifier.weight(1f), color = ArcadeColors.Text, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (player.uid == room.hostId) Icon(Icons.Default.EmojiEvents, contentDescription = "Room host", tint = ArcadeColors.Gold)
                        }
                    }
                }
                items((room.capacity - room.players.size).coerceAtLeast(0)) { index ->
                    Box(Modifier.fillMaxWidth().heightIn(min = 68.dp).clip(PixelShape()).border(BorderStroke(2.dp, ArcadeColors.Border), PixelShape()).padding(14.dp), contentAlignment = Alignment.CenterStart) {
                        Text("WAITING FOR PLAYER ${index + 1}...", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            state.error?.let { InlineError(it); Spacer(Modifier.height(8.dp)) }
            PixelButton("LEAVE ROOM", { viewModel.leaveRoom(code, onBackHome) }, Modifier.fillMaxWidth(), kind = PixelButtonKind.Secondary, loading = state.loading)
        }
    }
}

@Composable internal fun InlineError(message: String) = PixelCard(Modifier.fillMaxWidth(), accent = ArcadeColors.Danger, containerColor = ArcadeColors.SurfaceHigh) { Text(message, color = ArcadeColors.Danger, style = MaterialTheme.typography.bodyMedium) }
