package com.topdawg.focusmaxxing.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import com.topdawg.focusmaxxing.solo.SoloConstants
import com.topdawg.focusmaxxing.solo.SoloSessionAccounting
import com.topdawg.focusmaxxing.solo.SoloSessionRecord

class FakeRepositories : AuthRepository, UserRepository, RoomRepository, FriendRepository, SoloRepository {
    private val profiles = linkedMapOf<String, UserProfile>()
    private val rooms = MutableStateFlow<Map<String, Room>>(emptyMap())
    private val users = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    private val requests = MutableStateFlow<List<FriendRequest>>(emptyList())
    private val friendIds = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    private val soloSessions = MutableStateFlow<Map<String, List<SoloSessionRecord>>>(emptyMap())
    private val sequence = AtomicLong()
    private var uid: String? = null
    private var guest = false
    private val _authChanges = MutableStateFlow<String?>(null)
    override val authChanges: StateFlow<String?> = _authChanges.asStateFlow()
    override val currentUserId get() = uid
    override val isGuest get() = guest

    override suspend fun signInEmail(email: String, password: String) { uid = "email_${email.lowercase()}"; guest = false; _authChanges.value = uid }
    override suspend fun registerEmail(email: String, password: String) = signInEmail(email, password)
    override suspend fun signInGoogle(idToken: String) { uid = "google_${idToken.take(8)}"; guest = false; _authChanges.value = uid }
    override suspend fun signInGuest(): String { uid = "guest_${sequence.incrementAndGet()}"; guest = true; _authChanges.value = uid; return uid!! }
    override suspend fun linkEmail(email: String, password: String) { guest = false }
    override suspend fun linkGoogle(idToken: String) { guest = false }
    override suspend fun signOut() { uid = null; guest = false; _authChanges.value = null }

    override suspend fun getProfile(uid: String) = profiles[uid]
    override suspend fun ensureProfile(uid: String, guest: Boolean): UserProfile {
        profiles[uid]?.let { return it }
        val candidate = if (guest) generateSequence { "guest_${Random.nextInt(1000, 9999)}" }.first { value -> profiles.values.none { it.username == value } } else ""
        val profile = UserProfile(uid, candidate, guest)
        profiles[uid] = profile
        users.value = profiles.toMap()
        return profile
    }
    override suspend fun claimUsername(uid: String, username: String): UserProfile {
        require(username.matches(Regex("^[a-z0-9_]{3,20}$"))) { "Use 3–20 lowercase letters, numbers, or underscores." }
        require(profiles.values.none { it.uid != uid && it.username == username }) { "That username is already taken." }
        val profile = (profiles[uid] ?: UserProfile(uid, "", false)).copy(username = username)
        profiles[uid] = profile; users.value = profiles.toMap(); return profile
    }
    override suspend fun searchUsername(username: String) = profiles.values.firstOrNull { it.username == username }
    override suspend fun setGuestStatus(uid: String, isGuest: Boolean) { profiles[uid]?.let { profiles[uid] = it.copy(isGuest = isGuest); users.value = profiles.toMap() } }
    override fun observeProfile(uid: String): Flow<UserProfile?> = users.map { it[uid] ?: profiles[uid] }

    override suspend fun createRoom(name: String, isPublic: Boolean): Room {
        require(name.isNotBlank()) { "Enter a room name." }
        val userId = uid ?: error("Please sign in again.")
        val profile = profiles[userId] ?: ensureProfile(userId, guest)
        val code = generateSequence { (1..6).map { ("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")[Random.nextInt(36)] }.joinToString("") }.first { it !in rooms.value }
        val room = Room(code, name.trim(), userId, isPublic, 3, players = listOf(RoomPlayer(userId, profile.username, profile.rank, System.currentTimeMillis())))
        rooms.value = rooms.value + (code to room)
        return room
    }
    override suspend fun joinRoom(code: String): Room {
        val userId = uid ?: error("Please sign in again.")
        val room = rooms.value[code.uppercase()] ?: error("No room found with that code.")
        require(room.players.any { it.uid == userId } || room.players.size < room.capacity) { "This room is full." }
        val profile = profiles[userId] ?: ensureProfile(userId, guest)
        val updated = if (room.players.any { it.uid == userId }) room else room.copy(players = room.players + RoomPlayer(userId, profile.username, profile.rank, System.currentTimeMillis()), playerCount = room.players.size + 1)
        rooms.value = rooms.value + (updated.code to updated); return updated
    }
    override suspend fun leaveRoom(code: String) {
        val room = rooms.value[code] ?: return
        val remaining = room.players.filterNot { it.uid == uid }
        rooms.value = if (remaining.isEmpty()) rooms.value - code else rooms.value + (code to room.copy(hostId = if (room.hostId == uid) remaining.first().uid else room.hostId, players = remaining, playerCount = remaining.size))
    }
    override fun observeRoom(code: String): Flow<Room?> = rooms.map { it[code] }
    override fun observePublicRooms(): Flow<List<Room>> = rooms.map { it.values.filter { room -> room.isPublic && room.status == "waiting" } }

    override suspend fun sendRequest(username: String) {
        require(!guest) { "Create an account to add friends." }
        val sender = uid ?: error("Please sign in again.")
        val target = searchUsername(username) ?: error("No user found with that username.")
        require(target.uid != sender) { "You can't add yourself." }
        requests.value = requests.value + FriendRequest("r${sequence.incrementAndGet()}", sender, profiles[sender]?.username.orEmpty(), target.uid, target.username)
    }
    override suspend fun respond(requestId: String, accept: Boolean) {
        val request = requests.value.firstOrNull { it.id == requestId } ?: return
        if (accept) {
            val current = uid ?: return
            friendIds.value = friendIds.value + (current to (friendIds.value[current].orEmpty() + request.fromUid)) + (request.fromUid to (friendIds.value[request.fromUid].orEmpty() + current))
        }
        requests.value = requests.value.filterNot { it.id == requestId }
    }
    override suspend fun removeFriend(friendUid: String) {
        val current = uid ?: return
        friendIds.value = friendIds.value + (current to (friendIds.value[current].orEmpty() - friendUid)) + (friendUid to (friendIds.value[friendUid].orEmpty() - current))
    }
    override fun observeRequests(): Flow<List<FriendRequest>> = requests.map { all -> all.filter { it.toUid == uid } }
    override fun observeFriends(): Flow<List<UserProfile>> = friendIds.map { ids -> ids[uid].orEmpty().mapNotNull { profiles[it] } }

    override suspend fun finishSession(uid: String, session: SoloSessionRecord): SoloSessionRecord {
        soloSessions.value[uid].orEmpty().firstOrNull { it.sessionId == session.sessionId }?.let { return it }
        val profile = profiles[uid] ?: error("Your profile is unavailable.")
        val result = SoloSessionAccounting.award(profile, session)
        profiles[uid] = result.profile
        users.value = profiles.toMap()
        soloSessions.value = soloSessions.value + (uid to (listOf(result.session) + soloSessions.value[uid].orEmpty()).take(SoloConstants.HISTORY_LIMIT))
        return result.session
    }

    override fun observeRecentSessions(uid: String): Flow<List<SoloSessionRecord>> = soloSessions.map { it[uid].orEmpty().take(SoloConstants.HISTORY_LIMIT) }
}
