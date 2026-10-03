package com.topdawg.focusmaxxing.solo.ai

import com.topdawg.focusmaxxing.solo.MaterialIngestResult
import com.topdawg.focusmaxxing.solo.RecallGrade
import com.topdawg.focusmaxxing.solo.SoloMaterial
import com.topdawg.focusmaxxing.solo.SoloConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
private data class MaterialResponseDto(val materialId: String, val pageCount: Int, val pageWordCounts: List<Int>)

@Serializable
private data class GradeRequestDto(val materialId: String, val fromPage: Int, val toPage: Int, val summary: String)

@Serializable
private data class GradeResponseDto(val recallScore: Int, val wordsCovered: Int, val feedback: String, val keyPointsMissed: List<String> = emptyList())

class BackendRecallAiService(
    private val baseUrl: String,
    private val userIdProvider: () -> String?,
    private val tokenProvider: suspend () -> String?
) : RecallAiService {
    override val isFake = false
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(SoloConstants.BACKEND_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(SoloConstants.BACKEND_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(SoloConstants.BACKEND_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override suspend fun ingest(material: SoloMaterial, topic: String): SoloMaterial = withContext(Dispatchers.IO) {
        val result = upload(material, topic)
        material.copy(backendMaterialId = result.backendMaterialId, pageCount = result.pageCount, pageWordCounts = result.pageWordCounts)
    }

    override suspend fun grade(material: SoloMaterial, topic: String, fromPage: Int, toPage: Int, summary: String): RecallGrade = withContext(Dispatchers.IO) {
        var materialId = material.backendMaterialId ?: upload(material, topic).backendMaterialId
        var refreshedId: String? = null
        var response = postGrade(materialId, fromPage, toPage, summary)
        if (response.first == 404 && response.second.isMissingMaterial()) {
            materialId = upload(material, topic).backendMaterialId
            refreshedId = materialId
            response = postGrade(materialId, fromPage, toPage, summary)
        }
        if (response.first !in 200..299) throw IOException(response.second.errorMessage())
        val grade = json.decodeFromString(GradeResponseDto.serializer(), response.second)
        RecallGrade(grade.recallScore, grade.wordsCovered, grade.feedback, grade.keyPointsMissed, refreshedMaterialId = refreshedId)
    }

    private suspend fun upload(material: SoloMaterial, topic: String): MaterialIngestResult {
        val file = File(material.localPath)
        if (!file.isFile) throw IOException("The saved study material is missing from this device.")
        val mediaType = material.mimeType.toMediaType()
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("topic", topic)
            .addFormDataPart("file", material.displayName, file.asRequestBody(mediaType))
            .build()
        val request = authorizedRequest("$baseUrl/materials").post(requestBody).build()
        val (status, body) = execute(request)
        if (status !in 200..299) throw IOException(body.errorMessage())
        val response = json.decodeFromString(MaterialResponseDto.serializer(), body)
        return MaterialIngestResult(response.materialId, response.pageCount, response.pageWordCounts)
    }

    private suspend fun postGrade(materialId: String, fromPage: Int, toPage: Int, summary: String): Pair<Int, String> {
        val body = json.encodeToString(GradeRequestDto.serializer(), GradeRequestDto(materialId, fromPage, toPage, summary))
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = authorizedRequest("$baseUrl/checkpoint/score").post(body).build()
        return execute(request)
    }

    private suspend fun authorizedRequest(url: String): Request.Builder {
        val builder = Request.Builder().url(url)
        val token = tokenProvider()
        if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
        else builder.header("X-Dev-User", userIdProvider() ?: "android-dev-user")
        return builder
    }

    private fun execute(request: Request): Pair<Int, String> = client.newCall(request).execute().use { response ->
        response.code to (response.body?.string().orEmpty())
    }

    private fun String.isMissingMaterial(): Boolean = runCatching {
        Json.parseToJsonElement(this).jsonObject["detail"]?.jsonObject?.get("code")?.jsonPrimitive?.content == "material_not_found"
    }.getOrDefault(false)

    private fun String.errorMessage(): String = runCatching {
        val detail = Json.parseToJsonElement(this).jsonObject["detail"]
        detail?.jsonObject?.get("message")?.jsonPrimitive?.content
            ?: detail?.jsonPrimitive?.content
            ?: "The recall service could not complete the request."
    }.getOrDefault("The recall service could not complete the request.")
}
