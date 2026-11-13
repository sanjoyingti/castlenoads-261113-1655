package com.aibuilt.castlenoads.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.aibuilt.castlenoads.CastleApp
import com.aibuilt.castlenoads.R

@UnstableApi
class CastleDownloadService : DownloadService(
    FOREGROUND_ID,
    FOREGROUND_UPDATE_INTERVAL_MS,
    CHANNEL_ID,
    R.string.download_channel_name,
    R.string.download_channel_name
) {

    override fun getDownloadManager(): DownloadManager =
        (application as CastleApp).container.downloadManager

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        ensureChannel()
        val active = downloads.count { it.state == Download.STATE_DOWNLOADING }
        val queued = downloads.count { it.state == Download.STATE_QUEUED }
        val text = when {
            notMetRequirements != 0 -> "Waiting for a connection"
            active > 0 -> "Downloading $active title(s)…"
            queued > 0 -> "$queued title(s) queued"
            else -> "Preparing your download"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_notification)
            .setContentTitle(getString(R.string.download_channel_name))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setProgress(0, 0, active > 0)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.setShowBadge(false)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "castle_downloads"
        private const val FOREGROUND_ID = 4711
        private const val NOTIFICATION_ID = 4712
        private const val FOREGROUND_UPDATE_INTERVAL_MS = 500L
    }
}
