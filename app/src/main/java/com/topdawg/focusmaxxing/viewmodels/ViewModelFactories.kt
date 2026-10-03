package com.topdawg.focusmaxxing.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.topdawg.focusmaxxing.data.AppContainer

class AuthViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(c.auth, c.users) as T
}
class HomeViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(c.auth, c.users, c.rooms) as T
}
class LobbyViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = LobbyViewModel(c.auth, c.users, c.rooms) as T
}
class FriendsViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = FriendsViewModel(c.auth, c.users, c.friends) as T
}
