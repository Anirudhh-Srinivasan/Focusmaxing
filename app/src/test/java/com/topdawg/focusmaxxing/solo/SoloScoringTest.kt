package com.topdawg.focusmaxxing.solo

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SoloScoringTest {
    @Test fun capsClaimedWordsAtMaximumReadingRate() {
        val score = SoloScoring.scoreSegment(words = 1_000, activeSeconds = 60, recallScore = 100, interruptions = 0)
        assertEquals(400, score.cappedWords)
        assertEquals(8, score.points)
    }

    @Test fun recallBelowThresholdEarnsZero() {
        assertEquals(0, SoloScoring.scoreSegment(1_000, 600, 29, 0).points)
    }

    @Test fun focusFactorCannotFallBelowHalf() {
        val score = SoloScoring.scoreSegment(10_000, 600, 100, 20)
        assertEquals(0.5, score.focusFactor, 0.00001)
        assertEquals(40, score.points)
    }

    @Test fun roundsSegmentPoints() {
        val score = SoloScoring.scoreSegment(words = 100, activeSeconds = 60, recallScore = 100, interruptions = 0)
        assertEquals(2, score.points)
    }

    @Test fun shortSessionsDoNotCountAndDailyCapIsEnforced() {
        assertEquals(0, SoloScoring.sessionPoints(300, 599, 0))
        assertEquals(150, SoloScoring.sessionPoints(300, 600, 450))
        assertEquals(0, SoloScoring.sessionPoints(300, 3_600, 600))
    }

    @Test fun rankBoundariesAndProgressAreDerivedFromXp() {
        assertEquals(DawgRank.BRONZE, SoloScoring.rankForXp(999))
        assertEquals(DawgRank.SILVER, SoloScoring.rankForXp(1_000))
        assertEquals(DawgRank.GRANDMASTER, SoloScoring.rankForXp(25_000))
        val progress = SoloScoring.rankProgress(1_500)
        assertEquals(0.3333, progress.progressWithinRank.toDouble(), 0.001)
        assertEquals(1_000, progress.xpToNextRank)
    }

    @Test fun streakOnlyContinuesAcrossConsecutiveCountedDays() {
        val today = LocalDate.of(2026, 10, 4)
        assertEquals(4, SoloScoring.streakAfterCountedSession(3, today.minusDays(1), today))
        assertEquals(3, SoloScoring.streakAfterCountedSession(3, today, today))
        assertEquals(1, SoloScoring.streakAfterCountedSession(9, today.minusDays(2), today))
    }
}
