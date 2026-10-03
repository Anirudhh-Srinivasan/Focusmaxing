package com.topdawg.focusmaxxing.solo

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfRenderer
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class SoloMaterialFiles(private val context: Context, private val store: SoloLocalStore) {
    fun import(uri: Uri): SoloMaterial {
        val resolver = context.contentResolver
        val displayName = resolver.displayName(uri) ?: "Study material"
        val extension = displayName.substringAfterLast('.', "").lowercase()
        require(extension == "pdf" || extension == "txt") { "Choose a PDF or TXT file." }
        val mimeType = if (extension == "pdf") "application/pdf" else "text/plain"
        val directory = File(context.filesDir, "solo/materials").apply { mkdirs() }
        val localId = UUID.randomUUID().toString()
        val destination = File(directory, "$localId.$extension")
        try {
            val input = resolver.openInputStream(uri) ?: error("Could not open the selected file.")
            input.use { source ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var totalBytes = 0
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        totalBytes += read
                        require(totalBytes <= SoloConstants.MAX_UPLOAD_BYTES) { "Files must be no larger than 20 MB." }
                        output.write(buffer, 0, read)
                    }
                }
            }
            val pages: Int
            val counts: List<Int>
            if (extension == "pdf") {
                val descriptor = ParcelFileDescriptor.open(destination, ParcelFileDescriptor.MODE_READ_ONLY)
                descriptor.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        pages = renderer.pageCount
                        require(pages in 1..SoloConstants.MAX_PDF_PAGES) { "PDFs must contain between 1 and 300 pages." }
                    }
                }
                counts = List(pages) { 0 }
            } else {
                val text = destination.readText(Charsets.UTF_8)
                val virtualPages = splitTxtIntoPages(text)
                pages = virtualPages.size
                counts = virtualPages.map(::countWords)
            }
            return SoloMaterial(localId, displayName, destination.absolutePath, mimeType, extension == "pdf", pages, counts, addedAtMs = System.currentTimeMillis()).also(store::saveMaterial)
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
    }

    fun readTxtPages(material: SoloMaterial): List<String> = splitTxtIntoPages(File(material.localPath).readText(Charsets.UTF_8))

    private fun ContentResolver.displayName(uri: Uri): String? = query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

    companion object {
        fun splitTxtIntoPages(text: String): List<String> {
            val words = text.split(Regex("\\s+")).filter(String::isNotBlank)
            require(words.isNotEmpty()) { "The text file is empty." }
            return words.chunked(SoloConstants.TXT_WORDS_PER_VIRTUAL_PAGE).map { it.joinToString(" ") }
        }
        fun countWords(text: String): Int = Regex("\\b[\\w'-]+\\b").findAll(text).count()
    }
}
