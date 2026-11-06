package com.example.offlineplayer.media

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/**
 * Сериализация [SavedMedia] в JSON.
 * Значения, совпадающие с дефолтом, не пишутся — симметрично тому,
 * как [parse] подставляет тот же дефолт при отсутствии ключа.
 * Формат плоский (progressSeconds и durationSeconds лежат рядом, не вложенным объектом),
 * поэтому файлы, записанные до введения [Position], читаются без изменений.
 */
val SavedMedia.json: JSONObject
    get() = JSONObject().apply {
        put("id", id.toString())
        put("url", url.toString())
        put("type", type.name)
        put("title", title)
        put("progressSeconds", position.progressSeconds)
        put("downloadedAtEpochMillis", downloadedAt.toEpochMilli())
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
        position.durationSeconds?.let { put("durationSeconds", it) }
        sourceTitle?.let { put("sourceTitle", it) }
        watchByEpochMillis?.let { put("watchByEpochMillis", it) }
    }

/**
 * Разбор JSON в [SavedMedia]. Возвращает null вместо исключения, если данные
 * повреждены или неполны — файл могли обновить вручную или испортить между версиями.
 *
 * Поля, которые определяют саму запись и которые нельзя придумать заново без потери смысла
 * ([SavedMedia.id], [SavedMedia.url], [SavedMedia.downloadedAt]), обязательны: при их отсутствии
 * или невалидном значении запись отбрасывается, а не получает подставное значение.
 */
fun SavedMedia.Companion.parse(json: JSONObject): SavedMedia? = runCatching { // Желательно возвращать result с кодом ошибки
    val id = json.optString("id", "").toUuidOrNull() ?: return null
    val url = json.optString("url", "").toMediaUriOrNull() ?: return null
    val title = json.optString("title", "").takeIf { it.isNotBlank() } ?: return null
    val type = json.optString("type", "")
        .takeIf { it.isNotBlank() }
        ?.let { name -> runCatching { MediaType.valueOf(name) }.getOrNull() }
        ?: return null

    val downloadedAt = (json.opt("downloadedAtEpochMillis") as? Number)
        ?.let { Instant.ofEpochMilli(it.toLong()) }
        ?: return null

    val isOnlyAudio = if (json.has("isOnlyAudio")) {
        json.optBoolean("isOnlyAudio", type != MediaType.Video)
    } else {
        type != MediaType.Video
    }

    val position = Position(
        progressSeconds = json.optInt("progressSeconds", 0).coerceAtLeast(0),
        durationSeconds = if (json.has("durationSeconds")) {
            json.optInt("durationSeconds", 0).coerceAtLeast(0)
        } else {
            null
        }
    )

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
        position = position,
        downloadedAt = downloadedAt,
        downloadStatus = downloadStatus,
        sourceTitle = sourceTitle,
        isFavorite = isFavorite,
        isWatched = isWatched,
        tagColor = tagColor,
        watchByEpochMillis = watchByEpochMillis
    )
}.getOrNull()

// UUID.fromString принимает и "1-1-1-1-1", поэтому результат сверяется с канонической записью.
private fun String.toUuidOrNull(): UUID? =
    runCatching { UUID.fromString(this) }.getOrNull()?.takeIf { it.toString() == lowercase() }

fun List<SavedMedia>.toJsonArray(): JSONArray = JSONArray().apply {
    this@toJsonArray.forEach { put(it.json) }
}

fun JSONArray.parseSavedMediaList(): List<SavedMedia> =
    (0 until length()).mapNotNull { index -> optJSONObject(index)?.let { SavedMedia.parse(it) } }
