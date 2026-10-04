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
        assertEquals(0, SoloScoring.scoreSegment(1_000, 600, 44, 0).points)
        assertEquals(0, SoloScoring.scoreSegment(1_000, 600, 35, 0).points)
        assertEquals(9, SoloScoring.scoreSegment(1_000, 600, 45, 0).points)
    }

    @Test fun reflectedPageWordsPreventWideRangeInflation() {
        val pageWords = listOf(247, 226, 198, 212, 213, 207, 203, 275)
        val pageOneClaimedAsEight = SoloScoring.creditedWords(pageWords, 1, 8, listOf(1))
        val honestPagesOneToThree = SoloScoring.creditedWords(pageWords, 1, 3, listOf(1, 2, 3))
        assertEquals(247, pageOneClaimedAsEight)
        assertEquals(671, honestPagesOneToThree)
        val inflatedClaimScore = SoloScoring.scoreSegment(pageOneClaimedAsEight, 60, 80, 0).points
        val honestRangeScore = SoloScoring.scoreSegment(honestPagesOneToThree, 60, 75, 0).points
        assertEquals(4, inflatedClaimScore)
        assertEquals(6, honestRangeScore)
        assertEquals(0, SoloScoring.creditedWords(pageWords, 1, 8, emptyList()))
    }

    @Test fun zeroPointReasonExplainsLowRecallOrNoCreditedPages() {
        assertEquals("Recall too low for points on this one (minimum 45).", SoloScoring.zeroPointsReason(35, 1))
        assertEquals("Your summary didn't match the pages you picked.", SoloScoring.zeroPointsReason(70, 0))
        assertEquals(null, SoloScoring.zeroPointsReason(70, 1))
    }

    @Test fun debugFastModeIsAbsentFromReleaseConfiguration() {
        if (com.topdawg.focusmaxxing.BuildConfig.DEBUG) {
            assertEquals(true, 2 in SoloConstants.SESSION_LENGTHS_MINUTES)
            assertEquals(30, SoloConstants.CHECKPOINT_INTERVAL_MIN_SECONDS)
            assertEquals(45, SoloConstants.CHECKPOINT_INTERVAL_MAX_SECONDS)
            assertEquals(20, SoloConstants.MIN_REMAINING_SECONDS_FOR_CHECKPOINT)
            assertEquals(60, SoloConstants.MIN_COUNTED_SESSION_SECONDS)
        } else {
            assertEquals(false, 2 in SoloConstants.SESSION_LENGTHS_MINUTES)
            assertEquals(600, SoloConstants.CHECKPOINT_INTERVAL_MIN_SECONDS)
            assertEquals(900, SoloConstants.CHECKPOINT_INTERVAL_MAX_SECONDS)
            assertEquals(300, SoloConstants.MIN_REMAINING_SECONDS_FOR_CHECKPOINT)
            assertEquals(600, SoloConstants.MIN_COUNTED_SESSION_SECONDS)
        }
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
        assertEquals(0, SoloScoring.sessionPoints(300, (SoloConstants.MIN_COUNTED_SESSION_SECONDS - 1).toLong(), 0))
        assertEquals(150, SoloScoring.sessionPoints(300, SoloConstants.MIN_COUNTED_SESSION_SECONDS.toLong(), 450))
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
