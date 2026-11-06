package com.example.offlineplayer.state

import com.example.offlineplayer.media.SavedMedia

/**
 * Состояние экрана «Библиотека»: единственный источник правды о том, что сейчас на экране.
 *
 * Компонент:
 * - Вход: новое состояние от [LibraryActionExecutor], через [LibraryStore].
 * - Выход: слепок состояния, на который подписано представление (`StateFlow` в [LibraryStore]).
 * - Конфигурация: нет.
 * - Зависимости: нет.
 */
data class LibraryState(
    /** Всегда отсортирован: новые скачанные сверху. */
    val items: List<SavedMedia> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Последняя удалённая запись; пока она не null, пользователю предлагается «Отменить». */
    val lastDeleted: SavedMedia? = null,
    /** Номер последнего запрошенного чтения; ответы с меньшим номером устарели. */
    val loadRequestId: Int = 0
)
