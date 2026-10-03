package com.topdawg.focusmaxxing.solo.ai

import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfRenderer
import com.topdawg.focusmaxxing.solo.RecallGrade
import com.topdawg.focusmaxxing.solo.SoloConstants
import com.topdawg.focusmaxxing.solo.SoloMaterial
import com.topdawg.focusmaxxing.solo.SoloMaterialFiles
import java.io.File
import java.util.UUID

class FakeRecallAiService : RecallAiService {
    override val isFake = true

    override suspend fun ingest(material: SoloMaterial, topic: String): SoloMaterial {
        val pageCount = if (material.isPdf) {
            val descriptor = ParcelFileDescriptor.open(File(material.localPath), ParcelFileDescriptor.MODE_READ_ONLY)
            descriptor.use { PdfRenderer(it).use(PdfRenderer::getPageCount) }
        } else SoloMaterialFiles.splitTxtIntoPages(File(material.localPath).readText(Charsets.UTF_8)).size
        require(pageCount in 1..SoloConstants.MAX_PDF_PAGES || !material.isPdf) { "PDFs must contain at most 300 pages." }
        return material.copy(
            pageCount = pageCount,
            pageWordCounts = List(pageCount) { SoloConstants.TXT_WORDS_PER_VIRTUAL_PAGE },
            backendMaterialId = "fake-${UUID.randomUUID()}"
        )
    }

    override suspend fun grade(material: SoloMaterial, topic: String, fromPage: Int, toPage: Int, summary: String): RecallGrade {
        val words = Regex("\\b[\\w'-]+\\b").findAll(summary).count()
        return RecallGrade(
            recallScore = if (words >= SoloConstants.MIN_RECALL_WORDS) 70 else 20,
            wordsCovered = if (fromPage in 1..material.pageCount && toPage in fromPage..material.pageCount) {
                material.pageWordCounts.subList(fromPage - 1, toPage).sum()
            } else 0,
            feedback = "FAKE/DEV ONLY: This score is based only on recall length, not material accuracy.",
            keyPointsMissed = emptyList(),
            isFake = true
        )
    }
}
