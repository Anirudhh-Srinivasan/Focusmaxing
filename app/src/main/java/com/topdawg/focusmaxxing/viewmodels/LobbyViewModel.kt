package com.topdawg.focusmaxxing.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topdawg.focusmaxxing.data.AuthRepository
import com.topdawg.focusmaxxing.data.Room
import com.topdawg.focusmaxxing.data.RoomRepository
import com.topdawg.focusmaxxing.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class LobbyUiState(val room: Room? = null, val loading: Boolean = false, val error: String? = null)
class LobbyViewModel(private val auth: AuthRepository, private val users: UserRepository, private val repository: RoomRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()
    private var roomJob: Job? = null
    fun validateRoomCode(code: String): Boolean = code.matches(Regex("^[A-Z0-9]{6}$"))
    fun createRoom(name: String, isPublic: Boolean, onCreated: (String) -> Unit) = action { onCreated(repository.createRoom(name, isPublic).code) }
    fun joinRoom(code: String, onJoined: (String) -> Unit) = action { onJoined(repository.joinRoom(code).code) }
    fun observeRoom(code: String) {
        roomJob?.cancel()
        roomJob = viewModelScope.launch { _uiState.value = LobbyUiState(loading = true); try { repository.observeRoom(code).collect { _uiState.value = LobbyUiState(room = it, error = if (it == null) "This room is no longer available." else null) } } catch (e: Exception) { _uiState.value = LobbyUiState(error = e.message ?: "Could not load this room.") } }
    }
    fun leaveRoom(code: String, onLeft: () -> Unit) = action { repository.leaveRoom(code); onLeft() }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun reportError(message: String) { _uiState.value = _uiState.value.copy(loading = false, error = message) }
    private fun action(block: suspend () -> Unit) { viewModelScope.launch { _uiState.value = _uiState.value.copy(loading = true, error = null); try { block(); _uiState.value = _uiState.value.copy(loading = false) } catch (e: Exception) { _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "Something went wrong. Please try again.") } } }
}
