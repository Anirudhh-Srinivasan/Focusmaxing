package com.topdawg.focusmaxxing.solo.ai

import com.topdawg.focusmaxxing.solo.RecallGrade
import com.topdawg.focusmaxxing.solo.SoloMaterial

interface RecallAiService {
    val isFake: Boolean
    suspend fun ingest(material: SoloMaterial, topic: String): SoloMaterial
    suspend fun grade(material: SoloMaterial, topic: String, fromPage: Int, toPage: Int, summary: String): RecallGrade
}
