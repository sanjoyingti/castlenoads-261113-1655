package com.aibuilt.castlenoads.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aibuilt.castlenoads.model.Episode
import com.aibuilt.castlenoads.model.Title
import com.aibuilt.castlenoads.model.toPlayable
import com.aibuilt.castlenoads.ui.CastleViewModel
import com.aibuilt.castlenoads.ui.components.EmptyPanel
import com.aibuilt.castlenoads.ui.components.Tag
import com.aibuilt.castlenoads.ui.components.TitleCard

@Composable
fun DetailScreen(
    vm: CastleViewModel,
    titleId: String,
    onBack: () -> Unit,
    onPlay: (Title, String?, Boolean) -> Unit,
    onOpenTitle: (Title) -> Unit
) {
    val titles by vm.titles.collectAsState()
    val watchlist by vm.watchlistIds.collectAsState()
    val settings by vm.settings.collectAsState()
    val preparing by vm.preparing.collectAsState()
    val downloads by vm.downloadList.collectAsState()

    val title = titles?.firstOrNull { it.id == titleId }
    if (title == null) {
        Box(Modifier.fillMaxSize()) {
            EmptyPanel("Title not found", "It may have left the catalogue.")
        }
        return
    }

    var seasonNumber by remember(title.id) { mutableIntStateOf(title.seasons.firstOrNull()?.number ?: 1) }
    var seasonMenu by remember { mutableStateOf(false) }
    val season = title.seasons.firstOrNull { it.number == seasonNumber }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
            ) {
                AsyncImage(
                    model = title.backdropUrl,
                    contentDescription = title.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.45f), MaterialTheme.colorScheme.background)
                            )
                        )
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (title.ultraHd) Tag("4K ULTRA HD")
                    if (title.premium) Tag("PREMIUM · UNLOCKED") else Tag("INCLUDED")
                }
                Spacer(Modifier.height(8.dp))
                Text(title.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${title.year} · ${title.maturity} · " +
                        if (title.isSeries) "${title.seasons.size} season(s)"
                        else "${title.movie?.runtimeMinutes ?: 0} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { onPlay(title, title.firstEpisode()?.id, false) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text(if (title.isSeries) "Play first episode" else "Play")
                    }
                    OutlinedButton(onClick = { vm.toggleWatchlist(title.id) }) {
                        Icon(
                            if (watchlist.contains(title.id)) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = null
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(if (watchlist.contains(title.id)) "In my list" else "My list")
                    }
                    val playable = title.toPlayable(title.firstEpisode()?.id)
                    val key = playable?.key
                    val downloaded = downloads.any { it.request.id == key }
                    IconButton(
                        onClick = {
                            val p = playable ?: return@IconButton
                            vm.download(p, title.name, settings.wifiOnlyDownloads)
                        },
                        enabled = key != null && !downloaded && preparing.none { it == key }
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Download",
                            tint = if (downloaded) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (title.tagline.isNotBlank()) {
                    Text(title.tagline, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                }
                Text(title.synopsis, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(10.dp))
                if (title.genres.isNotEmpty()) {
                    Text(
                        title.genres.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (title.castList.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Cast: ${title.castList.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!title.trailerUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { onPlay(title, null, true) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("Play trailer")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        if (title.isSeries && season != null) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Episodes",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        OutlinedButton(onClick = { seasonMenu = true }) {
                            Text("Season $seasonNumber")
                        }
                        DropdownMenu(expanded = seasonMenu, onDismissRequest = { seasonMenu = false }) {
                            title.seasons.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("Season ${s.number}") },
                                    onClick = {
                                        seasonNumber = s.number
                                        seasonMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            items(season.episodes, key = { it.id }) { episode ->
                EpisodeRow(episode = episode, onPlay = { onPlay(title, episode.id, false) })
            }
        }

        item {
            val related = titles.orEmpty()
                .filter { it.id != title.id && it.categories.any { c -> title.categories.contains(c) } }
                .take(10)
            if (related.isNotEmpty()) {
                Column(Modifier.padding(top = 18.dp, bottom = 24.dp)) {
                    Text(
                        "More like this",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(related, key = { it.id }) { rel ->
                            TitleCard(title = rel, onClick = { onOpenTitle(rel) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, onPlay: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onPlay)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("${episode.number}")
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${episode.runtimeMinutes} min · ${episode.audioLanguages.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    episode.synopsis,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play episode")
        }
    }
}
