package com.aibuilt.castlenoads

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.aibuilt.castlenoads.cast.CastManager
import com.aibuilt.castlenoads.data.CatalogRepository
import com.aibuilt.castlenoads.data.DownloadRepository
import com.aibuilt.castlenoads.data.UserRepository
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
class AppContainer(private val context: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val cacheDir: File = File(context.filesDir, "media_cache").apply { mkdirs() }
    private val downloadIndexDir: File = File(context.filesDir, "download_index").apply { mkdirs() }

    val databaseProvider = StandaloneDatabaseProvider(context)

    val downloadCache: SimpleCache = SimpleCache(cacheDir, NoOpCacheEvictor(), databaseProvider)

    private val upstreamFactory = DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory())

    val dataSourceFactory: CacheDataSource.Factory = CacheDataSource.Factory()
        .setCache(downloadCache)
        .setUpstreamDataSourceFactory(upstreamFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    val downloadManager: DownloadManager = DownloadManager(
        context,
        databaseProvider,
        downloadCache,
        dataSourceFactory,
        MoreExecutors.directExecutor()
    ).apply {
        resumeDownloads()
    }

    val catalog = CatalogRepository(context)
    val user = UserRepository(context)
    val cast = CastManager(context)
    val downloads = DownloadRepository(context)

    fun createPlayer(playerContext: Context, maxHeight: Int = Int.MAX_VALUE): ExoPlayer {
        val selector = DefaultTrackSelector(playerContext).apply {
            parameters = buildUponParameters()
                .setMaxVideoSize(Int.MAX_VALUE, maxHeight)
                .build()
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(playerContext)
            .setDataSourceFactory(dataSourceFactory)
        return ExoPlayer.Builder(playerContext)
            .setTrackSelector(selector)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    /**
     * A download request is built straight from the stream URL. HLS playlists get the HLS mime type
     * so the download manager picks the streaming downloader, everything else downloads as a file.
     */
    fun buildDownloadRequest(videoUrl: String, key: String, dataLabel: String): DownloadRequest {
        val mimeType = if (videoUrl.contains(".m3u8", ignoreCase = true)) {
            MimeTypes.APPLICATION_M3U8
        } else {
            MimeTypes.VIDEO_MP4
        }
        return DownloadRequest.Builder(key, Uri.parse(videoUrl))
            .setMimeType(mimeType)
            .setData(dataLabel.toByteArray())
            .build()
    }

    fun prepareDownload(
        videoUrl: String,
        key: String,
        dataLabel: String,
        onReady: (DownloadRequest) -> Unit,
        onError: (String) -> Unit
    ) {
        appScope.launch {
            runCatching { buildDownloadRequest(videoUrl, key, dataLabel) }
                .onSuccess { onReady(it) }
                .onFailure { onError(it.message ?: "unknown error") }
        }
    }

    @Suppress("unused")
    fun indexDir(): File = downloadIndexDir
}

class CastleApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SingletonImageLoader.setSafe { appContext ->
            ImageLoader.Builder(appContext)
                .components { add(OkHttpNetworkFetcherFactory()) }
                .build()
        }
    }
}
