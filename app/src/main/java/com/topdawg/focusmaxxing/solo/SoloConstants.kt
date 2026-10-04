package com.topdawg.focusmaxxing.solo

/** Tune all solo scoring thresholds and rank boundaries here. */
object SoloConstants {
    const val MAX_UPLOAD_BYTES = 20 * 1024 * 1024
    const val MAX_PDF_PAGES = 300
    const val TXT_WORDS_PER_VIRTUAL_PAGE = 300
    const val MIN_SESSION_MINUTES = 25
    const val MAX_SESSION_MINUTES = 90
    val SESSION_LENGTHS_MINUTES = listOf(25, 45, 60, 90)
    const val COUNTDOWN_SECONDS = 3
    const val MIN_CHECKPOINT_INTERVAL_MINUTES = 10
    const val MAX_CHECKPOINT_INTERVAL_MINUTES = 15
    const val MIN_REMAINING_MINUTES_FOR_CHECKPOINT = 5
    const val MAX_PAGES_PER_CHECKPOINT = 60
    const val MIN_RECALL_SENTENCES = 2
    const val MAX_RECALL_SENTENCES = 4
    const val MIN_RECALL_WORDS = 15
    const val MAX_RECALL_WORDS = 150
    const val PDF_BITMAP_CACHE_BYTES = 32 * 1024 * 1024
    const val HISTORY_LIMIT = 10
    const val SESSION_TICK_MS = 1_000L
    const val NOTIFICATION_CHANNEL_ID = "solo-checkpoints"
    const val NOTIFICATION_ID = 3107
    const val BACKEND_CONNECT_TIMEOUT_SECONDS = 30L
    const val BACKEND_READ_TIMEOUT_SECONDS = 90L
    const val BACKEND_WRITE_TIMEOUT_SECONDS = 60L

    const val MAX_WPM = 400
    const val MIN_RECALL_SCORE_FOR_POINTS = 45
    const val FOCUS_PENALTY_PER_INTERRUPTION = 0.05
    const val MIN_FOCUS_FACTOR = 0.5
    const val WORDS_PER_POINT = 50.0
    const val MIN_COUNTED_SESSION_MINUTES = 10
    const val MAX_POINTS_PER_DAY = 600
    const val INTERRUPTION_BACKGROUND_SECONDS = 30

    const val BRONZE_XP = 0
    const val SILVER_XP = 1_000
    const val GOLD_XP = 2_500
    const val PLATINUM_XP = 5_000
    const val DIAMOND_XP = 9_000
    const val MASTER_XP = 15_000
    const val GRANDMASTER_XP = 25_000
}

enum class DawgRank(val label: String, val minimumXp: Int) {
    BRONZE("Bronze", SoloConstants.BRONZE_XP),
    SILVER("Silver", SoloConstants.SILVER_XP),
    GOLD("Gold", SoloConstants.GOLD_XP),
    PLATINUM("Platinum", SoloConstants.PLATINUM_XP),
    DIAMOND("Diamond", SoloConstants.DIAMOND_XP),
    MASTER("Master", SoloConstants.MASTER_XP),
    GRANDMASTER("Grandmaster", SoloConstants.GRANDMASTER_XP)
}

data class RankProgress(
    val rank: DawgRank,
    val progressWithinRank: Float,
    val xpToNextRank: Int,
    val nextRank: DawgRank?
)

data class SegmentScore(
    val words: Int,
    val activeSeconds: Long,
    val cappedWords: Int,
    val recallFactor: Double,
    val focusFactor: Double,
    val points: Int
)

object SoloScoring {
    fun creditedWords(pageWordCounts: List<Int>, fromPage: Int, toPage: Int, pagesReflected: List<Int>): Int {
        val first = fromPage.coerceAtLeast(1)
        val last = toPage.coerceAtMost(pageWordCounts.size)
        if (last < first) return 0
        return pagesReflected.asSequence().filter { it in first..last }.distinct().sumOf { pageWordCounts[it - 1].coerceAtLeast(0) }
    }

    fun scoreSegment(words: Int, activeSeconds: Long, recallScore: Int, interruptions: Int): SegmentScore {
        val safeWords = words.coerceAtLeast(0)
        val safeSeconds = activeSeconds.coerceAtLeast(0L)
        val safeRecall = recallScore.coerceIn(0, 100)
        val safeInterruptions = interruptions.coerceAtLeast(0)
        val cappedWords = minOf(safeWords.toDouble(), safeSeconds / 60.0 * SoloConstants.MAX_WPM).toInt()
        val recallFactor = if (safeRecall < SoloConstants.MIN_RECALL_SCORE_FOR_POINTS) 0.0 else safeRecall / 100.0
        val focusFactor = maxOf(
            SoloConstants.MIN_FOCUS_FACTOR,
            1.0 - SoloConstants.FOCUS_PENALTY_PER_INTERRUPTION * safeInterruptions
        )
        val points = kotlin.math.round(cappedWords * recallFactor * focusFactor / SoloConstants.WORDS_PER_POINT).toInt()
        return SegmentScore(safeWords, safeSeconds, cappedWords, recallFactor, focusFactor, points)
    }

    fun sessionPoints(proposedPoints: Int, activeSeconds: Long, pointsAlreadyEarnedToday: Int): Int {
        if (activeSeconds < SoloConstants.MIN_COUNTED_SESSION_MINUTES * 60L) return 0
        val remainingToday = (SoloConstants.MAX_POINTS_PER_DAY - pointsAlreadyEarnedToday.coerceAtLeast(0)).coerceAtLeast(0)
        return proposedPoints.coerceAtLeast(0).coerceAtMost(remainingToday)
    }

    fun rankForXp(xp: Int): DawgRank = DawgRank.entries.last { xp.coerceAtLeast(0) >= it.minimumXp }

    fun rankProgress(xp: Int): RankProgress {
        val safeXp = xp.coerceAtLeast(0)
        val rank = rankForXp(safeXp)
        val next = DawgRank.entries.getOrNull(rank.ordinal + 1)
            ?: return RankProgress(rank, 1f, 0, null)
        val tierSpan = next.minimumXp - rank.minimumXp
        val earnedInTier = safeXp - rank.minimumXp
        return RankProgress(
            rank = rank,
            progressWithinRank = (earnedInTier.toFloat() / tierSpan).coerceIn(0f, 1f),
            xpToNextRank = next.minimumXp - safeXp,
            nextRank = next
        )
    }

    fun streakAfterCountedSession(previousStreak: Int, lastCountedDate: java.time.LocalDate?, today: java.time.LocalDate): Int = when {
        lastCountedDate == today -> previousStreak.coerceAtLeast(1)
        lastCountedDate == today.minusDays(1) -> previousStreak.coerceAtLeast(0) + 1
        else -> 1
    }
}
