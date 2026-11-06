package com.example.offlineplayer.state

import com.example.offlineplayer.media.SavedMedia

/**
 * Исполнитель действий: S' = f(S, A). Единственное место, где рождается новое состояние.
 * Чистая функция: ни диска, ни сети, ни потоков — поэтому проверяется обычным unit-тестом.
 *
 * Компонент:
 * - Вход: текущее состояние и действие.
 * - Выход: новое состояние.
 * - Конфигурация: нет.
 * - Зависимости: нет.
 */
class LibraryActionExecutor {

    fun execute(state: LibraryState, action: LibraryAction): LibraryState = when (action) {
        LibraryAction.RefreshRequested -> state.copy(
            isLoading = true,
            error = null,
            loadRequestId = state.loadRequestId + 1
        )

        is LibraryAction.DeleteClicked -> {
            val media = state.items.firstOrNull { it.id == action.id }
            if (media == null) {
                state
            } else {
                state.copy(items = state.items - media, lastDeleted = media)
            }
        }

        is LibraryAction.UndoDeleteClicked -> state.copy(
            items = state.items.with(action.media),
            lastDeleted = null
        )

        LibraryAction.UndoOfferDismissed -> state.copy(lastDeleted = null)

        is LibraryAction.MediaSaved -> state.copy(items = state.items.with(action.media))

        LibraryAction.ErrorDismissed -> state.copy(error = null)

        // Ответ на устаревший запрос игнорируется целиком: ни список, ни индикатор не трогаем.
        is LibraryAction.ListLoaded ->
            if (action.requestId < state.loadRequestId) {
                state
            } else {
                state.copy(
                    items = action.items.sortedByDescending { it.downloadedAt },
                    isLoading = false,
                    error = null
                )
            }

        is LibraryAction.LoadFailed ->
            if (action.requestId < state.loadRequestId) {
                state
            } else {
                state.copy(isLoading = false, error = action.message)
            }

        is LibraryAction.PersistFailed -> state.copy(error = action.message)
    }

    /** Вставка или замена по id с сохранением порядка «новые сверху». */
    private fun List<SavedMedia>.with(media: SavedMedia): List<SavedMedia> =
        (filterNot { it.id == media.id } + media).sortedByDescending { it.downloadedAt }
}
