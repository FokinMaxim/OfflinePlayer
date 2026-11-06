package com.example.offlineplayer.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offlineplayer.media.SavedMedia
import com.example.offlineplayer.state.LibraryAction
import com.example.offlineplayer.state.LibraryState
import com.example.offlineplayer.ui.theme.OfflinePlayerTheme
import java.net.URI

/**
 * Экран «Библиотека»: рисует [state] и превращает нажатия в действия. Сам ничего не меняет и
 * никуда не ходит: любое изменение — через [onAction], переходы на другие экраны — через лямбды.
 *
 * Компонент:
 * - Вход: [LibraryState].
 * - Выход: пиксели; [LibraryAction] через [onAction]; запросы переходов [onAdd] и [onEdit].
 * - Конфигурация: нет.
 * - Зависимости: нет (локальное UI-состояние вроде `SnackbarHostState` правдой о данных не является).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaListScreen(
    state: LibraryState,
    onAction: (LibraryAction) -> Unit,
    onAdd: () -> Unit,
    onEdit: (SavedMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar «Отменить» — отображение состояния: он есть, пока в state есть lastDeleted.
    val deleted = state.lastDeleted
    LaunchedEffect(deleted) {
        if (deleted != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Удалено: ${deleted.title}",
                actionLabel = "Отменить",
                duration = SnackbarDuration.Short
            )
            onAction(
                if (result == SnackbarResult.ActionPerformed) {
                    LibraryAction.UndoDeleteClicked(deleted)
                } else {
                    LibraryAction.UndoOfferDismissed
                }
            )
        }
    }

    val error = state.error
    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            onAction(LibraryAction.ErrorDismissed)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Библиотека") },
                actions = {
                    TextButton(onClick = { onAction(LibraryAction.RefreshRequested) }) {
                        Text("Обновить")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Text("+", fontSize = 28.sp)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.items.isEmpty() && state.isLoading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.items.isEmpty() ->
                    Text("Пока ничего нет — нажмите «+»", modifier = Modifier.align(Alignment.Center))

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.items, key = { it.id.toString() }) { media ->
                        SwipeableMediaListItem(
                            media = media,
                            onEdit = { onEdit(media) },
                            onDelete = { onAction(LibraryAction.DeleteClicked(media.id)) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            if (state.isLoading && state.items.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

private val previewItems = listOf(
    SavedMedia(url = URI.create("https://youtu.be/1"), title = "Первое видео", sourceTitle = "Канал"),
    SavedMedia(url = URI.create("https://youtu.be/2"), title = "Второе, с очень длинным названием, которое переносится на несколько строк")
)

@Preview(showBackground = true)
@Composable
private fun MediaListScreenPreview() {
    OfflinePlayerTheme {
        MediaListScreen(state = LibraryState(items = previewItems), onAction = {}, onAdd = {}, onEdit = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaListScreenEmptyPreview() {
    OfflinePlayerTheme {
        MediaListScreen(state = LibraryState(), onAction = {}, onAdd = {}, onEdit = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaListScreenLoadingPreview() {
    OfflinePlayerTheme {
        MediaListScreen(state = LibraryState(isLoading = true), onAction = {}, onAdd = {}, onEdit = {})
    }
}
