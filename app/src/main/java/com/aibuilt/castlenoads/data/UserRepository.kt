package com.aibuilt.castlenoads.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aibuilt.castlenoads.model.AppSettings
import com.aibuilt.castlenoads.model.ContinueEntry
import com.aibuilt.castlenoads.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.castleDataStore: DataStore<Preferences> by preferencesDataStore(name = "castle_user_data")

class UserRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val WATCHLIST = stringSetPreferencesKey("watchlist")
        val CONTINUE = stringPreferencesKey("continue_watching")
        val PROFILES = stringPreferencesKey("profiles")
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        val SETTINGS = stringPreferencesKey("settings")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    private val defaults: List<Profile> = listOf(Profile(id = "profile-1", name = "Guest", avatarIndex = 0))

    // ---------- onboarding ----------

    val onboarded: Flow<Boolean> = context.castleDataStore.data.map { it[Keys.ONBOARDED] ?: false }

    suspend fun setOnboarded(value: Boolean) {
        context.castleDataStore.edit { it[Keys.ONBOARDED] = value }
    }

    // ---------- profiles ----------

    val profiles: Flow<List<Profile>> = context.castleDataStore.data.map { prefs ->
        val stored = prefs[Keys.PROFILES]
        val decoded = stored?.let { runCatching { json.decodeFromString<List<Profile>>(it) }.getOrNull() }
        if (decoded.isNullOrEmpty()) defaults else decoded
    }

    val activeProfileId: Flow<String> = context.castleDataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_PROFILE] ?: "profile-1"
    }

    suspend fun saveProfiles(profiles: List<Profile>) {
        context.castleDataStore.edit { it[Keys.PROFILES] = json.encodeToString(profiles) }
    }

    suspend fun setActiveProfile(id: String) {
        context.castleDataStore.edit { it[Keys.ACTIVE_PROFILE] = id }
    }

    suspend fun addProfile(profile: Profile, max: Int): Boolean {
        var added = false
        context.castleDataStore.edit { prefs ->
            val stored = prefs[Keys.PROFILES]
            val current = stored?.let { runCatching { json.decodeFromString<List<Profile>>(it) }.getOrNull() }
                ?: defaults
            if (current.size < max) {
                prefs[Keys.PROFILES] = json.encodeToString(current + profile)
                added = true
            }
        }
        return added
    }

    suspend fun deleteProfile(id: String) {
        context.castleDataStore.edit { prefs ->
            val stored = prefs[Keys.PROFILES]
            val current = stored?.let { runCatching { json.decodeFromString<List<Profile>>(it) }.getOrNull() }
                ?: defaults
            val remaining = current.filterNot { it.id == id }.ifEmpty { defaults }
            prefs[Keys.PROFILES] = json.encodeToString(remaining)
            if (prefs[Keys.ACTIVE_PROFILE] == id) {
                prefs[Keys.ACTIVE_PROFILE] = remaining.first().id
            }
        }
    }

    // ---------- watchlist ----------

    val watchlist: Flow<Set<String>> = context.castleDataStore.data.map { it[Keys.WATCHLIST] ?: emptySet() }

    suspend fun toggleWatchlist(titleId: String) {
        context.castleDataStore.edit { prefs ->
            val current = prefs[Keys.WATCHLIST] ?: emptySet()
            prefs[Keys.WATCHLIST] = if (current.contains(titleId)) current - titleId else current + titleId
        }
    }

    // ---------- continue watching ----------

    val continueWatching: Flow<List<ContinueEntry>> = context.castleDataStore.data.map { prefs ->
        val stored = prefs[Keys.CONTINUE]
        stored?.let { runCatching { json.decodeFromString<List<ContinueEntry>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun saveProgress(entry: ContinueEntry) {
        context.castleDataStore.edit { prefs ->
            val stored = prefs[Keys.CONTINUE]
            val current = stored?.let { runCatching { json.decodeFromString<List<ContinueEntry>>(it) }.getOrNull() }
                ?: emptyList()
            val without = current.filterNot { it.key == entry.key }
            val updated = (listOf(entry.copy(updatedAt = System.currentTimeMillis())) + without)
                .sortedByDescending { it.updatedAt }
                .take(20)
            prefs[Keys.CONTINUE] = json.encodeToString(updated)
        }
    }

    suspend fun removeProgress(key: String) {
        context.castleDataStore.edit { prefs ->
            val stored = prefs[Keys.CONTINUE]
            val current = stored?.let { runCatching { json.decodeFromString<List<ContinueEntry>>(it) }.getOrNull() }
                ?: emptyList()
            prefs[Keys.CONTINUE] = json.encodeToString(current.filterNot { it.key == key })
        }
    }

    suspend fun clearProgress() {
        context.castleDataStore.edit { it[Keys.CONTINUE] = json.encodeToString(emptyList<ContinueEntry>()) }
    }

    // ---------- settings ----------

    val settings: Flow<AppSettings> = context.castleDataStore.data.map { prefs ->
        val stored = prefs[Keys.SETTINGS]
        stored?.let { runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull() } ?: AppSettings()
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.castleDataStore.edit { prefs ->
            val stored = prefs[Keys.SETTINGS]
            val current = stored?.let { runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull() }
                ?: AppSettings()
            prefs[Keys.SETTINGS] = json.encodeToString(transform(current))
        }
    }
}
