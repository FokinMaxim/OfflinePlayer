package com.example.offlineplayer.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.offlineplayer.media.MediaType
import com.example.offlineplayer.media.SavedMedia
import com.example.offlineplayer.ui.theme.OfflinePlayerTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ColorSaver = Saver<Color, Int>(
    save = { it.toArgb() },
    restore = { Color(it) }
)

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private fun formatEpochMillis(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

/**
 * Экран редактирования [SavedMedia]. Состояние хранится прямо здесь (своего ViewModel пока нет —
 * это тема следующих лекций), поля сохраняются через rememberSaveable, чтобы пережить поворот экрана.
 */
@Composable
fun MediaEditScreen(
    initial: SavedMedia,
    onSave: (SavedMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by rememberSaveable { mutableStateOf(initial.title) }
    var url by rememberSaveable { mutableStateOf(initial.url) }
    var type by rememberSaveable { mutableStateOf(initial.type) }
    var isWatched by rememberSaveable { mutableStateOf(initial.isWatched) }
    var tagColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(initial.tagColor) }
    var customColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(initial.tagColor) }
    var watchByEpochMillis by rememberSaveable { mutableStateOf(initial.watchByEpochMillis) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickerScreen(
            initialColor = customColor,
            onDone = { picked ->
                customColor = picked
                tagColor = picked
                showColorPicker = false
            },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WatchedCheckbox(
            isWatched = isWatched,
            onCheckedChange = { isWatched = it }
        )

        // "Дело сделано" — карточка сереет: контент ниже рисуется с уменьшенной непрозрачностью.
        Column(
            modifier = Modifier.alpha(if (isWatched) 0.5f else 1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GrowingTitleField(value = title, onValueChange = { title = it })
            UrlField(value = url, onValueChange = { url = it })
            WatchByDateField(
                watchByEpochMillis = watchByEpochMillis,
                onDateSelected = { watchByEpochMillis = it }
            )
            TypeRow(selectedType = type, onTypeSelected = { type = it })
            ColorRow(
                selectedColor = tagColor,
                onColorSelected = { tagColor = it },
                customPreviewColor = customColor,
                onCustomColorLongPress = { showColorPicker = true }
            )
        }

        Button(
            onClick = {
                onSave(
                    initial.copy(
                        title = title,
                        url = url,
                        type = type,
                        isWatched = isWatched,
                        tagColor = tagColor,
                        watchByEpochMillis = watchByEpochMillis
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить")
        }
    }
}

@Composable
private fun GrowingTitleField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Название") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun UrlField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Ссылка") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WatchedCheckbox(isWatched: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = isWatched, onCheckedChange = onCheckedChange)
        Text("Просмотрено")
    }
}

@Composable
private fun WatchByDateField(watchByEpochMillis: Long?, onDateSelected: (Long?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { showDialog = true }) {
            Text("Выбрать дату")
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Дедлайн просмотра: " +
                (watchByEpochMillis?.let { formatEpochMillis(it) } ?: "не выбран")
        )
    }

    if (showDialog) {
        val state = rememberDatePickerState(initialSelectedDateMillis = watchByEpochMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateSelected(state.selectedDateMillis)
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun TypeRow(selectedType: MediaType, onTypeSelected: (MediaType) -> Unit) {
    Column {
        Text("Тип:")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MediaType.entries.forEach { type ->
                FilterChip(
                    selected = type == selectedType,
                    onClick = { onTypeSelected(type) },
                    label = { Text(type.name) }
                )
            }
        }
    }
}

@Composable
private fun ColorRow(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    customPreviewColor: Color,
    onCustomColorLongPress: () -> Unit
) {
    Column {
        Text("Цвет:")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PRESET_TAG_COLORS.forEach { color ->
                ColorSwatch(
                    color = color,
                    selected = color == selectedColor,
                    onClick = { onColorSelected(color) }
                )
            }
            CustomColorSwatch(
                color = customPreviewColor,
                selected = customPreviewColor == selectedColor,
                onClick = { onColorSelected(customPreviewColor) },
                onLongPress = onCustomColorLongPress
            )
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(color, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Text("✓", color = Color.Black)
        }
    }
}

@Composable
private fun CustomColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(color, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Text("✓", color = Color.Black)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaEditScreenPreview() {
    OfflinePlayerTheme {
        MediaEditScreen(initial = SavedMedia(url = "https://youtu.be/abc", title = "Preview title"), onSave = {})
    }
}

@PreviewScreenSizes
@Composable
private fun MediaEditScreenAdaptivePreview() {
    OfflinePlayerTheme {
        MediaEditScreen(
            initial = SavedMedia(
                url = "https://youtu.be/abc",
                title = "Очень длинное название, которое должно переноситься на несколько строк и расширять поле ввода",
                type = MediaType.Music,
                isWatched = true
            ),
            onSave = {}
        )
    }
}
