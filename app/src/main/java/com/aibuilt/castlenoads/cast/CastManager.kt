package com.aibuilt.castlenoads.cast

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class CastManager(private val context: Context) {

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()

    private val _routes = MutableStateFlow<List<String>>(emptyList())
    val routes: StateFlow<List<String>> = _routes.asStateFlow()

    private val router: MediaRouter? = runCatching {
        MediaRouter.getInstance(context.applicationContext)
    }.getOrNull()

    private val selector: MediaRouteSelector? = runCatching {
        MediaRouteSelector.Builder()
            .addControlCategory(
                CastMediaControlIntent.categoryForCast(CastOptionsProvider.CAST_RECEIVER_ID)
            )
            .build()
    }.getOrNull()

    private val listener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            _connected.value = true
            _deviceName.value = session.castDevice?.friendlyName
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            _connected.value = true
            _deviceName.value = session.castDevice?.friendlyName
        }

        override fun onSessionEnding(session: CastSession) = Unit

        override fun onSessionEnded(session: CastSession, error: Int) {
            _connected.value = false
            _deviceName.value = null
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionStartFailed(session: CastSession, error: Int) = Unit
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionResumeFailed(session: CastSession, error: Int) = Unit
    }

    private val routeCallback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = publishRoutes()
        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = publishRoutes()
        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = publishRoutes()
    }

    init {
        runCatching {
            CastContext.getSharedInstance(context).sessionManager
                .addSessionManagerListener(listener, CastSession::class.java)
        }
        runCatching {
            val session = CastContext.getSharedInstance(context).sessionManager.currentCastSession
            if (session != null) {
                _connected.value = true
                _deviceName.value = session.castDevice?.friendlyName
            }
        }
    }

    private fun currentSession(): CastSession? = runCatching {
        CastContext.getSharedInstance(context).sessionManager.currentCastSession
    }.getOrNull()

    fun castDeviceName(): String? = currentSession()?.castDevice?.friendlyName

    // ---------- device discovery used by the cast screen ----------

    fun startDiscovery() {
        val mediaRouter = router ?: return
        val mediaSelector = selector ?: return
        runCatching {
            mediaRouter.addCallback(
                mediaSelector,
                routeCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
            )
        }
        publishRoutes()
    }

    fun stopDiscovery() {
        runCatching { router?.removeCallback(routeCallback) }
        _routes.value = emptyList()
    }

    fun selectRouteByName(name: String) {
        val mediaRouter = router ?: return
        runCatching {
            mediaRouter.routes.firstOrNull { it.name == name }?.let { mediaRouter.selectRoute(it) }
        }
    }

    private fun publishRoutes() {
        val mediaRouter = router
        val mediaSelector = selector
        if (mediaRouter == null || mediaSelector == null) {
            _routes.value = emptyList()
            return
        }
        _routes.value = runCatching {
            mediaRouter.routes
                .filter { route -> runCatching { route.matchesSelector(mediaSelector) }.getOrDefault(false) }
                .map { route -> route.name }
        }.getOrDefault(emptyList())
    }

    fun disconnect() {
        runCatching {
            CastContext.getSharedInstance(context).sessionManager.endCurrentSession(true)
        }
        _connected.value = false
        _deviceName.value = null
    }

    fun load(url: String, title: String, subtitle: String, positionMs: Long, durationMs: Long) {
        val client = currentSession()?.remoteMediaClient ?: return
        val contentType = if (url.contains(".m3u8")) "application/vnd.apple.mpegurl" else "video/mp4"
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        metadata.putString(MediaMetadata.KEY_TITLE, title)
        metadata.putString(MediaMetadata.KEY_SUBTITLE, subtitle)
        val info = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(contentType)
            .setStreamDuration(durationMs)
            .setMetadata(metadata)
            .build()
        val request = MediaLoadRequestData.Builder()
            .setMediaInfo(info)
            .setAutoplay(true)
            .setCurrentTime(positionMs)
            .build()
        runCatching { client.load(request) }
    }

    fun play() {
        runCatching { currentSession()?.remoteMediaClient?.play() }
    }

    fun pause() {
        runCatching { currentSession()?.remoteMediaClient?.pause() }
    }

    fun position(): Long = runCatching {
        currentSession()?.remoteMediaClient?.approximateStreamPosition
    }.getOrNull() ?: 0L
}
