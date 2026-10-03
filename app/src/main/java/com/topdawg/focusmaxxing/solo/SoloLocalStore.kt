package com.topdawg.focusmaxxing.solo

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SoloLocalStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("solo_study_state", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Synchronized
    fun materials(): List<SoloMaterial> = preferences.getString(KEY_MATERIALS, null)?.let {
        json.decodeFromString(ListSerializer(SoloMaterial.serializer()), it)
    }.orEmpty()

    @Synchronized
    fun saveMaterial(material: SoloMaterial) {
        val materials = materials().filterNot { it.localId == material.localId }
        preferences.edit().putString(KEY_MATERIALS, json.encodeToString(ListSerializer(SoloMaterial.serializer()), listOf(material) + materials)).apply()
    }

    @Synchronized
    fun updateLastPage(materialId: String, page: Int) {
        val updated = materials().map { if (it.localId == materialId) it.copy(lastReadPage = page.coerceIn(1, it.pageCount)) else it }
        preferences.edit().putString(KEY_MATERIALS, json.encodeToString(ListSerializer(SoloMaterial.serializer()), updated)).apply()
    }

    @Synchronized
    fun activeSession(): ActiveSoloSession? = preferences.getString(KEY_ACTIVE_SESSION, null)?.let {
        json.decodeFromString(ActiveSoloSession.serializer(), it)
    }

    @Synchronized
    fun saveActiveSession(session: ActiveSoloSession) {
        preferences.edit().putString(KEY_ACTIVE_SESSION, json.encodeToString(ActiveSoloSession.serializer(), session)).apply()
    }

    @Synchronized
    fun clearActiveSession() {
        preferences.edit().remove(KEY_ACTIVE_SESSION).apply()
    }

    companion object {
        private const val KEY_MATERIALS = "materials"
        private const val KEY_ACTIVE_SESSION = "active_session"
    }
}
