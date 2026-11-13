package com.aibuilt.castlenoads.ui

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.aibuilt.castlenoads.CastleApp
import com.aibuilt.castlenoads.model.AppSettings
import com.aibuilt.castlenoads.model.Category
import com.aibuilt.castlenoads.model.ContinueEntry
import com.aibuilt.castlenoads.model.Playable
import com.aibuilt.castlenoads.model.Profile
import com.aibuilt.castlenoads.model.Title
import com.aibuilt.castlenoads.premium.Entitlements
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class CastleViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as CastleApp).container
    private val catalog = container.catalog
    private val user = container.user
    private val downloads = container.downloads

    val titles: StateFlow<List<Title>?> = catalog.titles
    val loading: StateFlow<Boolean> = catalog.loading
    val error: StateFlow<String?> = catalog.error

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _category = MutableStateFlow<Category?>(null)
    val category: StateFlow<Category?> = _category.asStateFlow()

    val continueWatching: StateFlow<List<Pair<ContinueEntry, Title>>> =
        combine(user.continueWatching, catalog.titles) { entries, list ->
            entries.mapNotNull { entry ->
                list?.firstOrNull { it.id == entry.titleId }?.let { entry to it }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val watchlist: StateFlow<List<Title>> =
        combine(user.watchlist, catalog.titles) { ids, list ->
            list.orEmpty().filter { ids.contains(it.id) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val watchlistIds: StateFlow<Set<String>> =
        user.watchlist.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val profiles: StateFlow<List<Profile>> =
        user.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeProfileId: StateFlow<String> =
        user.activeProfileId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "profile-1")

    val settings: StateFlow<AppSettings> =
        user.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val onboarded: StateFlow<Boolean> =
        user.onboarded.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val downloadList = downloads.downloads
    val preparing = downloads.preparing
    val downloadMessage = downloads.message

    val castConnected = container.cast.connected
    val castDeviceName = container.cast.deviceName
    val castRoutes = container.cast.routes

    val premium = Entitlements.isPremium
    val planName = Entitlements.planName
    val unlocked = Entitlements.unlockedFeatures

    fun refresh() {
        viewModelScope.launch { catalog.load() }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setCategory(value: Category?) {
        _category.value = value
    }

    fun titleById(id: String?): Title? = catalog.byId(id)

    fun results(): List<Title> = catalog.search(_query.value, _category.value)

    fun toggleWatchlist(titleId: String) {
        viewModelScope.launch { user.toggleWatchlist(titleId) }
    }

    fun saveProgress(playable: Playable, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        viewModelScope.launch {
            if (positionMs >= durationMs - 15_000L) {
                user.removeProgress(playable.key)
            } else if (positionMs > 5_000L) {
                user.saveProgress(
                    ContinueEntry(
                        key = playable.key,
                        titleId = playable.titleId,
                        episodeId = playable.episodeId,
                        positionMs = positionMs,
                        durationMs = durationMs
                    )
                )
            }
        }
    }

    fun resumePosition(key: String): Long =
        continueWatching.value.firstOrNull { it.first.key == key }?.first?.positionMs ?: 0L

    fun removeProgress(key: String) {
        viewModelScope.launch { user.removeProgress(key) }
    }

    fun clearProgress() {
        viewModelScope.launch { user.clearProgress() }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { user.updateSettings(transform) }
    }

    fun finishOnboarding() {
        viewModelScope.launch { user.setOnboarded(true) }
    }

    fun addProfile(name: String, kid: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val profile = Profile(
                id = "profile-${UUID.randomUUID()}",
                name = name.trim(),
                avatarIndex = profiles.value.size % AVATARS.size,
                kid = kid
            )
            val added = user.addProfile(profile, Entitlements.maxProfiles())
            onResult(added)
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch { user.deleteProfile(id) }
    }

    fun selectProfile(id: String) {
        viewModelScope.launch { user.setActiveProfile(id) }
    }

    fun download(playable: Playable, titleName: String, wifiOnly: Boolean) {
        downloads.requestDownload(
            key = playable.key,
            videoUrl = playable.videoUrl,
            titleName = titleName,
            subtitleLabel = playable.subtitle,
            wifiOnly = wifiOnly
        )
    }

    fun removeDownload(key: String) = downloads.remove(key)

    fun retryDownload(key: String) = downloads.retry(key)

    fun consumeDownloadMessage() = downloads.consumeMessage()

    @OptIn(UnstableApi::class)
    fun downloadLabel(download: Download): Pair<String, String> =
        downloads.downloadedLabel(download.request)

    fun castLoad(playable: Playable, positionMs: Long, durationMs: Long) {
        container.cast.load(playable.videoUrl, playable.displayName, playable.subtitle, positionMs, durationMs)
    }

    fun castPlay() = container.cast.play()

    fun castPause() = container.cast.pause()

    fun castDisconnect() = container.cast.disconnect()

    fun openCastDialog() = container.cast.startDiscovery()

    fun closeCastDialog() = container.cast.stopDiscovery()

    fun castSelect(routeName: String) = container.cast.selectRouteByName(routeName)

    companion object {
        val AVATARS = listOf("🛡️", "🏰", "🗡️", "👑", "🐉", "🦉")
    }
}
