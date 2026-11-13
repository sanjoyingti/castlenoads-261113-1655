package com.aibuilt.castlenoads.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aibuilt.castlenoads.CastleApp
import com.aibuilt.castlenoads.model.QUALITY_OPTIONS
import com.aibuilt.castlenoads.model.Playable
import com.aibuilt.castlenoads.model.heightForQuality
import com.aibuilt.castlenoads.model.toPlayable
import com.aibuilt.castlenoads.ui.CastleViewModel
import com.aibuilt.castlenoads.ui.components.ErrorPanel
import com.aibuilt.castlenoads.ui.components.LoadingPanel
import kotlinx.coroutines.delay

private data class TrackOption(val label: String, val groupIndex: Int, val trackIndex: Int, val selected: Boolean)

private fun optionsFor(player: ExoPlayer, type: Int): List<TrackOption> {
    val tracks = player.currentTracks
    val result = mutableListOf<TrackOption>()
    tracks.groups.forEachIndexed { groupIndex, group ->
        if (group.type == type) {
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val label = format.label?.takeIf { it.isNotBlank() }
                    ?: format.language?.takeIf { it.isNotBlank() && it != "und" }
                    ?: "Track ${trackIndex + 1}"
                result += TrackOption(
                    label = label,
                    groupIndex = groupIndex,
                    trackIndex = trackIndex,
                    selected = group.isTrackSelected(trackIndex)
                )
            }
        }
    }
    return result
}

private fun selectTrack(player: ExoPlayer, type: Int, option: TrackOption) {
    val group = player.currentTracks.groups.getOrNull(option.groupIndex) ?: return
    val override = TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex)
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(type)
        .setTrackTypeDisabled(type, false)
        .addOverride(override)
        .build()
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    vm: CastleViewModel,
    titleId: String,
    episodeId: String?,
    trailer: Boolean,
    onBack: () -> Unit,
    onCast: () -> Unit,
    onPlayEpisode: (String, String?, Boolean) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val container = remember { (context.applicationContext as CastleApp).container }
    val titles by vm.titles.collectAsState()
    val settings by vm.settings.collectAsState()
    val castConnected by vm.castConnected.collectAsState()
    val castName by vm.castDeviceName.collectAsState()
    val preparing by vm.preparing.collectAsState()
    val downloads by vm.downloadList.collectAsState()

    val title = titles?.firstOrNull { it.id == titleId }

    BackHandler { onBack() }

    if (title == null) {
        Box(Modifier.fillMaxSize()) { LoadingPanel("Preparing the stream…") }
        return
    }

    val playable: Playable? = remember(title.id, episodeId, trailer, titles) {
        if (trailer) {
            val url = title.trailerUrl
            if (url.isNullOrBlank()) null else Playable(
                key = "${title.id}#trailer",
                titleId = title.id,
                episodeId = null,
                displayName = "${title.name} — Trailer",
                subtitle = "Official trailer",
                videoUrl = url,
                audioLanguages = listOf("English"),
                runtimeMinutes = 2,
                nextEpisodeId = null
            )
        } else {
            title.toPlayable(episodeId)
        }
    }

    if (playable == null) {
        Box(Modifier.fillMaxSize()) {
            ErrorPanel("This title has no playable stream.") { onBack() }
        }
        return
    }

    var quality by remember(playable.key) { mutableStateOf(settings.preferredQuality) }
    var subtitlesOn by remember(playable.key) { mutableStateOf(settings.subtitlesOn) }
    var showQuality by remember { mutableStateOf(false) }
    var showAudio by remember { mutableStateOf(false) }
    var showSubs by remember { mutableStateOf(false) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(true) }
    var ended by remember { mutableStateOf(false) }
    var audioOptions by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var subOptions by remember { mutableStateOf<List<TrackOption>>(emptyList()) }

    val startPosition = remember(playable.key) { vm.resumePosition(playable.key) }
    val alreadyDownloaded = downloads.any { it.request.id == playable.key }
    val preparingThis = preparing.contains(playable.key)

    val player = remember(playable.key, quality) {
        container.createPlayer(context, heightForQuality(quality))
    }

    DisposableEffect(player) {
        view.keepScreenOn = true
        player.setMediaItem(MediaItem.Builder().setUri(playable.videoUrl).build())
        player.prepare()
        player.playWhenReady = true
        if (startPosition > 0L) player.seekTo(startPosition)

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    durationMs = player.duration.coerceAtLeast(0L)
                }
                if (playbackState == Player.STATE_ENDED) {
                    ended = true
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                audioOptions = optionsFor(player, C.TRACK_TYPE_AUDIO)
                subOptions = optionsFor(player, C.TRACK_TYPE_TEXT)
            }
        }
        player.addListener(listener)

        onDispose {
            view.keepScreenOn = false
            val finalDuration = if (player.duration > 0L) player.duration else durationMs
            vm.saveProgress(playable, player.currentPosition.coerceAtLeast(0L), finalDuration)
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            if (player.duration > 0L) durationMs = player.duration
            delay(400L)
        }
    }

    LaunchedEffect(subtitlesOn, player) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesOn)
            .build()
    }

    LaunchedEffect(ended, settings.autoplayNext, playable.key) {
        if (ended && settings.autoplayNext && playable.nextEpisodeId != null) {
            delay(900L)
            onPlayEpisode(titleId, playable.nextEpisodeId, false)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        playable.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        playable.subtitle,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = {
                        if (castConnected) {
                            vm.castLoad(playable, positionMs, durationMs)
                        } else {
                            onCast()
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.Cast,
                        contentDescription = "Cast",
                        tint = if (castConnected) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                IconButton(
                    onClick = {
                        if (!alreadyDownloaded && !preparingThis) {
                            vm.download(playable, title.name, settings.wifiOnlyDownloads)
                        }
                    },
                    enabled = !trailer && !alreadyDownloaded && !preparingThis
                ) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = "Download",
                        tint = if (alreadyDownloaded) MaterialTheme.colorScheme.primary
                        else if (trailer) Color.Gray else Color.White
                    )
                }
            }
            if (castConnected && castName != null) {
                Text(
                    "Casting to $castName",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )
            }
        }

        if (ended) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Finished", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = { player.seekTo(0); player.play(); ended = false }) {
                        Icon(Icons.Filled.Replay10, contentDescription = "Replay", tint = Color.White)
                    }
                    val next = playable.nextEpisodeId
                    if (next != null) {
                        IconButton(onClick = { onPlayEpisode(titleId, next, false) }) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next episode", tint = Color.White)
                        }
                    }
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to details",
                            tint = Color.White
                        )
                    }
                }
            }
        } else {
            IconButton(
                onClick = { if (playing) player.pause() else player.play() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
            ) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(positionMs), color = Color.White, fontSize = 12.sp)
                Slider(
                    value = positionMs.toFloat().coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                    onValueChange = { player.seekTo(it.toLong()) },
                    valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Text(formatTime(durationMs), color = Color.White, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L)) }) {
                    Icon(Icons.Filled.Replay10, contentDescription = "Back 10 seconds", tint = Color.White)
                }
                IconButton(onClick = { player.seekTo(player.currentPosition + 10_000L) }) {
                    Icon(Icons.Filled.Forward10, contentDescription = "Forward 10 seconds", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))

                Box {
                    IconButton(onClick = { showSubs = true }) {
                        Icon(
                            Icons.Filled.Subtitles,
                            contentDescription = "Subtitles",
                            tint = if (subtitlesOn) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                    DropdownMenu(expanded = showSubs, onDismissRequest = { showSubs = false }) {
                        DropdownMenuItem(
                            text = { Text(if (subtitlesOn) "Subtitles: on" else "Subtitles: off") },
                            onClick = {
                                subtitlesOn = !subtitlesOn
                                vm.updateSettings { it.copy(subtitlesOn = subtitlesOn) }
                                showSubs = false
                            }
                        )
                        if (subOptions.isEmpty()) {
                            DropdownMenuItem(text = { Text("No subtitle tracks") }, onClick = { showSubs = false })
                        } else {
                            subOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label + if (option.selected) " ✓" else "") },
                                    onClick = {
                                        subtitlesOn = true
                                        selectTrack(player, C.TRACK_TYPE_TEXT, option)
                                        showSubs = false
                                    }
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showAudio = true }) {
                        Icon(Icons.Filled.Audiotrack, contentDescription = "Audio track", tint = Color.White)
                    }
                    DropdownMenu(expanded = showAudio, onDismissRequest = { showAudio = false }) {
                        if (audioOptions.isEmpty()) {
                            playable.audioLanguages.forEach { language ->
                                DropdownMenuItem(text = { Text(language) }, onClick = { showAudio = false })
                            }
                        } else {
                            audioOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label + if (option.selected) " ✓" else "") },
                                    onClick = {
                                        selectTrack(player, C.TRACK_TYPE_AUDIO, option)
                                        showAudio = false
                                    }
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showQuality = true }) {
                        Icon(Icons.Filled.Hd, contentDescription = "Quality", tint = Color.White)
                    }
                    DropdownMenu(expanded = showQuality, onDismissRequest = { showQuality = false }) {
                        QUALITY_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option + if (option == quality) " ✓" else "") },
                                onClick = {
                                    quality = option
                                    vm.updateSettings { it.copy(preferredQuality = option) }
                                    showQuality = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                val nextEpisode = playable.nextEpisodeId
                if (nextEpisode != null) {
                    IconButton(onClick = { onPlayEpisode(titleId, nextEpisode, false) }) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next episode", tint = Color.White)
                    }
                }
            }
        }
    }
}
