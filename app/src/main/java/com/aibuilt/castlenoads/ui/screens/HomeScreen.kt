package com.aibuilt.castlenoads.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aibuilt.castlenoads.model.Category
import com.aibuilt.castlenoads.model.Title
import com.aibuilt.castlenoads.ui.CastleViewModel
import com.aibuilt.castlenoads.ui.components.CategoryRow
import com.aibuilt.castlenoads.ui.components.ContinueRow
import com.aibuilt.castlenoads.ui.components.ErrorPanel
import com.aibuilt.castlenoads.ui.components.LoadingPanel
import com.aibuilt.castlenoads.ui.components.Tag

@Composable
fun HomeScreen(
    vm: CastleViewModel,
    onOpenTitle: (Title) -> Unit,
    onPlay: (Title) -> Unit,
    onResume: (Title, String?) -> Unit,
    onSearch: () -> Unit,
    onCast: () -> Unit
) {
    val titles by vm.titles.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val continueWatching by vm.continueWatching.collectAsState()
    val watchlist by vm.watchlistIds.collectAsState()
    val castConnected by vm.castConnected.collectAsState()

    LaunchedEffect(Unit) { vm.refresh() }

    when {
        loading && titles == null -> LoadingPanel()
        error != null && titles == null -> ErrorPanel(error ?: "Unknown error") { vm.refresh() }
        else -> {
            val list = titles.orEmpty()
            val featured = list.firstOrNull { it.ultraHd } ?: list.firstOrNull()
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                item {
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Castle",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onCast) {
                                Icon(
                                    Icons.Filled.Cast,
                                    contentDescription = "Cast",
                                    tint = if (castConnected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onSearch) {
                                Icon(Icons.Filled.Search, contentDescription = "Search")
                            }
                        }
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Tag("PREMIUM ACTIVE")
                            Tag("NO ADS")
                            Text(
                                "Everything unlocked",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (featured != null) {
                    item {
                        HeroBanner(
                            title = featured,
                            inWatchlist = watchlist.contains(featured.id),
                            onPlay = { onPlay(featured) },
                            onDetails = { onOpenTitle(featured) },
                            onWatchlist = { vm.toggleWatchlist(featured.id) }
                        )
                    }
                }

                item {
                    ContinueRow(
                        entries = continueWatching,
                        onOpen = { entry, title -> onResume(title, entry.episodeId) },
                        onRemove = { entry -> vm.removeProgress(entry.key) }
                    )
                }

                Category.entries.forEach { category ->
                    val rowTitles = list.filter { it.categories.contains(category) }
                    if (rowTitles.isNotEmpty()) {
                        item(key = category.name) {
                            CategoryRow(
                                label = category.label,
                                titles = rowTitles,
                                onTitleClick = onOpenTitle
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun HeroBanner(
    title: Title,
    inWatchlist: Boolean,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onWatchlist: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(14.dp))
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
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (title.ultraHd) Tag("4K ULTRA HD")
                if (title.isSeries) Tag("SERIES")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                title.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                title.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPlay) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text(if (title.isSeries) "Play S1 E1" else "Play")
                }
                OutlinedButton(onClick = onDetails) { Text("Details") }
                IconButton(onClick = onWatchlist) {
                    Icon(
                        if (inWatchlist) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = "My list",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CastInfoLine(name: String?, onClick: () -> Unit) {
    if (name == null) return
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Cast, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(8.dp))
        Text("Casting to $name", fontSize = 13.sp)
    }
}
