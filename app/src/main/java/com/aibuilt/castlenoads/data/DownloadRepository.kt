package com.aibuilt.castlenoads.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.aibuilt.castlenoads.CastleApp
import com.aibuilt.castlenoads.player.CastleDownloadService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@UnstableApi
class DownloadRepository(private val context: Context) {

    private val container get() = (context.applicationContext as CastleApp).container

    private val _downloads = MutableStateFlow<List<Download>>(emptyList())
    val downloads: StateFlow<List<Download>> = _downloads.asStateFlow()

    private val _preparing = MutableStateFlow<Set<String>>(emptySet())
    val preparing: StateFlow<Set<String>> = _preparing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val listener = object : DownloadManager.Listener {
        override fun onInitialized(downloadManager: DownloadManager) {
            _downloads.value = downloadManager.currentDownloads
        }

        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            _downloads.value = downloadManager.currentDownloads
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            _downloads.value = downloadManager.currentDownloads
        }

        override fun onDownloadsPausedChanged(downloadManager: DownloadManager, downloadsPaused: Boolean) = Unit

        override fun onIdle(downloadManager: DownloadManager) = Unit

        override fun onWaitingForRequirementsChanged(
            downloadManager: DownloadManager,
            waitingForRequirements: Boolean
        ) = Unit
    }

    init {
        container.downloadManager.addListener(listener)
        _downloads.value = container.downloadManager.currentDownloads
    }

    fun isDownloaded(key: String): Boolean =
        _downloads.value.any { it.request.id == key && it.state == Download.STATE_COMPLETED }

    fun downloadFor(key: String): Download? = _downloads.value.firstOrNull { it.request.id == key }

    fun onWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun requestDownload(key: String, videoUrl: String, titleName: String, subtitleLabel: String, wifiOnly: Boolean) {
        if (_downloads.value.any { it.request.id == key }) {
            _message.value = "Already in your downloads."
            return
        }
        if (wifiOnly && !onWifi()) {
            _message.value = "Waiting for Wi-Fi — the download will start when you reconnect."
        }
        _preparing.value = _preparing.value + key
        container.prepareDownload(
            videoUrl = videoUrl,
            key = key,
            dataLabel = "$titleName\u0000$subtitleLabel",
            onReady = { request ->
                _preparing.value = _preparing.value - key
                DownloadService.sendAddDownload(context, CastleDownloadService::class.java, request, true)
            },
            onError = { error ->
                _preparing.value = _preparing.value - key
                _message.value = "Download failed: $error"
            }
        )
    }

    fun remove(key: String) {
        DownloadService.sendRemoveDownload(context, CastleDownloadService::class.java, key, false)
    }

    fun retry(key: String) {
        val download = downloadFor(key) ?: return
        DownloadService.sendAddDownload(
            context,
            CastleDownloadService::class.java,
            download.request,
            true
        )
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun downloadedLabel(request: DownloadRequest): Pair<String, String> {
        val raw = request.data?.let { String(it) }.orEmpty()
        val parts = raw.split("\u0000")
        return Pair(parts.getOrNull(0).orEmpty().ifBlank { "Title" }, parts.getOrNull(1).orEmpty())
    }
}
