package com.aibuilt.castlenoads.ui.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.aibuilt.castlenoads.ui.CastleViewModel
import com.aibuilt.castlenoads.ui.components.EmptyPanel

private fun stateLabel(state: Int): String = when (state) {
    Download.STATE_COMPLETED -> "Ready to watch offline"
    Download.STATE_DOWNLOADING -> "Downloading…"
    Download.STATE_QUEUED -> "Waiting to start"
    Download.STATE_STOPPED -> "Paused"
    Download.STATE_FAILED -> "Failed"
    Download.STATE_REMOVING -> "Removing…"
    Download.STATE_RESTARTING -> "Restarting…"
    else -> "Unknown"
}

@OptIn(UnstableApi::class)
@Composable
fun DownloadsScreen(
    vm: CastleViewModel,
    onOpenTitle: (String, String?) -> Unit
) {
    val downloads by vm.downloadList.collectAsState()
    val message by vm.downloadMessage.collectAsState()

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3200)
            vm.consumeDownloadMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Downloads",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
        )
        Text(
            "Castle Premium is active — save as many titles as you like.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (message != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (downloads.isEmpty()) {
            Box(Modifier.weight(1f)) {
                EmptyPanel(
                    "No downloads yet",
                    "Open any title and tap the download arrow to keep it on this device."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(downloads, key = { it.request.id }) { download ->
                    DownloadRow(
                        download = download,
                        titleLabel = vm.downloadLabel(download),
                        onPlay = { onOpenTitle(download.request.id.substringBefore("#"), null) },
                        onRetry = { vm.retryDownload(download.request.id) },
                        onRemove = { vm.removeDownload(download.request.id) }
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun DownloadRow(
    download: Download,
    titleLabel: Pair<String, String>,
    onPlay: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit
) {
    val progress = if (download.contentLength > 0L) {
        (download.bytesDownloaded.toFloat() / download.contentLength.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        titleLabel.first,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (titleLabel.second.isBlank()) stateLabel(download.state)
                        else "${titleLabel.second} · ${stateLabel(download.state)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (download.state == Download.STATE_COMPLETED) {
                    IconButton(onClick = onPlay) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                    }
                }
                if (download.state == Download.STATE_FAILED || download.state == Download.STATE_STOPPED) {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Retry")
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete download")
                }
            }
            if (download.state == Download.STATE_DOWNLOADING || download.state == Download.STATE_QUEUED) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
            if (download.state == Download.STATE_COMPLETED) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tap play to watch offline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onPlay)
                )
            }
        }
    }
}

@Composable
private fun Unused() {
    Button(onClick = {}) { Text("") }
    Text("", textAlign = TextAlign.Center)
}
