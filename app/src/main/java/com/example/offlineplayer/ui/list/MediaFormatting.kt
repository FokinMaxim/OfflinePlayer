package com.example.offlineplayer.ui.list

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.offlineplayer.media.DownloadStatus
import com.example.offlineplayer.media.MediaType
import java.util.Locale

/** "1:05", "1:02:03"; null — длительность ещё не известна. */
fun formatDuration(seconds: Int?): String {
    if (seconds == null) return "—"
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, secs)
    }
}

fun MediaType.label(): String = when (this) {
    MediaType.Video -> "Видео"
    MediaType.Podcast -> "Подкаст"
    MediaType.Music -> "Музыка"
}

fun DownloadStatus.label(): String = when (this) {
    DownloadStatus.Queued -> "В очереди"
    DownloadStatus.Downloading -> "Скачивается"
    DownloadStatus.Completed -> "Скачано"
    DownloadStatus.Failed -> "Ошибка"
}

/** Чёрный или белый — смотря что читается на фоне пользовательского цвета метки. */
fun readableContentColor(background: Color): Color =
    if (background.luminance() > 0.5f) Color.Black else Color.White
