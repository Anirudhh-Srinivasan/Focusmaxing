package com.topdawg.focusmaxxing.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topdawg.focusmaxxing.data.AuthRepository
import com.topdawg.focusmaxxing.data.FriendRepository
import com.topdawg.focusmaxxing.data.FriendRequest
import com.topdawg.focusmaxxing.data.UserProfile
import com.topdawg.focusmaxxing.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendsUiState(val friends: List<UserProfile> = emptyList(), val requests: List<FriendRequest> = emptyList(), val selectedProfile: UserProfile? = null, val loading: Boolean = true, val error: String? = null)
class FriendsViewModel(private val auth: AuthRepository, private val users: UserRepository, private val repo: FriendRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()
    val isGuest get() = auth.isGuest
    init {
        viewModelScope.launch { try { repo.observeFriends().collect { _uiState.value = _uiState.value.copy(friends = it, loading = false) } } catch (e: Exception) { fail(e) } }
        viewModelScope.launch { try { repo.observeRequests().collect { _uiState.value = _uiState.value.copy(requests = it, loading = false) } } catch (e: Exception) { fail(e) } }
    }
    fun search(username: String) = action {
        val profile = users.searchUsername(username.trim().lowercase()) ?: error("No user found with that username.")
        _uiState.value = _uiState.value.copy(selectedProfile = profile)
    }
    fun openProfile(profile: UserProfile) { _uiState.value = _uiState.value.copy(selectedProfile = profile, error = null) }
    fun closeProfile() { _uiState.value = _uiState.value.copy(selectedProfile = null) }
    fun sendRequest(username: String) = action { repo.sendRequest(username) }
    fun respond(id: String, accept: Boolean) = action { repo.respond(id, accept) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    private fun action(block: suspend () -> Unit) { viewModelScope.launch { _uiState.value = _uiState.value.copy(loading = true, error = null); try { block(); _uiState.value = _uiState.value.copy(loading = false) } catch (e: Exception) { fail(e) } } }
    private fun fail(e: Exception) { _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "Something went wrong. Please try again.") }
}
