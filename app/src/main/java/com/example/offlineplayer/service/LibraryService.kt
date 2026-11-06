package com.example.offlineplayer.service

import com.example.offlineplayer.state.LibraryAction
import com.example.offlineplayer.state.LibraryState

/**
 * Сервис «Библиотеки»: всё, что уходит за пределы экрана (диск, позже сеть и загрузки).
 * Подписан на действия через [com.example.offlineplayer.state.LibraryStore], а результат своей
 * работы возвращает в систему новым действием, а не записью в состояние.
 *
 * Интерфейс нужен, чтобы в тестах подставить поддельный сервис (например, с задержкой).
 */
interface LibraryService {
    /** Вызывается стором после обработки каждого действия: само действие и состояние после него. */
    fun onAction(action: LibraryAction, state: LibraryState)
}
