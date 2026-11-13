package com.aibuilt.castlenoads.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val avatarIndex: Int = 0,
    val kid: Boolean = false
)

@Serializable
data class ContinueEntry(
    val key: String,
    val titleId: String,
    val episodeId: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = 0L
) {
    val percent: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

@Serializable
data class AppSettings(
    val darkTheme: Boolean = true,
    val autoplayNext: Boolean = true,
    val subtitlesOn: Boolean = false,
    val wifiOnlyDownloads: Boolean = true,
    val preferredQuality: String = "Auto",
    val downloadQuality: String = "720p"
)

/** Ultra HD 4K is unlocked, so "4K" is a real choice for every title. */
val QUALITY_OPTIONS = listOf("Auto", "4K", "1080p", "720p", "480p", "360p")

fun heightForQuality(quality: String): Int = when (quality) {
    "4K" -> 2160
    "1080p" -> 1080
    "720p" -> 720
    "480p" -> 480
    "360p" -> 360
    else -> Int.MAX_VALUE
}

const val MAX_PROFILES = 5
