package com.example.offlineplayer.state

import com.example.offlineplayer.media.SavedMedia
import java.util.UUID

/**
 * Все способы изменить состояние «Библиотеки». Состояние меняется только через эти действия.
 * `sealed`, чтобы `when` в исполнителе без `else` не позволил забыть новый вариант.
 */
sealed interface LibraryAction {

    // Действия пользователя и системы

    /** Перечитать список с диска: при старте и по кнопке «Обновить». */
    data object RefreshRequested : LibraryAction

    data class DeleteClicked(val id: UUID) : LibraryAction

    /** Несёт саму запись, чтобы действие целиком описывало намерение и сервису не пришлось гадать. */
    data class UndoDeleteClicked(val media: SavedMedia) : LibraryAction

    data object UndoOfferDismissed : LibraryAction

    /** Запись создана или изменена на экране редактирования. */
    data class MediaSaved(val media: SavedMedia) : LibraryAction

    data object ErrorDismissed : LibraryAction

    // Ответы сервиса

    data class ListLoaded(val requestId: Int, val items: List<SavedMedia>) : LibraryAction

    // Ошибки

    data class LoadFailed(val requestId: Int, val message: String) : LibraryAction

    data class PersistFailed(val message: String) : LibraryAction
}
