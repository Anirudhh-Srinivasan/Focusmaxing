package com.topdawg.focusmaxxing.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topdawg.focusmaxxing.BuildConfig
import com.topdawg.focusmaxxing.solo.*
import com.topdawg.focusmaxxing.solo.SoloUiState
import com.topdawg.focusmaxxing.ui.components.*
import com.topdawg.focusmaxxing.ui.theme.*
import java.io.File
import java.util.LinkedHashMap
import kotlin.math.roundToInt
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Composable
fun SoloStudyScreen(viewModel: SoloViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var confirmLeave by remember { mutableStateOf(false) }
    BackHandler(enabled = state.activeSession != null) { confirmLeave = true }
    var topic by remember { mutableStateOf("") }
    var duration by remember { mutableIntStateOf(25) }
    var startPage by remember(state.selectedMaterial?.localId) { mutableStateOf(state.selectedMaterial?.lastReadPage?.toString() ?: "1") }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importMaterial) }
    var notificationsUnavailable by remember { mutableStateOf(android.os.Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> notificationsUnavailable = !granted }
    val keepScreenOn = state.phase == SoloPhase.READER
    DisposableEffect(keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (keepScreenOn) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    when (state.phase) {
        SoloPhase.SETUP -> SetupScreen(viewModel, state, topic, { topic = it }, duration, { duration = it }, startPage, { startPage = it }, picker::launch, onBack, context, notificationPermission::launch, notificationsUnavailable)
        SoloPhase.COUNTDOWN -> CountdownScreen(onFinished = viewModel::beginSession)
        SoloPhase.READER -> {
            val active = state.activeSession
            if (active == null) ErrorSoloScreen("No active session found.", onBack)
            else ReaderScreen(active.material, state.remainingSeconds, active.currentPage, viewModel::updateCurrentPage, { confirmLeave = true }, state.error, state.loading)
        }
        SoloPhase.CHECKPOINT -> {
            val active = state.activeSession
            if (active == null) ErrorSoloScreen("The checkpoint session is unavailable.", onBack)
            else CheckpointScreen(active.material.pageCount, active.segmentFromPage.coerceIn(1, active.material.pageCount), active.currentPage.coerceIn(1, active.material.pageCount), active.checkpointIsFinal, active.checkpointActiveElapsedMs?.let { (it - active.segmentStartActiveMs).coerceAtLeast(0) / 1000L } ?: 0L, state.loading, state.error, viewModel::submitCheckpoint, { confirmLeave = true })
        }
        SoloPhase.SEGMENT_RESULT -> SegmentResultScreen(state.lastSegment, state.isFakeAi, viewModel::continueAfterSegment)
        SoloPhase.RESULTS -> SessionResultsScreen(state.completedSession, viewModel::runItBack, onBack)
    }
    if (confirmLeave) PixelDialog(onDismissRequest = { confirmLeave = false }, title = "LEAVE THIS SESSION?", confirmButton = { PixelButton("LEAVE", { confirmLeave = false; viewModel.leaveSession(onBack) }, kind = PixelButtonKind.Danger) }, dismissButton = { PixelButton("KEEP GOING", { confirmLeave = false }, kind = PixelButtonKind.Secondary) }) {
        Text("Your scored checkpoints are saved. The current unfinished segment will be lost.", color = ArcadeColors.Text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun SetupScreen(viewModel: SoloViewModel, state: SoloUiState, topic: String, onTopic: (String) -> Unit, duration: Int, onDuration: (Int) -> Unit, startPage: String, onStartPage: (String) -> Unit, pick: (Array<String>) -> Unit, onBack: () -> Unit, context: android.content.Context, requestPermission: (String) -> Unit, notificationsUnavailable: Boolean) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(safeWindowInsets()).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← HOME", color = ArcadeColors.TextMuted) }
        Text("SOLO BATTLE", color = ArcadeColors.Accent, style = MaterialTheme.typography.headlineSmall)
        PixelCard(Modifier.fillMaxWidth(), accent = ArcadeColors.Accent) {
            Text("MISSION", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(12.dp))
            PixelTextField(topic, onTopic, "TOPIC", placeholder = "What are you learning?", supportingMessage = "Pick one thing to lock in on.", singleLine = true)
        }
        PixelCard(Modifier.fillMaxWidth()) {
            Text("STUDY MATERIAL", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
            if (state.materials.isEmpty()) Text("Pick a PDF or TXT file to load your material.", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
            state.materials.forEach { material ->
                val selected = state.selectedMaterial?.localId == material.localId
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioButton(selected, { viewModel.selectMaterial(material) }, colors = RadioButtonDefaults.colors(selectedColor = ArcadeColors.Accent, unselectedColor = ArcadeColors.TextMuted))
                    Column(Modifier.weight(1f)) { Text(material.displayName, color = ArcadeColors.Text, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${material.pageCount} pages", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall) }
                }
            }
            PixelButton("PICK PDF OR TXT", { pick(arrayOf("application/pdf", "text/plain")) }, Modifier.fillMaxWidth(), kind = PixelButtonKind.Secondary)
        }
        PixelCard(Modifier.fillMaxWidth()) {
            Text("SESSION LENGTH", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoloConstants.SESSION_LENGTHS_MINUTES.forEach { minutes ->
                    DurationChip(if (BuildConfig.DEBUG && minutes == 2) "2 MIN · DEBUG" else "$minutes MIN", duration == minutes) { onDuration(minutes) }
                }
                }
            }
            Spacer(Modifier.height(10.dp))
            PixelTextField(startPage, { onStartPage(it.filter(Char::isDigit)) }, "START PAGE", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        }
        if (state.isFakeAi) PixelCard(Modifier.fillMaxWidth(), accent = ArcadeColors.Info) { Text("DEV MODE · recall scores are based on summary length.", color = ArcadeColors.Info, style = MaterialTheme.typography.bodySmall) }
        if (notificationsUnavailable) Text("Notifications are off. Checkpoints appear while Focusmaxxing is open.", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        state.error?.let { InlineError(it) }
        PixelButton("START SESSION", {
            if (android.os.Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            viewModel.prepareSession(topic, duration, startPage.toIntOrNull() ?: -1)
        }, Modifier.fillMaxWidth(), loading = state.loading)
        if (state.activeSession != null) PixelButton("RESUME ACTIVE SESSION", viewModel::openSolo, Modifier.fillMaxWidth(), kind = PixelButtonKind.Secondary)
        PixelCard(Modifier.fillMaxWidth()) {
            Text("RECENT RUNS", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium)
            if (state.historyLoading && state.recentSessions.isEmpty()) PixelLoadingIndicator(contentDescription = "Loading recent sessions")
            if (!state.historyLoading && state.recentSessions.isEmpty()) Text("Completed study sessions will show here.", color = ArcadeColors.TextMuted)
            state.historyError?.let { Text(it, color = ArcadeColors.Danger) }
            if (state.historyError != null) PixelButton("RETRY HISTORY", viewModel::retryHistory, kind = PixelButtonKind.Secondary)
            state.recentSessions.forEach { session ->
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = ArcadeColors.Border)
                Text(session.topic, color = ArcadeColors.Text, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${session.materialName} · ${java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(session.endedAtMs))}", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${session.totalPoints} XP · ${session.totalWordsCovered} words${if (session.abandoned) " · left early" else ""}", color = ArcadeColors.Text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable private fun DurationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, style = MaterialTheme.typography.labelLarge) }, colors = FilterChipDefaults.filterChipColors(containerColor = ArcadeColors.SurfaceHigh, labelColor = ArcadeColors.TextMuted, selectedContainerColor = ArcadeColors.Accent, selectedLabelColor = ArcadeColors.Background), border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = ArcadeColors.Border, selectedBorderColor = ArcadeColors.Accent))
}

@Composable private fun CountdownScreen(onFinished: () -> Unit) {
    var seconds by remember { mutableIntStateOf(SoloConstants.COUNTDOWN_SECONDS) }
    LaunchedEffect(Unit) { while (seconds > 1) { kotlinx.coroutines.delay(1_000); seconds-- }; kotlinx.coroutines.delay(1_000); onFinished() }
    Box(Modifier.fillMaxSize().windowInsetsPadding(safeWindowInsets()), contentAlignment = Alignment.Center) { Text(seconds.toString(), color = ArcadeColors.Accent, style = MaterialTheme.typography.displayLarge) }
}

@Composable private fun ReaderScreen(material: SoloMaterial, remaining: Long, currentPage: Int, onCurrentPage: (Int) -> Unit, onLeaveRequested: () -> Unit, error: String?, leaving: Boolean) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentPage - 1).coerceIn(0, material.pageCount - 1))
    val textResult by produceState<Result<List<String>>>(Result.success(emptyList()), material.localId) { value = if (material.isPdf) Result.success(emptyList()) else runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { SoloMaterialFiles.splitTxtIntoPages(File(material.localPath).readText(Charsets.UTF_8)) } } }
    val textPages = textResult.getOrNull().orEmpty()
    LaunchedEffect(listState) {
        snapshotFlow { val info = listState.layoutInfo; info.visibleItemsInfo.maxByOrNull { item -> (minOf(item.offset + item.size, info.viewportEndOffset) - maxOf(item.offset, info.viewportStartOffset)).coerceAtLeast(0) }?.index }.collect { it?.let { index -> onCurrentPage((index + 1).coerceIn(1, material.pageCount)) } }
    }
    val timeFraction = (remaining.toFloat() / (SoloConstants.MAX_SESSION_MINUTES * 60f)).coerceIn(0f, 1f)
    Column(Modifier.fillMaxSize().windowInsetsPadding(safeWindowInsets()).background(ArcadeColors.Background)) {
        PixelProgressBar(timeFraction, Modifier.fillMaxWidth().height(3.dp), segments = 32, color = ArcadeColors.AccentDim, contentDescription = "Study timer")
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium, fontFamily = PixelHeadingFont)
            Text("$currentPage / ${material.pageCount}", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onLeaveRequested, enabled = !leaving) { Text(if (leaving) "Saving…" else "Leave", color = ArcadeColors.TextMuted) }
        }
        error?.takeIf(String::isNotBlank)?.let { Text(it, color = ArcadeColors.Danger, modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall) }
        if (material.isPdf) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(material.pageCount) { index -> PdfPage(material.localPath, index, Modifier.fillMaxWidth()) } }
        } else if (textResult.isFailure) {
            Column(Modifier.padding(20.dp)) { Text("Could not read this text material from private storage.", color = ArcadeColors.Danger); TextButton(onClick = onLeaveRequested) { Text("Leave session") } }
        } else LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
            itemsIndexed(textPages) { index, page ->
                Text("PAGE ${index + 1}", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.labelMedium)
                Text(page, Modifier.padding(vertical = 16.dp), color = Color(0xFFF0EEE8), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 30.sp))
                HorizontalDivider(Modifier.padding(vertical = 18.dp), color = ArcadeColors.Border)
            }
        }
    }
}

@Composable private fun PdfPage(path: String, pageIndex: Int, modifier: Modifier = Modifier) {
    var retry by remember { mutableIntStateOf(0) }
    val result by produceState<Result<Bitmap?>?>(null, path, pageIndex, retry) { value = runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { PdfBitmapCache.render(path, pageIndex) } } }
    val bitmap = result?.getOrNull()
    if (bitmap != null) Image(bitmap.asImageBitmap(), "PDF page ${pageIndex + 1}", modifier = modifier.heightIn(min = 400.dp))
    else if (result?.isFailure == true) Column(modifier.padding(16.dp)) { Text("Could not render page ${pageIndex + 1}.", color = ArcadeColors.Danger); TextButton(onClick = { retry++ }) { Text("Retry") } }
    else Box(modifier.height(480.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ArcadeColors.TextMuted) }
}

private object PdfBitmapCache {
    private val cache = object : LinkedHashMap<String, Bitmap>(16, .75f, true) {
        private var bytes = 0
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean { if (bytes > SoloConstants.PDF_BITMAP_CACHE_BYTES) { eldest?.value?.let { bytes -= it.allocationByteCount; it.recycle() }; return true }; return false }
        override fun put(key: String, value: Bitmap): Bitmap? { bytes += value.allocationByteCount; return super.put(key, value) }
    }
    @Synchronized fun render(path: String, page: Int): Bitmap? {
        val key = "$path#$page"; cache[key]?.let { return it }
        val descriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        val bitmap = descriptor.use { pfd -> PdfRenderer(pfd).use { renderer -> if (page !in 0 until renderer.pageCount) return null; renderer.openPage(page).use { pdfPage -> val width = 1400; val height = (width * pdfPage.height.toFloat() / pdfPage.width).roundToInt().coerceAtLeast(1); Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { pdfPage.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) } } } }
        cache[key] = bitmap; return bitmap
    }
}

@Composable private fun CheckpointScreen(pageCount: Int, defaultFrom: Int, defaultTo: Int, isFinal: Boolean, activeSeconds: Long, loading: Boolean, error: String?, onSubmit: (Int, Int, String, Boolean) -> Unit, onLeave: () -> Unit) {
    var from by remember(defaultFrom) { mutableStateOf(defaultFrom.toString()) }
    var to by remember(defaultTo) { mutableStateOf(defaultTo.toString()) }
    var summary by remember { mutableStateOf("") }
    var nothing by remember { mutableStateOf(false) }
    val wordCount = remember(summary) { Regex("\\b[\\w'-]+\\b").findAll(summary).count() }
    val validWords = wordCount in SoloConstants.MIN_RECALL_WORDS..SoloConstants.MAX_RECALL_WORDS
    Column(Modifier.fillMaxSize().windowInsetsPadding(safeWindowInsets()).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CHECKPOINT", color = ArcadeColors.Accent, style = MaterialTheme.typography.headlineSmall)
        Text(if (isFinal) "Final recall check" else "Lock in what stuck.", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
        PixelCard(Modifier.fillMaxWidth(), accent = ArcadeColors.Info) { Text("Scores are estimates. Your timer is paused while you check in.", color = ArcadeColors.Text, style = MaterialTheme.typography.bodySmall) }
        PixelCard(Modifier.fillMaxWidth()) {
            Text("PAGES COVERED", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PixelTextField(from, { from = it.filter(Char::isDigit) }, "FROM", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                PixelTextField(to, { to = it.filter(Char::isDigit) }, "TO", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
            Text("Pages 1–$pageCount · up to ${SoloConstants.MAX_PAGES_PER_CHECKPOINT} pages", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(nothing, { nothing = it }, colors = CheckboxDefaults.colors(checkedColor = ArcadeColors.Accent, uncheckedColor = ArcadeColors.TextMuted)); Text("I didn't cover anything", color = ArcadeColors.Text) }
        }
        PixelCard(Modifier.fillMaxWidth(), accent = if (validWords) ArcadeColors.Accent else null) {
            Text("QUICK RECALL", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
            PixelTextField(summary, { value -> if (Regex("\\b[\\w'-]+\\b").findAll(value).count() <= SoloConstants.MAX_RECALL_WORDS) summary = value }, "WHAT DID YOU LEARN?", placeholder = "Write 2–4 sentences…", supportingMessage = "15–150 words", enabled = !nothing, minLines = 4)
            Spacer(Modifier.height(8.dp)); PixelProgressBar((wordCount / SoloConstants.MAX_RECALL_WORDS.toFloat()).coerceIn(0f, 1f), Modifier.fillMaxWidth().height(10.dp), segments = 15, color = if (validWords) ArcadeColors.Accent else ArcadeColors.Info, contentDescription = "Recall word count $wordCount of ${SoloConstants.MAX_RECALL_WORDS}")
            Text("$wordCount / ${SoloConstants.MAX_RECALL_WORDS} words${if (validWords) " · good to go" else " · 15 min / 150 max"}", color = if (validWords) ArcadeColors.Accent else ArcadeColors.TextMuted, style = MaterialTheme.typography.labelMedium)
        }
            Text("Study time: ${activeSeconds / 60} min ${activeSeconds % 60} sec", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        error?.takeIf(String::isNotBlank)?.let { InlineError(it) }
        PixelButton(if (isFinal) "FINISH SESSION" else "SUBMIT CHECKPOINT", { onSubmit(from.toIntOrNull() ?: -1, to.toIntOrNull() ?: -1, summary, nothing) }, Modifier.fillMaxWidth(), loading = loading)
        PixelButton("LEAVE SESSION", onLeave, Modifier.fillMaxWidth(), kind = PixelButtonKind.Secondary, enabled = !loading)
    }
}

@Composable private fun SegmentResultScreen(segment: SoloSegmentRecord?, fake: Boolean, onContinue: () -> Unit) {
    if (segment == null) return ErrorSoloScreen("The segment result is unavailable.", onContinue)
    Column(Modifier.fillMaxSize().windowInsetsPadding(safeWindowInsets()).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SEGMENT CLEAR", color = ArcadeColors.Accent, style = MaterialTheme.typography.headlineSmall)
        Text("Scores are estimates${if (fake) " · DEV MODE" else ""}", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        PixelCard(Modifier.fillMaxWidth(), accent = ArcadeColors.Accent) {
            Text(if (segment.didNotCoverAnything) "NO PAGES COVERED" else "PAGES ${segment.fromPage}–${segment.toPage}", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium)
            Text("${segment.wordsCovered} words · ${segment.activeSeconds / 60} min · ${segment.paceWpm} wpm · ${segment.recallScore}% recall", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            var displayed by remember(segment.points) { mutableIntStateOf(0) }
            LaunchedEffect(segment.points) { for (value in 1..segment.points.coerceAtMost(100)) { displayed = value; kotlinx.coroutines.delay(18) }; displayed = segment.points }
            Text("$displayed XP", color = ArcadeColors.Accent, fontFamily = PixelHeadingFont, fontSize = 22.sp, modifier = Modifier.padding(vertical = 8.dp))
            SegmentCreditLine(segment)
            if (segment.points == 0) SoloScoring.zeroPointsReason(segment.recallScore, SoloScoring.pagesReflected(segment.fromPage, segment.toPage, segment.pagesReflected).size)?.let { Text(it, color = ArcadeColors.Warning, style = MaterialTheme.typography.bodySmall) }
            Text(segment.feedback, color = ArcadeColors.Text, style = MaterialTheme.typography.bodyMedium)
            if (segment.keyPointsMissed.isNotEmpty()) { Text("KEY POINTS TO REVISIT", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.labelLarge); segment.keyPointsMissed.forEach { Text("• $it", color = ArcadeColors.Text, style = MaterialTheme.typography.bodySmall) } }
        }
        Spacer(Modifier.weight(1f)); PixelButton("BACK TO READING", onContinue, Modifier.fillMaxWidth())
    }
}

@Composable private fun SessionResultsScreen(session: SoloSessionRecord?, onRunItBack: () -> Unit, onBack: () -> Unit) {
    if (session == null) return ErrorSoloScreen("The saved session result is unavailable.", onBack)
    val xpBefore = (session.xpAfter - session.totalPoints).coerceAtLeast(0)
    val oldRank = SoloScoring.rankForXp(xpBefore)
    val progress = SoloScoring.rankProgress(session.xpAfter)
    var displayedPoints by remember(session.sessionId) { mutableIntStateOf(0) }
    var showRankUp by remember(session.sessionId) { mutableStateOf(oldRank != progress.rank) }
    val animatedProgress by animateFloatAsState(progress.progressWithinRank, label = "rank-progress")
    LaunchedEffect(session.sessionId) { if (session.totalPoints <= 120) { for (value in 0..session.totalPoints) { displayedPoints = value; kotlinx.coroutines.delay(10) } } else { displayedPoints = session.totalPoints }; displayedPoints = session.totalPoints }
    LazyColumn(Modifier.fillMaxSize().windowInsetsPadding(safeWindowInsets()).padding(16.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("SESSION COMPLETE", color = ArcadeColors.Accent, style = MaterialTheme.typography.headlineSmall) }
        item { Text("$displayedPoints XP earned · ${session.totalWordsCovered} words · ${session.interruptions} interruptions", color = ArcadeColors.Text, style = MaterialTheme.typography.bodyMedium) }
        item {
            PixelCard(Modifier.fillMaxWidth(), accent = ArcadeColors.Accent) {
                RankBadge(progress.rank.label, size = RankBadgeSize.Medium)
                Text("${session.xpAfter} XP", color = ArcadeColors.Accent, fontFamily = PixelHeadingFont, fontSize = 20.sp)
                if (progress.nextRank != null) { PixelProgressBar(animatedProgress, Modifier.fillMaxWidth().padding(vertical = 8.dp), contentDescription = "Progress to ${progress.nextRank.label}"); Text("${progress.xpToNextRank} XP to ${progress.nextRank.label}", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall) } else Text("TOP RANK", color = ArcadeColors.Accent)
                Text("Scores are estimates.", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item { Text("SEGMENTS", color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium) }
        itemsIndexed(session.segments) { index, segment ->
            PixelCard(Modifier.fillMaxWidth()) {
                Text("SEGMENT ${index + 1} · ${if (segment.didNotCoverAnything) "NO PAGES" else "PAGES ${segment.fromPage}–${segment.toPage}"}", color = ArcadeColors.Text, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp)); Text("${segment.wordsCovered} words · ${segment.activeSeconds / 60} min · ${segment.recallScore}% recall · ${segment.points} XP", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                SegmentCreditLine(segment)
                if (segment.points == 0) SoloScoring.zeroPointsReason(segment.recallScore, SoloScoring.pagesReflected(segment.fromPage, segment.toPage, segment.pagesReflected).size)?.let { Text(it, color = ArcadeColors.Warning, style = MaterialTheme.typography.bodySmall) }
                Text(segment.feedback, color = ArcadeColors.Text, style = MaterialTheme.typography.bodySmall)
            }
        }
        item { PixelButton("RUN IT BACK", onRunItBack, Modifier.fillMaxWidth()) }
        item { PixelButton("BACK TO HOME", onBack, Modifier.fillMaxWidth(), kind = PixelButtonKind.Secondary) }
    }
    if (showRankUp) PixelDialog(onDismissRequest = { showRankUp = false }, title = "RANK UP!", confirmButton = { PixelButton("LET'S GO", { showRankUp = false }) }) {
        Text("You crossed into ${progress.rank.label}.", color = ArcadeColors.Text, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp)); RankBadge(progress.rank.label, Modifier.fillMaxWidth(), RankBadgeSize.Large)
    }
}

@Composable private fun SegmentCreditLine(segment: SoloSegmentRecord) {
    val pages = SoloScoring.pagesReflected(segment.fromPage, segment.toPage, segment.pagesReflected)
    val total = SoloScoring.creditedWords(segment.pageWordCounts, segment.fromPage, segment.toPage, (segment.fromPage..segment.toPage).toList())
    val credited = SoloScoring.creditedWords(segment.pageWordCounts, segment.fromPage, segment.toPage, pages)
    Text("Pages credited: ${pages.size} of ${segment.fromPage}–${segment.toPage} ($credited of $total words)", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
}

@Composable private fun ErrorSoloScreen(message: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(safeWindowInsets()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { InlineError(message); PixelButton("BACK", onBack, Modifier.fillMaxWidth(), kind = PixelButtonKind.Secondary) }
}
