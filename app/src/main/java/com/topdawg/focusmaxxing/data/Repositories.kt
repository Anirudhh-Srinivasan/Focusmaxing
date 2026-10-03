package com.topdawg.focusmaxxing.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import com.topdawg.focusmaxxing.solo.SoloSessionRecord

interface AuthRepository {
    val currentUserId: String?
    val isGuest: Boolean
    val authChanges: StateFlow<String?>
    suspend fun signInEmail(email: String, password: String)
    suspend fun registerEmail(email: String, password: String)
    suspend fun signInGoogle(idToken: String)
    suspend fun signInGuest(): String
    suspend fun linkEmail(email: String, password: String)
    suspend fun linkGoogle(idToken: String)
    suspend fun signOut()
}

interface UserRepository {
    suspend fun getProfile(uid: String): UserProfile?
    suspend fun ensureProfile(uid: String, guest: Boolean): UserProfile
    suspend fun claimUsername(uid: String, username: String): UserProfile
    suspend fun searchUsername(username: String): UserProfile?
    suspend fun setGuestStatus(uid: String, isGuest: Boolean)
    fun observeProfile(uid: String): Flow<UserProfile?>
}

interface RoomRepository {
    suspend fun createRoom(name: String, isPublic: Boolean): Room
    suspend fun joinRoom(code: String): Room
    suspend fun leaveRoom(code: String)
    fun observeRoom(code: String): Flow<Room?>
    fun observePublicRooms(): Flow<List<Room>>
}

interface FriendRepository {
    suspend fun sendRequest(username: String)
    suspend fun respond(requestId: String, accept: Boolean)
    suspend fun removeFriend(friendUid: String)
    fun observeRequests(): Flow<List<FriendRequest>>
    fun observeFriends(): Flow<List<UserProfile>>
}

interface SoloRepository {
    suspend fun finishSession(uid: String, session: SoloSessionRecord): SoloSessionRecord
    fun observeRecentSessions(uid: String): Flow<List<SoloSessionRecord>>
}
