package com.example.offlineplayer.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offlineplayer.media.DownloadStatus
import com.example.offlineplayer.media.MediaType
import com.example.offlineplayer.media.Position
import com.example.offlineplayer.media.SavedMedia
import com.example.offlineplayer.ui.theme.OfflinePlayerTheme
import java.net.URI

private val ItemShape = RoundedCornerShape(12.dp)

/** Строка списка со свайпом влево: красный фон «Delete» проступает под сдвигаемой карточкой. */
@Composable
fun SwipeableMediaListItem(
    media: SavedMedia,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) currentOnDelete()
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            val swiping = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (swiping) Color(0xFFDC322F) else Color.Transparent, ItemShape)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (swiping) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) {
        MediaListItem(media = media, onEdit = onEdit, onDelete = onDelete)
    }
}

/** Карточка одного медиа. Высоту не задаём: она складывается из содержимого (в том числе длинного названия). */
@Composable
fun MediaListItem(
    media: SavedMedia,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Просмотренное приглушаем без прозрачности, чтобы под карточкой не просвечивал фон свайпа.
    val container = if (media.isWatched) {
        media.tagColor.copy(alpha = 0.45f).compositeOver(MaterialTheme.colorScheme.surface)
    } else {
        media.tagColor
    }
    val content = readableContentColor(container).copy(alpha = if (media.isWatched) 0.6f else 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ItemShape,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MediaPreview(type = media.type)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    media.sourceTitle?.let { source ->
                        Text(text = source, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = listOf(
                            media.type.label(),
                            formatDuration(media.position.durationSeconds),
                            media.downloadStatus.label()
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                val buttonColors = ButtonDefaults.textButtonColors(contentColor = content)
                TextButton(onClick = onEdit, colors = buttonColors) { Text("Изменить") }
                TextButton(onClick = onDelete, colors = buttonColors) { Text("Удалить") }
            }
        }
    }
}

/** Заглушка превью. Здесь позже появится реальная миниатюра скачанного файла. */
@Composable
fun MediaPreview(type: MediaType, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(96.dp)
            .height(54.dp)
            .background(Color(0xFF9E9E9E), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (type) {
                MediaType.Video -> "▶"
                MediaType.Podcast -> "◉"
                MediaType.Music -> "♪"
            },
            color = Color.White,
            fontSize = 24.sp
        )
    }
}

private fun sample(title: String, tag: Color, watched: Boolean = false, source: String? = "Канал") = SavedMedia(
    url = URI.create("https://youtu.be/abc"),
    title = title,
    tagColor = tag,
    isWatched = watched,
    sourceTitle = source,
    position = Position(durationSeconds = 3725),
    downloadStatus = DownloadStatus.Completed
)

@Preview(showBackground = true)
@Composable
private fun MediaListItemPreview() {
    OfflinePlayerTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MediaListItem(sample("Короткое", Color.White), onEdit = {}, onDelete = {})
            MediaListItem(
                sample("Очень длинное название, которое занимает несколько строк и заставляет карточку расти по содержимому", Color(0xFFCFE6B8)),
                onEdit = {}, onDelete = {}
            )
            MediaListItem(sample("Тёмная метка", Color(0xFF073642), source = null), onEdit = {}, onDelete = {})
            MediaListItem(sample("Просмотрено", Color(0xFFB9D7EF), watched = true), onEdit = {}, onDelete = {})
        }
    }
}
