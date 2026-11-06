package com.example.offlineplayer.media

import androidx.compose.ui.graphics.Color
import java.net.URI
import java.time.Instant
import java.util.UUID

/**
 * Одна скачанная единица медиа (видео, подкаст или музыка).
 *
 * Поля разбиты на три группы, как того требует задание:
 *  - обязательные без дефолта: [url], [title];
 *  - обязательные с разумным дефолтом: [type], [id], [isOnlyAudio], [position],
 *    [downloadedAt], [downloadStatus], [isFavorite], [isWatched], [tagColor];
 *  - по-настоящему необязательные (nullable): [sourceTitle], [watchByEpochMillis].
 *
 * Валидность [url], [downloadedAt] и [position] обеспечивается их типами и разбором JSON,
 * поэтому в самом классе перепроверять их не нужно.
 */
data class SavedMedia(
    val url: URI,
    val type: MediaType = MediaType.Video,
    val title: String,
    val id: UUID = UUID.randomUUID(),
    // Для Video по умолчанию считаем, что скачано видео целиком;
    // Podcast/Music — по своей природе аудио.
    val isOnlyAudio: Boolean = type != MediaType.Video,
    // Место, на котором закончился последний просмотр/прослушивание, и длительность.
    val position: Position = Position(),
    // Миллисекундная точность: в JSON время хранится в миллисекундах, иначе round-trip не равен оригиналу.
    val downloadedAt: Instant = Instant.ofEpochMilli(System.currentTimeMillis()),
    val downloadStatus: DownloadStatus = DownloadStatus.Queued,
    val sourceTitle: String? = null,
    val isFavorite: Boolean = false,
    // "Дело сделано" для медиатеки: пользователь вручную отмечает, что досмотрел/дослушал.
    val isWatched: Boolean = false,
    // Цветовая метка/категория, выставляется пользователем на экране редактирования.
    val tagColor: Color = Color.White,
    // Дедлайн-аналог: дата, до которой хочется посмотреть/прослушать. Не путать с downloadedAt.
    val watchByEpochMillis: Long? = null
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
    }

    // Пустой companion object — точка, к которой снаружи (в файле с JSON-логикой)
    // подвешивается расширение SavedMedia.Companion.parse(...), как требует задание.
    companion object
}
