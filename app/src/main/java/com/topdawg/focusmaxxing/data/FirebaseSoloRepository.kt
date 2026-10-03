package com.topdawg.focusmaxxing.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.topdawg.focusmaxxing.solo.SoloConstants
import com.topdawg.focusmaxxing.solo.SoloSegmentRecord
import com.topdawg.focusmaxxing.solo.SoloSessionAccounting
import com.topdawg.focusmaxxing.solo.SoloSessionRecord
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseSoloRepository(private val db: FirebaseFirestore) : SoloRepository {
    override suspend fun finishSession(uid: String, session: SoloSessionRecord): SoloSessionRecord {
        val userRef = db.collection("users").document(uid)
        val sessionRef = userRef.collection("sessions").document(session.sessionId)
        return db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val savedSnapshot = transaction.get(sessionRef)
            if (savedSnapshot.exists()) return@runTransaction savedSnapshot.toSoloSession()
            if (!userSnapshot.exists()) throw IllegalStateException("Your profile is unavailable.")
            val accounting = SoloSessionAccounting.award(userSnapshot.toUserProfile(), session)
            transaction.set(sessionRef, accounting.session.toFirestoreMap())
            transaction.update(userRef, mapOf(
                "xp" to accounting.profile.xp,
                "dawgRank" to accounting.profile.rank,
                "sessionsPlayed" to accounting.profile.sessionsPlayed,
                "streak" to accounting.profile.streak,
                "lastCountedSessionDate" to accounting.profile.lastCountedSessionDate,
                "soloPointsDate" to accounting.profile.soloPointsDate,
                "soloPointsToday" to accounting.profile.soloPointsToday
            ))
            accounting.session
        }.await()
    }

    override fun observeRecentSessions(uid: String): Flow<List<SoloSessionRecord>> = callbackFlow {
        val listener = db.collection("users").document(uid).collection("sessions")
            .orderBy("endedAtMs", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(SoloConstants.HISTORY_LIMIT.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else trySend(snapshot?.documents.orEmpty().map { it.toSoloSession() })
            }
        awaitClose { listener.remove() }
    }

    private fun SoloSessionRecord.toFirestoreMap(): Map<String, Any?> = mapOf(
        "sessionId" to sessionId,
        "topic" to topic,
        "materialId" to materialId,
        "materialName" to materialName,
        "startedAtMs" to startedAtMs,
        "endedAtMs" to endedAtMs,
        "durationSeconds" to durationSeconds,
        "activeSeconds" to activeSeconds,
        "startPage" to startPage,
        "lastPageReached" to lastPageReached,
        "segments" to segments.map { segment -> mapOf(
            "fromPage" to segment.fromPage,
            "toPage" to segment.toPage,
            "pagesCovered" to segment.pagesCovered,
            "wordsCovered" to segment.wordsCovered,
            "activeSeconds" to segment.activeSeconds,
            "recallScore" to segment.recallScore,
            "paceWpm" to segment.paceWpm,
            "interruptions" to segment.interruptions,
            "points" to segment.points,
            "feedback" to segment.feedback,
            "keyPointsMissed" to segment.keyPointsMissed,
            "didNotCoverAnything" to segment.didNotCoverAnything
        ) },
        "totalWordsCovered" to totalWordsCovered,
        "interruptions" to interruptions,
        "rawPoints" to rawPoints,
        "totalPoints" to totalPoints,
        "counted" to counted,
        "abandoned" to abandoned,
        "dateKey" to dateKey,
        "xpAfter" to xpAfter,
        "rankAfter" to rankAfter,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun DocumentSnapshot.toSoloSession(): SoloSessionRecord {
        val segmentMaps = get("segments") as? List<*> ?: emptyList<Any>()
        return SoloSessionRecord(
            sessionId = getString("sessionId") ?: id,
            topic = getString("topic") ?: "",
            materialId = getString("materialId") ?: "",
            materialName = getString("materialName") ?: "",
            startedAtMs = getLong("startedAtMs") ?: 0L,
            endedAtMs = getLong("endedAtMs") ?: 0L,
            durationSeconds = (getLong("durationSeconds") ?: 0L).toInt(),
            activeSeconds = getLong("activeSeconds") ?: 0L,
            startPage = (getLong("startPage") ?: 1L).toInt(),
            lastPageReached = (getLong("lastPageReached") ?: 1L).toInt(),
            segments = segmentMaps.mapNotNull { raw ->
                val map = raw as? Map<*, *> ?: return@mapNotNull null
                SoloSegmentRecord(
                    fromPage = map.long("fromPage").toInt(),
                    toPage = map.long("toPage").toInt(),
                    pagesCovered = map.long("pagesCovered").toInt(),
                    wordsCovered = map.long("wordsCovered").toInt(),
                    activeSeconds = map.long("activeSeconds"),
                    recallScore = map.long("recallScore").toInt(),
                    paceWpm = map.long("paceWpm").toInt(),
                    interruptions = map.long("interruptions").toInt(),
                    points = map.long("points").toInt(),
                    feedback = map["feedback"] as? String ?: "",
                    keyPointsMissed = (map["keyPointsMissed"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                    didNotCoverAnything = map["didNotCoverAnything"] as? Boolean ?: false
                )
            },
            totalWordsCovered = (getLong("totalWordsCovered") ?: 0L).toInt(),
            interruptions = (getLong("interruptions") ?: 0L).toInt(),
            rawPoints = (getLong("rawPoints") ?: 0L).toInt(),
            totalPoints = (getLong("totalPoints") ?: 0L).toInt(),
            counted = getBoolean("counted") ?: false,
            abandoned = getBoolean("abandoned") ?: false,
            dateKey = getString("dateKey") ?: "1970-01-01",
            xpAfter = (getLong("xpAfter") ?: 0L).toInt(),
            rankAfter = getString("rankAfter") ?: "Bronze"
        )
    }

    private fun Map<*, *>.long(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L
}
