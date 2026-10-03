package com.topdawg.focusmaxxing.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topdawg.focusmaxxing.data.AuthRepository
import com.topdawg.focusmaxxing.data.Room
import com.topdawg.focusmaxxing.data.RoomRepository
import com.topdawg.focusmaxxing.data.UserProfile
import com.topdawg.focusmaxxing.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class HomeUiState(val profile: UserProfile? = null, val rooms: List<Room> = emptyList(), val profileLoading: Boolean = true, val roomsLoading: Boolean = true, val error: String? = null) {
    val loading: Boolean get() = profileLoading || roomsLoading
}
class HomeViewModel(private val auth: AuthRepository, private val users: UserRepository, private val rooms: RoomRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var roomsJob: Job? = null
    init {
        val uid = auth.currentUserId
        if (uid == null) _uiState.value = HomeUiState(profileLoading = false, roomsLoading = false, error = "Please sign in again.")
        else {
            viewModelScope.launch { try { users.observeProfile(uid).collect { _uiState.value = _uiState.value.copy(profile = it, profileLoading = false) } } catch (e: Exception) { _uiState.value = _uiState.value.copy(profileLoading = false, error = e.message ?: "Could not load your profile.") } }
            observeRooms()
        }
    }
    fun refreshProfile() { val uid = auth.currentUserId ?: return; viewModelScope.launch { _uiState.value = _uiState.value.copy(profileLoading = true, error = null); try { _uiState.value = _uiState.value.copy(profile = users.getProfile(uid), profileLoading = false, error = null) } catch (e: Exception) { _uiState.value = _uiState.value.copy(profileLoading = false, error = e.message ?: "Could not load your stats.") } } }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun retryPublicRooms() { observeRooms() }
    private fun observeRooms() {
        roomsJob?.cancel()
        roomsJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(roomsLoading = true, error = null)
            try { rooms.observePublicRooms().collect { _uiState.value = _uiState.value.copy(rooms = it, roomsLoading = false, error = null) } }
            catch (e: Exception) { _uiState.value = _uiState.value.copy(roomsLoading = false, error = e.message ?: "Could not load public rooms.") }
        }
    }
}
