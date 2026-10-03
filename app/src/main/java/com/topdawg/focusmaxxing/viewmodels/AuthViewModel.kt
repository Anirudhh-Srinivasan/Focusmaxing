package com.topdawg.focusmaxxing.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topdawg.focusmaxxing.data.AuthRepository
import com.topdawg.focusmaxxing.data.UserProfile
import com.topdawg.focusmaxxing.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthDestination { LOADING, SIGN_IN, USERNAME, HOME }
data class AuthUiState(val destination: AuthDestination = AuthDestination.LOADING, val loading: Boolean = false, val error: String? = null, val profile: UserProfile? = null, val upgradeDone: Boolean = false)

class AuthViewModel(private val auth: AuthRepository, private val users: UserRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    init { viewModelScope.launch { auth.authChanges.collect { refresh() } }; viewModelScope.launch { refresh() } }
    private suspend fun refresh() {
        val uid = auth.currentUserId
        if (uid == null) { _uiState.value = AuthUiState(destination = AuthDestination.SIGN_IN); return }
        try {
            val profile = users.getProfile(uid)
            _uiState.value = AuthUiState(destination = if (profile?.username.isNullOrBlank()) AuthDestination.USERNAME else AuthDestination.HOME, profile = profile)
        } catch (e: Exception) { _uiState.value = _uiState.value.copy(destination = AuthDestination.SIGN_IN, error = e.message ?: "Could not load your account.") }
    }
    fun signIn(email: String, password: String) = runAction {
        require(email.isNotBlank()) { "Enter your email address." }
        require(password.isNotBlank()) { "Enter your password." }
        auth.signInEmail(email, password); refresh()
    }
    fun register(email: String, password: String) = runAction {
        require(email.contains("@")) { "Enter a valid email address." }
        require(password.length >= 6) { "Password must be at least 6 characters." }
        auth.registerEmail(email, password); refresh()
    }
    fun signInGoogle(idToken: String) = runAction { auth.signInGoogle(idToken); refresh() }
    fun signInGuest() = runAction {
        val uid = auth.signInGuest(); val profile = users.ensureProfile(uid, true)
        _uiState.value = AuthUiState(destination = AuthDestination.HOME, profile = profile)
    }
    fun saveUsername(username: String) = runAction {
        val uid = auth.currentUserId ?: error("Please sign in again.")
        val profile = users.claimUsername(uid, username)
        _uiState.value = AuthUiState(destination = AuthDestination.HOME, profile = profile)
    }
    fun upgradeEmail(email: String, password: String) = runAction {
        auth.linkEmail(email, password)
        auth.currentUserId?.let { users.setGuestStatus(it, false) }
        _uiState.value = _uiState.value.copy(loading = false, upgradeDone = true, error = null)
    }
    fun upgradeGoogle(idToken: String) = runAction {
        auth.linkGoogle(idToken)
        auth.currentUserId?.let { users.setGuestStatus(it, false) }
        _uiState.value = _uiState.value.copy(loading = false, upgradeDone = true, error = null)
    }
    fun signOut() = runAction { auth.signOut(); _uiState.value = AuthUiState(destination = AuthDestination.SIGN_IN) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun reportError(message: String) { _uiState.value = _uiState.value.copy(loading = false, error = message) }
    private fun runAction(block: suspend () -> Unit) { viewModelScope.launch { _uiState.value = _uiState.value.copy(loading = true, error = null); try { block() } catch (e: Exception) { _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "Something went wrong. Please try again.") } } }
}
