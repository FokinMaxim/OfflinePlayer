package com.example.offlineplayer.service

import com.example.offlineplayer.media.MediaStorage
import com.example.offlineplayer.state.LibraryAction
import com.example.offlineplayer.state.LibraryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Реализация [LibraryService] поверх файлового [MediaStorage].
 *
 * Компонент:
 * - Вход: действия-команды (`RefreshRequested`, `DeleteClicked`, `UndoDeleteClicked`, `MediaSaved`)
 *   вместе с состоянием после них.
 * - Выход: действия-результаты через [dispatch] (`ListLoaded`, `LoadFailed`, `PersistFailed`).
 * - Конфигурация: нет.
 * - Зависимости: [storage], [dispatch], [scope], [io].
 *
 * [io] обязан быть однопоточным (`Dispatchers.IO.limitedParallelism(1)`): `MediaStorage` не
 * потокобезопасен, а команды должны выполняться в порядке получения.
 */
class StorageLibraryService(
    private val storage: MediaStorage,
    private val dispatch: (LibraryAction) -> Unit,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher
) : LibraryService {

    // Трогается только на [io], поэтому синхронизация не нужна.
    private var loaded = false

    override fun onAction(action: LibraryAction, state: LibraryState) {
        when (action) {
            LibraryAction.RefreshRequested -> load(requestId = state.loadRequestId)
            is LibraryAction.DeleteClicked -> persist { storage.remove(action.id) }
            is LibraryAction.UndoDeleteClicked -> persist { storage.add(action.media) }
            is LibraryAction.MediaSaved -> persist { storage.add(action.media) }
            LibraryAction.UndoOfferDismissed,
            LibraryAction.ErrorDismissed,
            is LibraryAction.ListLoaded,
            is LibraryAction.LoadFailed,
            is LibraryAction.PersistFailed -> Unit
        }
    }

    private fun load(requestId: Int) {
        scope.launch {
            try {
                val items = withContext(io) {
                    storage.load()
                    loaded = true
                    storage.items
                }
                dispatch(LibraryAction.ListLoaded(requestId, items))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dispatch(LibraryAction.LoadFailed(requestId, e.message ?: "Не удалось загрузить список"))
            }
        }
    }

    private fun persist(change: () -> Unit) {
        scope.launch {
            try {
                withContext(io) {
                    // Без прочитанного списка запись затёрла бы файл одной записью.
                    if (!loaded) {
                        storage.load()
                        loaded = true
                    }
                    change()
                    storage.save()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dispatch(LibraryAction.PersistFailed(e.message ?: "Не удалось сохранить изменения"))
            }
        }
    }
}
