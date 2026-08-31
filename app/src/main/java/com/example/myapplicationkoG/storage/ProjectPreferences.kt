package com.example.myapplicationkoG.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplicationkoG.domain.model.ClothingDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Stores the active project document as a JSON blob.
 * Binary assets (originals/masks/patches/fabrics) are kept in files via [ImageCache].
 * The DataStore holds structure, references, and parameters only.
 *
 * Note: this is intentionally a single "active project" slot for Part 1.
 * Multi-project management can come later.
 */
private val Context.dataStore by preferencesDataStore(name = "garment_project")

class ProjectPreferences(private val context: Context) {

    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val key: Preferences.Key<String> = stringPreferencesKey("active_document_json")

    val documentFlow: Flow<ClothingDocument?> = context.dataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString<ClothingDocument>(it) }.getOrNull() }
    }

    suspend fun saveDocument(doc: ClothingDocument) {
        val encoded = json.encodeToString(ClothingDocument.serializer(), doc)
        context.dataStore.edit { it[key] = encoded }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(key) }
    }
}
