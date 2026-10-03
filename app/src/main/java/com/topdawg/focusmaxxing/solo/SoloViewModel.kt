package com.topdawg.focusmaxxing.solo

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topdawg.focusmaxxing.data.AuthRepository
import com.topdawg.focusmaxxing.data.SoloRepository
import com.topdawg.focusmaxxing.solo.ai.RecallAiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random
import androidx.lifecycle.ViewModelProvider

enum class SoloPhase { SETUP, COUNTDOWN, READER, CHECKPOINT, SEGMENT_RESULT, RESULTS }

data class SoloUiState(
    val phase: SoloPhase = SoloPhase.SETUP,
    val materials: List<SoloMaterial> = emptyList(),
    val selectedMaterial: SoloMaterial? = null,
    val activeSession: ActiveSoloSession? = null,
    val recentSessions: List<SoloSessionRecord> = emptyList(),
    val remainingSeconds: Long = 0L,
    val loading: Boolean = false,
    val error: String? = null,
    val isFakeAi: Boolean = false
)

class SoloViewModel(
    private val appContext: Context,
    private val auth: AuthRepository,
    private val repository: SoloRepository,
    private val localStore: SoloLocalStore,
    private val materialFiles: SoloMaterialFiles,
    private val recallAi: RecallAiService
) : ViewModel() {
    private val _uiState = MutableStateFlow(SoloUiState(isFakeAi = recallAi.isFake))
    val uiState: StateFlow<SoloUiState> = _uiState.asStateFlow()
    private var pendingStart: PendingStart? = null
    private var ticker: Job? = null
    private var foreground = true

    fun currentState(): SoloUiState = _uiState.value

    init {
        val materials = runCatching { localStore.materials() }.getOrElse { emptyList() }
        val active = runCatching { localStore.activeSession() }.getOrNull()
        _uiState.value = _uiState.value.copy(
            materials = materials,
            selectedMaterial = materials.firstOrNull(),
            activeSession = active,
            phase = if (active == null) SoloPhase.SETUP else if (active.checkpointActiveElapsedMs != null) SoloPhase.CHECKPOINT else SoloPhase.READER,
            remainingSeconds = active?.let { remainingSeconds(it, System.currentTimeMillis()) } ?: 0L
        )
        val uid = auth.currentUserId
        if (uid != null) viewModelScope.launch {
            try { repository.observeRecentSessions(uid).collect { _uiState.value = _uiState.value.copy(recentSessions = it) } }
            catch (error: Exception) { reportError(error.message ?: "Could not load recent study sessions.") }
        }
        active?.let { startTicker() }
    }

    fun openSolo() {
        val active = localStore.activeSession()
        if (active != null) {
            _uiState.value = _uiState.value.copy(activeSession = active, phase = if (active.checkpointActiveElapsedMs != null) SoloPhase.CHECKPOINT else SoloPhase.READER)
            startTicker()
        } else {
            _uiState.value = _uiState.value.copy(phase = SoloPhase.SETUP, selectedMaterial = localStore.materials().firstOrNull(), materials = localStore.materials(), error = null)
        }
    }

    fun importMaterial(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val material = withContext(Dispatchers.IO) { materialFiles.import(uri) }
                _uiState.value = _uiState.value.copy(
                    materials = localStore.materials(), selectedMaterial = material,
                    loading = false, error = null
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = error.message ?: "Could not open that study material.")
            }
        }
    }

    fun selectMaterial(material: SoloMaterial) {
        _uiState.value = _uiState.value.copy(selectedMaterial = material, error = null)
    }

    fun prepareSession(topic: String, durationMinutes: Int, startPage: Int) {
        viewModelScope.launch {
            val selected = _uiState.value.selectedMaterial
            try {
                require(topic.isNotBlank()) { "Enter a topic name." }
                require(durationMinutes in setOf(25, 45, 60, 90)) { "Choose a supported session length." }
                require(selected != null) { "Choose one PDF or TXT material first." }
                require(startPage in 1..selected.pageCount) { "Start page must be between 1 and ${selected.pageCount}." }
                _uiState.value = _uiState.value.copy(loading = true, error = null)
                val ingested = recallAi.ingest(selected, topic.trim())
                require(ingested.pageCount > 0 && ingested.pageWordCounts.size == ingested.pageCount) { "The material parser returned an invalid page list." }
                localStore.saveMaterial(ingested.copy(lastReadPage = selected.lastReadPage.coerceIn(1, ingested.pageCount)))
                pendingStart = PendingStart(ingested, topic.trim(), durationMinutes, startPage)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    phase = SoloPhase.COUNTDOWN,
                    selectedMaterial = ingested,
                    materials = localStore.materials(),
                    error = null
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = error.message ?: "Could not prepare your study session.")
            }
        }
    }

    fun beginSession() {
        val start = pendingStart ?: return reportError("The session setup expired. Please set it up again.")
        val now = System.currentTimeMillis()
        val durationMs = start.durationMinutes * 60_000L
        val firstDue = nextDueElapsed(0L, durationMs)
        val active = ActiveSoloSession(
            sessionId = UUID.randomUUID().toString(),
            material = start.material,
            topic = start.topic,
            durationSeconds = start.durationMinutes * 60,
            startPage = start.startPage,
            startedAtMs = now,
            currentPage = start.startPage,
            lastPageReached = start.startPage,
            segmentFromPage = start.startPage,
            nextCheckpointAtActiveMs = firstDue
        )
        pendingStart = null
        localStore.saveActiveSession(active)
        _uiState.value = _uiState.value.copy(phase = SoloPhase.READER, activeSession = active, remainingSeconds = start.durationMinutes * 60L, error = null)
        startTicker()
    }

    fun updateCurrentPage(page: Int) {
        val active = _uiState.value.activeSession ?: return
        val safePage = page.coerceIn(1, active.material.pageCount)
        val updated = active.copy(currentPage = safePage, lastPageReached = maxOf(active.lastPageReached, safePage))
        localStore.updateLastPage(active.material.localId, safePage)
        localStore.saveActiveSession(updated)
        _uiState.value = _uiState.value.copy(activeSession = updated)
    }

    fun onAppBackgrounded() {
        foreground = false
        val active = _uiState.value.activeSession ?: return
        val now = System.currentTimeMillis()
        val updated = active.copy(backgroundStartedAtMs = now)
        localStore.saveActiveSession(updated)
        _uiState.value = _uiState.value.copy(activeSession = updated)
        if (updated.checkpointActiveElapsedMs == null) {
            val alarmAt = updated.startedAtMs + updated.accumulatedPauseMs + updated.nextCheckpointAtActiveMs
            SoloCheckpointAlarm.schedule(appContext, maxOf(now, alarmAt))
        }
    }

    fun onAppForegrounded() {
        foreground = true
        val active = _uiState.value.activeSession ?: return
        val backgroundAt = active.backgroundStartedAtMs ?: return
        val awayMs = System.currentTimeMillis() - backgroundAt
        val updated = active.copy(
            backgroundStartedAtMs = null,
            currentSegmentInterruptions = active.currentSegmentInterruptions + if (awayMs > SoloConstants.INTERRUPTION_BACKGROUND_SECONDS * 1_000L) 1 else 0
        )
        localStore.saveActiveSession(updated)
        _uiState.value = _uiState.value.copy(activeSession = updated)
        refreshSession()
    }

    fun onAlarmTriggered() {
        foreground = true
        refreshSession()
    }

    fun reportError(message: String) {
        _uiState.value = _uiState.value.copy(loading = false, error = message)
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (true) {
                delay(SoloConstants.SESSION_TICK_MS)
                if (foreground) refreshSession()
            }
        }
    }

    private fun refreshSession() {
        val active = localStore.activeSession() ?: return
        val now = System.currentTimeMillis()
        val elapsed = activeElapsedMs(active, now)
        val durationMs = active.durationSeconds * 1_000L
        if (active.checkpointActiveElapsedMs == null && elapsed >= active.nextCheckpointAtActiveMs) {
            val final = elapsed >= durationMs
            val paused = active.copy(
                pausedAtMs = now,
                checkpointActiveElapsedMs = if (final) durationMs else elapsed,
                checkpointIsFinal = final,
                lastPageReached = maxOf(active.lastPageReached, active.currentPage)
            )
            localStore.saveActiveSession(paused)
            SoloCheckpointAlarm.cancel(appContext)
            _uiState.value = _uiState.value.copy(phase = SoloPhase.CHECKPOINT, activeSession = paused, remainingSeconds = 0L, error = null)
            return
        }
        val remaining = remainingSeconds(active, now)
        _uiState.value = _uiState.value.copy(
            phase = if (active.checkpointActiveElapsedMs != null) SoloPhase.CHECKPOINT else SoloPhase.READER,
            activeSession = active,
            remainingSeconds = remaining
        )
    }

    private fun nextDueElapsed(previousElapsedMs: Long, durationMs: Long): Long {
        val intervalMinutes = Random.nextInt(SoloConstants.MIN_CHECKPOINT_INTERVAL_MINUTES, SoloConstants.MAX_CHECKPOINT_INTERVAL_MINUTES + 1)
        val due = previousElapsedMs + intervalMinutes * 60_000L
        val lastAllowedIntermediate = durationMs - SoloConstants.MIN_REMAINING_MINUTES_FOR_CHECKPOINT * 60_000L
        return if (due <= lastAllowedIntermediate) due else durationMs
    }

    private fun activeElapsedMs(active: ActiveSoloSession, now: Long): Long {
        val pause = active.pausedAtMs?.let { now - it } ?: 0L
        return (now - active.startedAtMs - active.accumulatedPauseMs - pause).coerceAtLeast(0L)
    }

    private fun remainingSeconds(active: ActiveSoloSession, now: Long): Long {
        val remainingMs = active.durationSeconds * 1_000L - activeElapsedMs(active, now)
        return ((remainingMs.coerceAtLeast(0L) + 999L) / 1_000L)
    }

    private data class PendingStart(val material: SoloMaterial, val topic: String, val durationMinutes: Int, val startPage: Int)
}
