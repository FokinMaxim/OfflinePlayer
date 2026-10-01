package com.example.offlineplayer.media

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Сериализация [SavedMedia] в JSON.
 * Значения, совпадающие с дефолтом, не пишутся — симметрично тому,
 * как [parse] подставляет тот же дефолт при отсутствии ключа.
 */
val SavedMedia.json: JSONObject
    get() = JSONObject().apply {
        put("id", id.toString())
        put("url", url)
        put("type", type.name)
        put("title", title)
        put("progressSeconds", progressSeconds)
        put("downloadedAtEpochMillis", downloadedAtEpochMillis)
        put("isFavorite", isFavorite)
        put("isWatched", isWatched)
        if (isOnlyAudio != (type != MediaType.Video)) {
            put("isOnlyAudio", isOnlyAudio)
        }
        if (downloadStatus != DownloadStatus.Queued) {
            put("downloadStatus", downloadStatus.name)
        }
        if (tagColor != Color.White) {
            put("tagColor", tagColor.toArgb())
        }
        durationSeconds?.let { put("durationSeconds", it) }
        sourceTitle?.let { put("sourceTitle", it) }
        watchByEpochMillis?.let { put("watchByEpochMillis", it) }
    }

/**
 * Разбор JSON в [SavedMedia]. Возвращает null вместо исключения, если данные
 * повреждены или неполны — файл могли обновить вручную или испортить между версиями.
 */
fun SavedMedia.Companion.parse(json: JSONObject): SavedMedia? = runCatching {
    val url = json.optString("url", "").takeIf { it.isNotBlank() } ?: return null
    val title = json.optString("title", "").takeIf { it.isNotBlank() } ?: return null
    val type = json.optString("type", "")
        .takeIf { it.isNotBlank() }
        ?.let { name -> runCatching { MediaType.valueOf(name) }.getOrNull() }
        ?: return null

    val id = json.optString("id", "")
        .takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { UUID.fromString(raw) }.getOrNull() }
        ?: UUID.randomUUID()

    val isOnlyAudio = if (json.has("isOnlyAudio")) {
        json.optBoolean("isOnlyAudio", type != MediaType.Video)
    } else {
        type != MediaType.Video
    }

    val progressSeconds = json.optInt("progressSeconds", 0).coerceAtLeast(0)

    val durationSeconds = if (json.has("durationSeconds")) {
        json.optInt("durationSeconds", 0).coerceAtLeast(0)
    } else {
        null
    }

    val downloadedAtEpochMillis = json.optLong("downloadedAtEpochMillis", System.currentTimeMillis())

    val downloadStatus = json.optString("downloadStatus", "")
        .takeIf { it.isNotBlank() }
        ?.let { name -> runCatching { DownloadStatus.valueOf(name) }.getOrNull() }
        ?: DownloadStatus.Queued

    val sourceTitle = if (json.has("sourceTitle")) json.optString("sourceTitle") else null
    val isFavorite = json.optBoolean("isFavorite", false)
    val isWatched = json.optBoolean("isWatched", false)

    val tagColor = if (json.has("tagColor")) {
        Color(json.optInt("tagColor", Color.White.toArgb()))
    } else {
        Color.White
    }

    val watchByEpochMillis = if (json.has("watchByEpochMillis")) {
        json.optLong("watchByEpochMillis")
    } else {
        null
    }

    SavedMedia(
        url = url,
        type = type,
        title = title,
        id = id,
        isOnlyAudio = isOnlyAudio,
        progressSeconds = progressSeconds,
        durationSeconds = durationSeconds,
        downloadedAtEpochMillis = downloadedAtEpochMillis,
        downloadStatus = downloadStatus,
        sourceTitle = sourceTitle,
        isFavorite = isFavorite,
        isWatched = isWatched,
        tagColor = tagColor,
        watchByEpochMillis = watchByEpochMillis
    )
}.getOrNull()

fun List<SavedMedia>.toJsonArray(): JSONArray = JSONArray().apply {
    this@toJsonArray.forEach { put(it.json) }
}

fun JSONArray.parseSavedMediaList(): List<SavedMedia> =
    (0 until length()).mapNotNull { index -> optJSONObject(index)?.let { SavedMedia.parse(it) } }
