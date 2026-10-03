package com.topdawg.focusmaxxing.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.topdawg.focusmaxxing.solo.SoloConstants
import com.topdawg.focusmaxxing.solo.SoloMaterial
import com.topdawg.focusmaxxing.solo.SoloPhase
import com.topdawg.focusmaxxing.solo.SoloViewModel
import com.topdawg.focusmaxxing.solo.SoloScoring
import java.io.File
import java.util.LinkedHashMap
import kotlin.math.roundToInt

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
        SoloPhase.SETUP -> Column(Modifier.fillMaxSize().padding(16.dp)) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Solo Battle", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(topic, { topic = it }, label = { Text("Topic name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))
            Text("Study material")
            state.materials.forEach { material ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { viewModel.selectMaterial(material) }) { Text((if (state.selectedMaterial?.localId == material.localId) "✓ " else "") + material.displayName) }
                    Text("${material.pageCount} pages", style = MaterialTheme.typography.labelMedium)
                }
            }
            OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf", "text/plain")) }, modifier = Modifier.fillMaxWidth()) { Text("Pick PDF or TXT") }
            Spacer(Modifier.height(12.dp))
            Text("Session length")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SoloConstants.SESSION_LENGTHS_MINUTES.forEach { minutes ->
                    FilterChip(selected = duration == minutes, onClick = { duration = minutes }, label = { Text("${minutes}m") })
                }
            }
            OutlinedTextField(startPage, { startPage = it }, label = { Text("Start from page") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            if (state.isFakeAi) Text("FAKE/DEV ONLY — recall scores are based on summary length.", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium)
            if (notificationsUnavailable) Text("Notifications are off. Checkpoints will appear when Focusmaxxing is open.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(enabled = !state.loading, onClick = {
                if (android.os.Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.prepareSession(topic, duration, startPage.toIntOrNull() ?: -1)
            }, modifier = Modifier.fillMaxWidth()) { Text("Start session") }
            if (state.activeSession != null) TextButton(onClick = { viewModel.openSolo() }) { Text("Resume active session") }
        }
        SoloPhase.COUNTDOWN -> CountdownScreen(onFinished = viewModel::beginSession)
        SoloPhase.READER -> {
            val active = state.activeSession
            if (active == null) { ErrorSoloScreen("No active session found.", onBack) }
            else {
                ReaderScreen(
                    active.material,
                    state.remainingSeconds,
                    active.currentPage,
                    onCurrentPage = viewModel::updateCurrentPage,
                    onLeaveRequested = { confirmLeave = true }
                )
            }
        }
        SoloPhase.CHECKPOINT -> {
            val active = state.activeSession
            if (active == null) ErrorSoloScreen("The checkpoint session is unavailable.", onBack)
            else CheckpointScreen(
                active.material.pageCount,
                active.segmentFromPage.coerceIn(1, active.material.pageCount),
                active.currentPage.coerceIn(1, active.material.pageCount),
                active.checkpointIsFinal,
                active.checkpointActiveElapsedMs?.let { (it - active.segmentStartActiveMs).coerceAtLeast(0) / 1000L } ?: 0L,
                state.loading,
                state.error,
                onSubmit = viewModel::submitCheckpoint,
                onLeave = { confirmLeave = true }
            )
        }
        SoloPhase.SEGMENT_RESULT -> SegmentResultScreen(state.lastSegment, state.isFakeAi, viewModel::continueAfterSegment)
        SoloPhase.RESULTS -> SessionResultsScreen(state.completedSession, viewModel::runItBack, onBack)
    }
    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        title = { Text("Leave this session?") },
        text = { Text("Your scored checkpoints are saved. The current unfinished segment will be lost.") },
        confirmButton = { TextButton(onClick = { confirmLeave = false; viewModel.leaveSession(onBack) }) { Text("Leave") } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep studying") } }
    )
}

@Composable
private fun CountdownScreen(onFinished: () -> Unit) {
    var seconds by remember { mutableIntStateOf(SoloConstants.COUNTDOWN_SECONDS) }
    LaunchedEffect(Unit) {
        while (seconds > 1) { kotlinx.coroutines.delay(1_000); seconds-- }
        kotlinx.coroutines.delay(1_000)
        onFinished()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text(seconds.toString(), style = MaterialTheme.typography.displayLarge) }
}

@Composable
private fun ReaderScreen(material: SoloMaterial, remaining: Long, currentPage: Int, onCurrentPage: (Int) -> Unit, onLeaveRequested: () -> Unit) {
    val context = LocalContext.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentPage - 1).coerceIn(0, material.pageCount - 1))
    val textPages by produceState(initialValue = emptyList<String>(), material.localId) {
        value = if (material.isPdf) emptyList() else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { com.topdawg.focusmaxxing.solo.SoloMaterialFiles.splitTxtIntoPages(File(material.localPath).readText(Charsets.UTF_8)) }.getOrDefault(emptyList())
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            info.visibleItemsInfo.maxByOrNull { item ->
                (minOf(item.offset + item.size, info.viewportEndOffset) - maxOf(item.offset, info.viewportStartOffset)).coerceAtLeast(0)
            }?.index
        }
            .collect { visible -> visible?.let { onCurrentPage((it + 1).coerceIn(1, material.pageCount)) } }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onLeaveRequested) { Text("Leave") }
            Text("${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.titleMedium)
            Text("Page $currentPage/${material.pageCount}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
        }
        if (material.isPdf) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(material.pageCount) { index ->
                    PdfPage(material.localPath, index, Modifier.fillMaxWidth())
                }
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                itemsIndexed(textPages) { index, page ->
                    Text("Page ${index + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(page, modifier = Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun PdfPage(path: String, pageIndex: Int, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(null, path, pageIndex) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { PdfBitmapCache.render(path, pageIndex) }
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), "PDF page ${pageIndex + 1}", modifier = modifier.heightIn(min = 400.dp))
    else Box(modifier.height(480.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
}

private object PdfBitmapCache {
    private val cache = object : LinkedHashMap<String, Bitmap>(16, .75f, true) {
        private var bytes = 0
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
            if (bytes > SoloConstants.PDF_BITMAP_CACHE_BYTES) {
                eldest?.value?.let { bytes -= it.allocationByteCount }
                eldest?.value?.recycle()
                return true
            }
            return false
        }
        override fun put(key: String, value: Bitmap): Bitmap? { bytes += value.allocationByteCount; return super.put(key, value) }
    }
    @Synchronized fun render(path: String, page: Int): Bitmap? {
        val key = "$path#$page"
        cache[key]?.let { return it }
        val descriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        val bitmap = descriptor.use { pfd -> PdfRenderer(pfd).use { renderer ->
            if (page !in 0 until renderer.pageCount) return null
            renderer.openPage(page).use { pdfPage ->
                val width = 1400
                val height = (width * pdfPage.height.toFloat() / pdfPage.width).roundToInt().coerceAtLeast(1)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { pdfPage.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
            }
        } }
        cache[key] = bitmap
        return bitmap
    }
}

@Composable
private fun CheckpointScreen(
    pageCount: Int,
    defaultFrom: Int,
    defaultTo: Int,
    isFinal: Boolean,
    activeSeconds: Long,
    loading: Boolean,
    error: String?,
    onSubmit: (Int, Int, String, Boolean) -> Unit,
    onLeave: () -> Unit
) {
    var from by remember(defaultFrom) { mutableStateOf(defaultFrom.toString()) }
    var to by remember(defaultTo) { mutableStateOf(defaultTo.toString()) }
    var summary by remember { mutableStateOf("") }
    var nothing by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text(if (isFinal) "Final checkpoint" else "How far have you got?", style = MaterialTheme.typography.headlineSmall)
        TextButton(onClick = onLeave) { Text("Leave session") }
        Text("Scores are estimates. Your timer is paused while you check in.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(from, { from = it.filter(Char::isDigit) }, label = { Text("From page") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(to, { to = it.filter(Char::isDigit) }, label = { Text("To page") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        }
        Text("Pages 1–$pageCount · up to ${SoloConstants.MAX_PAGES_PER_CHECKPOINT} pages per checkpoint", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = nothing, onCheckedChange = { nothing = it })
            Text("I didn't cover anything")
        }
        OutlinedTextField(
            value = summary,
            onValueChange = { value -> if (Regex("\\b[\\w'-]+\\b").findAll(value).count() <= SoloConstants.MAX_RECALL_WORDS) summary = value },
            label = { Text("Write 2–4 sentences about what you covered") },
            supportingText = { Text("15–150 words") },
            enabled = !nothing,
            modifier = Modifier.fillMaxWidth().weight(1f),
            minLines = 4
        )
        Text("Study time in this segment: ${activeSeconds / 60} min ${activeSeconds % 60} sec", style = MaterialTheme.typography.labelMedium)
        error?.takeIf(String::isNotBlank)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(enabled = !loading, onClick = {
            onSubmit(from.toIntOrNull() ?: -1, to.toIntOrNull() ?: -1, summary, nothing)
        }, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "Scoring…" else if (isFinal) "Finish session" else "Submit checkpoint") }
    }
}

@Composable
private fun SegmentResultScreen(segment: com.topdawg.focusmaxxing.solo.SoloSegmentRecord?, fake: Boolean, onContinue: () -> Unit) {
    if (segment == null) return ErrorSoloScreen("The segment result is unavailable.", onContinue)
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Segment result", style = MaterialTheme.typography.headlineSmall)
        Text("Scores are estimates${if (fake) " · FAKE/DEV ONLY" else ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (segment.didNotCoverAnything) "No pages covered" else "Pages ${segment.fromPage}–${segment.toPage}", style = MaterialTheme.typography.titleMedium)
            Text("${segment.activeSeconds / 60} min · ${segment.paceWpm} words/min · recall ${segment.recallScore}%")
            Text("${segment.points} points", style = MaterialTheme.typography.titleLarge)
            Text(segment.feedback)
            if (segment.keyPointsMissed.isNotEmpty()) {
                Text("Key points to revisit", style = MaterialTheme.typography.titleSmall)
                segment.keyPointsMissed.forEach { Text("• $it") }
            }
        } }
        Spacer(Modifier.weight(1f))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Back to reading") }
    }
}

@Composable
private fun SessionResultsScreen(session: com.topdawg.focusmaxxing.solo.SoloSessionRecord?, onRunItBack: () -> Unit, onBack: () -> Unit) {
    if (session == null) return ErrorSoloScreen("The saved session result is unavailable.", onBack)
    val progress = SoloScoring.rankProgress(session.xpAfter)
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Session complete", style = MaterialTheme.typography.headlineSmall) }
        item { Text("${session.totalPoints} XP earned · ${session.totalWordsCovered} words · ${session.interruptions} interruptions") }
        item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text("${progress.rank.label} · ${session.xpAfter} XP", style = MaterialTheme.typography.titleLarge)
                if (progress.nextRank != null) {
                    LinearProgressIndicator(progress = { progress.progressWithinRank }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    Text("${progress.xpToNextRank} XP to ${progress.nextRank.label}")
                } else Text("Top rank")
            } }
        }
        item { Text("Segments", style = MaterialTheme.typography.titleLarge) }
        itemsIndexed(session.segments) { index, segment ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                Text("Segment ${index + 1}: pages ${segment.fromPage}–${segment.toPage}", style = MaterialTheme.typography.titleMedium)
                Text("${segment.wordsCovered} words · ${segment.activeSeconds / 60} min · recall ${segment.recallScore}% · ${segment.points} XP")
                Text(segment.feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        }
        item { Button(onClick = onRunItBack, modifier = Modifier.fillMaxWidth()) { Text("Run it back") } }
        item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to home") } }
    }
}

@Composable private fun ErrorSoloScreen(message: String, onBack: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp)) { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = onBack) { Text("Back") } } }
