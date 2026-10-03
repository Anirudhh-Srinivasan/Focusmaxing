package com.topdawg.focusmaxxing.data

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.random.Random

class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {
    private val _changes = MutableStateFlow(auth.currentUser?.uid)
    private val listener = FirebaseAuth.AuthStateListener { _changes.value = it.currentUser?.uid }
    init { auth.addAuthStateListener(listener) }
    override val currentUserId get() = auth.currentUser?.uid
    override val isGuest get() = auth.currentUser?.isAnonymous == true
    override val authChanges: StateFlow<String?> = _changes.asStateFlow()
    override suspend fun signInEmail(email: String, password: String) { auth.signInWithEmailAndPassword(email.trim(), password).await() }
    override suspend fun registerEmail(email: String, password: String) { auth.createUserWithEmailAndPassword(email.trim(), password).await() }
    override suspend fun signInGoogle(idToken: String) { auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await() }
    override suspend fun signInGuest(): String = auth.signInAnonymously().await().user?.uid ?: error("Could not start a guest session.")
    override suspend fun linkEmail(email: String, password: String) {
        val user = auth.currentUser ?: error("Please sign in again.")
        user.linkWithCredential(EmailAuthProvider.getCredential(email.trim(), password)).await()
    }
    override suspend fun linkGoogle(idToken: String) {
        val user = auth.currentUser ?: error("Please sign in again.")
        user.linkWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }
    override suspend fun signOut() { auth.signOut() }
}

class FirebaseUserRepository(private val db: FirebaseFirestore) : UserRepository {
    override suspend fun getProfile(uid: String): UserProfile? = db.collection("users").document(uid).get().await().takeIf { it.exists() }?.toUserProfile()
    override suspend fun ensureProfile(uid: String, guest: Boolean): UserProfile {
        getProfile(uid)?.let { return it }
        if (!guest) return UserProfile(uid = uid, username = "", isGuest = false)
        repeat(8) {
            val name = "guest_${Random.nextInt(1000, 9999)}"
            try { return claimUsername(uid, name).copy(isGuest = true) } catch (_: UsernameUnavailable) { }
        }
        error("Could not reserve a guest username. Please try again.")
    }
    override suspend fun claimUsername(uid: String, username: String): UserProfile {
        val normalized = username.lowercase(Locale.ROOT)
        require(normalized.matches(Regex("^[a-z0-9_]{3,20}$"))) { "Use 3–20 lowercase letters, numbers, or underscores." }
        val userRef = db.collection("users").document(uid)
        val usernameRef = db.collection("usernames").document(normalized)
        return db.runTransaction { tx ->
            val existing = tx.get(usernameRef)
            if (existing.exists() && existing.getString("uid") != uid) throw UsernameUnavailable()
            val old = tx.get(userRef).getString("username")
            if (old != null && old != normalized) throw IllegalStateException("This account already has a username.")
            val profile = tx.get(userRef).toUserProfileOrNew(uid)
            tx.set(usernameRef, mapOf("uid" to uid, "createdAt" to FieldValue.serverTimestamp()))
            tx.set(userRef, mapOf("username" to normalized, "isGuest" to (authIsGuest(uid)), "dawgRank" to profile.rank, "rp" to profile.rp, "xp" to profile.xp, "streak" to profile.streak, "battlesPlayed" to profile.battlesPlayed, "createdAt" to FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
            profile.copy(username = normalized)
        }.await()
    }
    private fun authIsGuest(uid: String) = FirebaseAuth.getInstance().currentUser?.let { it.uid == uid && it.isAnonymous } ?: false
    override suspend fun searchUsername(username: String): UserProfile? {
        val usernameDoc = db.collection("usernames").document(username.lowercase(Locale.ROOT)).get().await()
        val uid = usernameDoc.getString("uid") ?: return null
        return getProfile(uid)
    }
    override suspend fun setGuestStatus(uid: String, isGuest: Boolean) { db.collection("users").document(uid).update("isGuest", isGuest).await() }
    override fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) close(error) else trySend(snapshot?.takeIf { it.exists() }?.toUserProfile())
        }
        awaitClose { listener.remove() }
    }
    private fun com.google.firebase.firestore.DocumentSnapshot.toUserProfileOrNew(uid: String): UserProfile = if (exists()) toUserProfile() else UserProfile(uid, "", authIsGuest(uid))
    private class UsernameUnavailable : IllegalStateException("That username is already taken.")
}

class FirebaseRoomRepository(private val db: FirebaseFirestore, private val auth: AuthRepository, private val users: UserRepository) : RoomRepository {
    override suspend fun createRoom(name: String, isPublic: Boolean): Room {
        require(name.trim().isNotEmpty()) { "Enter a room name." }
        val uid = auth.currentUserId ?: error("Please sign in again.")
        val profile = users.getProfile(uid) ?: error("Finish setting up your profile first.")
        val rooms = db.collection("rooms")
        repeat(10) {
            val code = (1..6).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
            val roomRef = rooms.document(code)
            val created = db.runTransaction { tx ->
                if (tx.get(roomRef).exists()) false else {
                    tx.set(roomRef, mapOf("code" to code, "name" to name.trim(), "hostId" to uid, "isPublic" to isPublic, "capacity" to 3, "playerCount" to 1, "status" to "waiting", "createdAt" to FieldValue.serverTimestamp()))
                    tx.set(roomRef.collection("players").document(uid), mapOf("uid" to uid, "username" to profile.username, "rank" to profile.rank, "joinedAt" to FieldValue.serverTimestamp()))
                    true
                }
            }.await()
            if (created) return Room(code, name.trim(), uid, isPublic, 3, players = listOf(RoomPlayer(uid, profile.username, profile.rank, System.currentTimeMillis())))
        }
        error("Could not reserve a room code. Please try again.")
    }
    override suspend fun joinRoom(code: String): Room {
        val normalized = code.uppercase(Locale.ROOT)
        require(normalized.matches(Regex("^[A-Z0-9]{6}$"))) { "Enter a valid six-character room code." }
        val uid = auth.currentUserId ?: error("Please sign in again.")
        val profile = users.getProfile(uid) ?: error("Finish setting up your profile first.")
        val ref = db.collection("rooms").document(normalized)
        db.runTransaction { tx ->
            val room = tx.get(ref)
            if (!room.exists()) throw IllegalStateException("No room found with that code.")
            if (room.getString("status") != "waiting") throw IllegalStateException("This room is no longer open.")
            val playerRef = ref.collection("players").document(uid)
            val player = tx.get(playerRef)
            val existingPlayers = (room.getLong("playerCount") ?: 1L).toInt()
            val capacity = (room.getLong("capacity") ?: 3L).toInt()
            if (!player.exists() && existingPlayers >= capacity) throw IllegalStateException("This room is full.")
            if (!player.exists()) {
                tx.set(playerRef, mapOf("uid" to uid, "username" to profile.username, "rank" to profile.rank, "joinedAt" to FieldValue.serverTimestamp()))
                tx.update(ref, "playerCount", existingPlayers + 1)
            }
        }.await()
        return loadRoom(normalized) ?: error("The room closed while you were joining.")
    }
    private suspend fun loadRoom(code: String): Room? {
        val ref = db.collection("rooms").document(code)
        val doc = ref.get().await()
        if (!doc.exists()) return null
        val players = ref.collection("players").get().await().documents.map { RoomPlayer(it.id, it.getString("username") ?: "unknown", it.getString("rank") ?: "Bronze", it.getTimestamp("joinedAt")?.toDate()?.time ?: 0L) }
        return Room(code, doc.getString("name") ?: "Room", doc.getString("hostId") ?: "", doc.getBoolean("isPublic") ?: false, (doc.getLong("capacity") ?: 3L).toInt(), doc.getString("status") ?: "waiting", players)
    }
    override suspend fun leaveRoom(code: String) {
        val uid = auth.currentUserId ?: return
        val ref = db.collection("rooms").document(code)
        val playersRef = ref.collection("players")
        val candidates = playersRef.orderBy("joinedAt").get().await().documents.map { it.id }.filter { it != uid }
        db.runTransaction { tx ->
            val room = tx.get(ref)
            if (!room.exists()) return@runTransaction
            val ownPlayer = playersRef.document(uid)
            tx.get(ownPlayer)
            val count = (room.getLong("playerCount") ?: 1L).toInt()
            val hostLeaving = room.getString("hostId") == uid
            val newHost = if (hostLeaving) candidates.firstOrNull { candidate -> tx.get(playersRef.document(candidate)).exists() } else null
            tx.delete(ownPlayer)
            if (count <= 1) tx.delete(ref)
            else {
                if (hostLeaving && newHost == null) throw IllegalStateException("Could not hand host to another player. Please try again.")
                val updates = mutableMapOf<String, Any>("playerCount" to count - 1)
                if (hostLeaving && newHost != null) updates["hostId"] = newHost
                tx.update(ref, updates)
            }
        }.await()
    }
    override fun observeRoom(code: String): Flow<Room?> = callbackFlow {
        val ref = db.collection("rooms").document(code)
        var roomData: com.google.firebase.firestore.DocumentSnapshot? = null
        var playerData = emptyList<com.google.firebase.firestore.DocumentSnapshot>()
        fun publish() {
            val doc = roomData ?: return
            if (!doc.exists()) { trySend(null); return }
            trySend(Room(code, doc.getString("name") ?: "Room", doc.getString("hostId") ?: "", doc.getBoolean("isPublic") ?: false, (doc.getLong("capacity") ?: 3L).toInt(), doc.getString("status") ?: "waiting", playerData.map { RoomPlayer(it.id, it.getString("username") ?: "unknown", it.getString("rank") ?: "Bronze", it.getTimestamp("joinedAt")?.toDate()?.time ?: 0L) }))
        }
        val roomListener = ref.addSnapshotListener { d, e -> if (e != null) close(e) else { roomData = d; publish() } }
        val playersListener = ref.collection("players").addSnapshotListener { s, e -> if (e != null) close(e) else { playerData = s?.documents.orEmpty(); publish() } }
        awaitClose { roomListener.remove(); playersListener.remove() }
    }
    override fun observePublicRooms(): Flow<List<Room>> = callbackFlow {
        val listener = db.collection("rooms").whereEqualTo("isPublic", true).whereEqualTo("status", "waiting").addSnapshotListener { snapshots, error ->
            if (error != null) close(error) else trySend(snapshots?.documents.orEmpty().map { d -> Room(d.id, d.getString("name") ?: "Room", d.getString("hostId") ?: "", true, (d.getLong("capacity") ?: 3L).toInt(), playerCount = (d.getLong("playerCount") ?: 0L).toInt()) })
        }
        awaitClose { listener.remove() }
    }
}

class FirebaseFriendRepository(private val db: FirebaseFirestore, private val auth: AuthRepository, private val users: UserRepository) : FriendRepository {
    override suspend fun sendRequest(username: String) {
        require(!auth.isGuest) { "Create an account to add friends." }
        val from = auth.currentUserId ?: error("Please sign in again.")
        val target = users.searchUsername(username) ?: error("No user found with that username.")
        require(target.uid != from) { "You can't add yourself." }
        val ref = db.collection("friendRequests").document("${from}_${target.uid}")
        ref.set(mapOf("fromUid" to from, "fromUsername" to (users.getProfile(from)?.username ?: ""), "toUid" to target.uid, "toUsername" to target.username, "status" to "pending", "createdAt" to FieldValue.serverTimestamp())).await()
    }
    override suspend fun respond(requestId: String, accept: Boolean) {
        val current = auth.currentUserId ?: error("Please sign in again.")
        val ref = db.collection("friendRequests").document(requestId)
        db.runTransaction { tx ->
            val request = tx.get(ref)
            if (request.getString("toUid") != current || request.getString("status") != "pending") throw IllegalStateException("This request is no longer available.")
            if (accept) {
                val from = request.getString("fromUid") ?: error("Request is invalid.")
                tx.set(db.collection("users").document(current).collection("friends").document(from), mapOf("uid" to from))
                tx.set(db.collection("users").document(from).collection("friends").document(current), mapOf("uid" to current))
            }
            tx.update(ref, "status", if (accept) "accepted" else "declined")
        }.await()
    }
    override suspend fun removeFriend(friendUid: String) {
        val current = auth.currentUserId ?: return
        db.collection("users").document(current).collection("friends").document(friendUid).delete().await()
        db.collection("users").document(friendUid).collection("friends").document(current).delete().await()
    }
    override fun observeRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val uid = auth.currentUserId ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = db.collection("friendRequests").whereEqualTo("toUid", uid).whereEqualTo("status", "pending").addSnapshotListener { s, e ->
            if (e != null) close(e) else trySend(s?.documents.orEmpty().map { FriendRequest(it.id, it.getString("fromUid") ?: "", it.getString("fromUsername") ?: "", uid, it.getString("toUsername") ?: "") })
        }
        awaitClose { listener.remove() }
    }
    override fun observeFriends(): Flow<List<UserProfile>> = callbackFlow {
        val uid = auth.currentUserId ?: run { trySend(emptyList()); close(); return@callbackFlow }
        var friendIds = emptyList<String>()
        val profileMap = mutableMapOf<String, UserProfile>()
        val registration = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        val listListener = db.collection("users").document(uid).collection("friends").addSnapshotListener { s, e ->
            if (e != null) { close(e); return@addSnapshotListener }
            registration.forEach { it.remove() }; registration.clear(); profileMap.clear()
            friendIds = s?.documents.orEmpty().map { it.id }
            if (friendIds.isEmpty()) trySend(emptyList())
            friendIds.forEach { friendId -> registration += db.collection("users").document(friendId).addSnapshotListener { d, error ->
                if (error != null) close(error) else {
                    val profile = d?.takeIf { it.exists() }?.toUserProfile()
                    if (profile == null) profileMap.remove(friendId) else profileMap[friendId] = profile
                    trySend(friendIds.mapNotNull { profileMap[it] })
                }
            } }
        }
        awaitClose { listListener.remove(); registration.forEach { it.remove() } }
    }
}
