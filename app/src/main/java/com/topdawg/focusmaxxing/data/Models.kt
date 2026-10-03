package com.topdawg.focusmaxxing.data

import com.google.firebase.firestore.DocumentSnapshot

data class UserProfile(
    val uid: String,
    val username: String,
    val isGuest: Boolean,
    val rank: String = "Bronze",
    val rp: Int = 0,
    val xp: Int = 0,
    val streak: Int = 0,
    val battlesPlayed: Int = 0
)

data class Room(
    val code: String,
    val name: String,
    val hostId: String,
    val isPublic: Boolean,
    val capacity: Int,
    val status: String = "waiting",
    val players: List<RoomPlayer> = emptyList(),
    val playerCount: Int = players.size
)

data class RoomPlayer(val uid: String, val username: String, val rank: String, val joinedAt: Long)

data class FriendRequest(val id: String, val fromUid: String, val fromUsername: String, val toUid: String, val toUsername: String)

fun DocumentSnapshot.toUserProfile(): UserProfile = UserProfile(
    uid = id,
    username = getString("username") ?: "unknown",
    isGuest = getBoolean("isGuest") ?: false,
    rank = getString("dawgRank") ?: "Bronze",
    rp = getLong("rp")?.toInt() ?: 0,
    xp = getLong("xp")?.toInt() ?: 0,
    streak = getLong("streak")?.toInt() ?: 0,
    battlesPlayed = getLong("battlesPlayed")?.toInt() ?: 0
)
