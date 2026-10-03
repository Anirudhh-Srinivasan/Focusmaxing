package com.topdawg.focusmaxxing.solo

import kotlinx.serialization.Serializable

@Serializable
data class SoloMaterial(
    val localId: String,
    val displayName: String,
    val localPath: String,
    val mimeType: String,
    val isPdf: Boolean,
    val pageCount: Int,
    val pageWordCounts: List<Int>,
    val backendMaterialId: String? = null,
    val lastReadPage: Int = 1,
    val addedAtMs: Long = 0L
)

@Serializable
data class SoloSegmentRecord(
    val fromPage: Int,
    val toPage: Int,
    val pagesCovered: Int,
    val wordsCovered: Int,
    val activeSeconds: Long,
    val recallScore: Int,
    val paceWpm: Int,
    val interruptions: Int,
    val points: Int,
    val cappedWords: Int = 0,
    val recallFactor: Double = 0.0,
    val focusFactor: Double = 1.0,
    val feedback: String,
    val keyPointsMissed: List<String>,
    val didNotCoverAnything: Boolean
)

@Serializable
data class SoloSessionRecord(
    val sessionId: String,
    val topic: String,
    val materialId: String,
    val materialName: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val durationSeconds: Int,
    val activeSeconds: Long,
    val startPage: Int,
    val lastPageReached: Int,
    val segments: List<SoloSegmentRecord>,
    val totalWordsCovered: Int,
    val interruptions: Int,
    val rawPoints: Int,
    val totalPoints: Int,
    val counted: Boolean,
    val abandoned: Boolean,
    val dateKey: String,
    val xpAfter: Int = 0,
    val rankAfter: String = "Bronze"
)

@Serializable
data class ActiveSoloSession(
    val sessionId: String,
    val material: SoloMaterial,
    val topic: String,
    val durationSeconds: Int,
    val startPage: Int,
    val startedAtMs: Long,
    val accumulatedPauseMs: Long = 0L,
    val pausedAtMs: Long? = null,
    val currentPage: Int = startPage,
    val lastPageReached: Int = startPage,
    val segmentFromPage: Int = startPage,
    val segmentStartActiveMs: Long = 0L,
    val nextCheckpointAtActiveMs: Long,
    val currentSegmentInterruptions: Int = 0,
    val backgroundStartedAtMs: Long? = null,
    val completedSegments: List<SoloSegmentRecord> = emptyList(),
    val checkpointActiveElapsedMs: Long? = null,
    val checkpointIsFinal: Boolean = false,
    val isAbandoned: Boolean = false
)

@Serializable
data class MaterialIngestResult(
    val backendMaterialId: String,
    val pageCount: Int,
    val pageWordCounts: List<Int>
)

@Serializable
data class RecallGrade(
    val recallScore: Int,
    val wordsCovered: Int,
    val feedback: String,
    val keyPointsMissed: List<String>,
    val refreshedMaterialId: String? = null,
    val isFake: Boolean = false
)
