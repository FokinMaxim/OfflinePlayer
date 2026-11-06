package com.example.offlineplayer.state

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Хранит состояние и принимает действия. Любое действие, откуда бы оно ни пришло (экран, сервис),
 * встаёт в одну очередь и обрабатывается строго по одному — это и есть единственная точка изменения.
 *
 * Компонент:
 * - Вход: действия через [dispatch] (из любого потока).
 * - Выход: [state] для представления; пары «действие + новое состояние» для наблюдателей-сервисов.
 * - Конфигурация: начальное состояние.
 * - Зависимости: [LibraryActionExecutor], [CoroutineScope], в котором работает цикл обработки.
 */
class LibraryStore(
    initial: LibraryState,
    private val executor: LibraryActionExecutor,
    scope: CoroutineScope
) {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val observers = CopyOnWriteArrayList<(LibraryAction, LibraryState) -> Unit>()
    private val queue = Channel<LibraryAction>(Channel.UNLIMITED)

    init {
        scope.launch { //(context = CoroutineName('fff'))
            for (action in queue) {
                val newState = executor.execute(_state.value, action)
                _state.value = newState
                observers.forEach { observer -> observer(action, newState) }
            }
        }
    }

    fun dispatch(action: LibraryAction) {
        queue.trySend(action)
    }

    /** Подписка сервиса: он получает каждое обработанное действие и состояние после него. */
    fun addObserver(observer: (LibraryAction, LibraryState) -> Unit) {
        observers.add(observer)
    }
}
