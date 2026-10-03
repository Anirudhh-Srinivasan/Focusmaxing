package com.topdawg.focusmaxxing.solo

import com.topdawg.focusmaxxing.data.UserProfile
import java.time.LocalDate

data class SessionAccountingResult(val profile: UserProfile, val session: SoloSessionRecord)

object SoloSessionAccounting {
    fun award(profile: UserProfile, session: SoloSessionRecord): SessionAccountingResult {
        val date = LocalDate.parse(session.dateKey)
        val qualifies = session.activeSeconds >= SoloConstants.MIN_COUNTED_SESSION_MINUTES * 60L
        val alreadyToday = if (profile.soloPointsDate == session.dateKey) profile.soloPointsToday else 0
        val points = if (qualifies) SoloScoring.sessionPoints(session.rawPoints, session.activeSeconds, alreadyToday) else 0
        val xp = profile.xp + points
        val nextStreak = if (qualifies) {
            val lastDate = profile.lastCountedSessionDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            SoloScoring.streakAfterCountedSession(profile.streak, lastDate, date)
        } else profile.streak
        val updatedProfile = profile.copy(
            xp = xp,
            rank = SoloScoring.rankForXp(xp).label,
            sessionsPlayed = profile.sessionsPlayed + if (qualifies) 1 else 0,
            streak = nextStreak,
            lastCountedSessionDate = if (qualifies) session.dateKey else profile.lastCountedSessionDate,
            soloPointsDate = if (qualifies) session.dateKey else profile.soloPointsDate,
            soloPointsToday = if (qualifies) alreadyToday + points else profile.soloPointsToday
        )
        return SessionAccountingResult(updatedProfile, session.copy(totalPoints = points, counted = qualifies))
    }
}
