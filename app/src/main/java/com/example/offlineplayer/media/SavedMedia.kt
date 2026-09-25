package com.example.offlineplayer.media

import java.util.UUID

/**
 * Одна скачанная единица медиа (видео, подкаст или музыка).
 *
 * Поля разбиты на три группы, как того требует задание:
 *  - обязательные без дефолта: [url], [type], [title];
 *  - обязательные с разумным дефолтом: [id], [isOnlyAudio], [progressSeconds],
 *    [downloadedAtEpochMillis], [downloadStatus], [isFavorite];
 *  - по-настоящему необязательные (nullable): [durationSeconds], [sourceTitle].
 */
data class SavedMedia(
    val url: String,
    val type: MediaType = MediaType.Video,
    val title: String,
    val id: UUID = UUID.randomUUID(),
    // Для Video по умолчанию считаем, что скачано видео целиком;
    // Podcast/Music — по своей природе аудио.
    val isOnlyAudio: Boolean = type != MediaType.Video,
    // Таймкод, на котором закончился последний просмотр/прослушивание.
    val progressSeconds: Int = 0,
    val durationSeconds: Int? = null,
    val downloadedAtEpochMillis: Long = System.currentTimeMillis(), // доработать, реализовать чаерз Date
    val downloadStatus: DownloadStatus = DownloadStatus.Queued,
    val sourceTitle: String? = null,
    val isFavorite: Boolean = false
) {
    init {
        require(url.isNotBlank()) { "url must not be blank" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(progressSeconds >= 0) { "progressSeconds must not be negative" }
        require(durationSeconds == null || durationSeconds >= 0) {
            "durationSeconds must not be negative"
        }

    }

    // Пустой companion object — точка, к которой снаружи (в файле с JSON-логикой)
    // подвешивается расширение SavedMedia.Companion.parse(...), как требует задание.
    companion object
}
