package com.aibuilt.castlenoads.model

import kotlinx.serialization.Serializable

@Serializable
enum class Category(val label: String) {
    ACTION("Action"),
    DRAMA("Drama"),
    DOCUMENTARIES("Documentaries"),
    KIDS("Kids"),
    ULTRA_HD_4K("Ultra HD 4K")
}

@Serializable
data class Movie(
    val videoUrl: String,
    val runtimeMinutes: Int,
    val audioLanguages: List<String> = listOf("English")
)

@Serializable
data class Episode(
    val id: String,
    val number: Int,
    val title: String,
    val synopsis: String,
    val runtimeMinutes: Int,
    val videoUrl: String,
    val audioLanguages: List<String> = listOf("English")
)

@Serializable
data class Season(
    val number: Int,
    val episodes: List<Episode> = emptyList()
)

@Serializable
data class Title(
    val id: String,
    val name: String,
    val tagline: String,
    val synopsis: String,
    val year: Int,
    val maturity: String,
    val genres: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    val castList: List<String> = emptyList(),
    val posterUrl: String,
    val backdropUrl: String,
    val premium: Boolean = false,
    val ultraHd: Boolean = false,
    val downloadable: Boolean = true,
    val trailerUrl: String? = null,
    val movie: Movie? = null,
    val seasons: List<Season> = emptyList()
) {
    val isSeries: Boolean get() = seasons.isNotEmpty()

    fun allEpisodes(): List<Episode> = seasons.flatMap { it.episodes }

    fun episodeById(episodeId: String?): Episode? {
        if (episodeId.isNullOrBlank()) return null
        return allEpisodes().firstOrNull { it.id == episodeId }
    }

    fun firstEpisode(): Episode? = seasons.firstOrNull()?.episodes?.firstOrNull()

    fun nextEpisodeAfter(episodeId: String?): Episode? {
        val list = allEpisodes()
        if (list.isEmpty()) return null
        if (episodeId.isNullOrBlank()) return list.firstOrNull()
        val index = list.indexOfFirst { it.id == episodeId }
        return if (index >= 0 && index + 1 < list.size) list[index + 1] else null
    }

    fun encodeWatchKey(episodeId: String?): String =
        if (episodeId.isNullOrBlank()) "$id#movie" else "$id#$episodeId"
}

/** A concrete thing that can be played: a movie or one episode. */
data class Playable(
    val key: String,
    val titleId: String,
    val episodeId: String?,
    val displayName: String,
    val subtitle: String,
    val videoUrl: String,
    val audioLanguages: List<String>,
    val runtimeMinutes: Int,
    val nextEpisodeId: String?
)

fun Title.toPlayable(episodeId: String?): Playable? {
    val episode = episodeById(episodeId)
    if (episode != null) {
        return Playable(
            key = encodeWatchKey(episode.id),
            titleId = id,
            episodeId = episode.id,
            displayName = episode.title,
            subtitle = "S${seasons.firstOrNull { s -> s.episodes.any { it.id == episode.id } }?.number ?: 1} E${episode.number} · $name",
            videoUrl = episode.videoUrl,
            audioLanguages = episode.audioLanguages,
            runtimeMinutes = episode.runtimeMinutes,
            nextEpisodeId = nextEpisodeAfter(episode.id)?.id
        )
    }
    val firstEpisode = firstEpisode()
    if (firstEpisode != null) {
        return toPlayable(firstEpisode.id)
    }
    val film = movie ?: return null
    return Playable(
        key = encodeWatchKey(null),
        titleId = id,
        episodeId = null,
        displayName = name,
        subtitle = "$year · $maturity · ${film.runtimeMinutes} min",
        videoUrl = film.videoUrl,
        audioLanguages = film.audioLanguages,
        runtimeMinutes = film.runtimeMinutes,
        nextEpisodeId = null
    )
}
