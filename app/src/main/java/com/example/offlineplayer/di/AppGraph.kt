package com.example.offlineplayer.di

import android.content.Context
import com.example.offlineplayer.media.MediaStorage
import com.example.offlineplayer.service.LibraryService
import com.example.offlineplayer.service.StorageLibraryService
import com.example.offlineplayer.state.LibraryAction
import com.example.offlineplayer.state.LibraryActionExecutor
import com.example.offlineplayer.state.LibraryState
import com.example.offlineplayer.state.LibraryStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** Настройки, а не зависимости: меняются без правки кода компонентов. */
class LibraryConfig(val fileName: String)

/**
 * Контейнер зависимостей: единственное место, где создаются хранилище, сервис, стор и исполнитель
 * и связываются между собой. Он знает все конкретные классы, а про него не знает никто,
 * кроме точки входа ([com.example.offlineplayer.OfflinePlayerApp]).
 *
 * Всё, что владеет ресурсом или работает при создании, создаётся лениво, при первом обращении:
 * до этого момента не трогаются ни файлы, ни корутины. Конфиг — просто значение, ему лень не нужна.
 *
 * Компонент:
 * - Вход: [context] (нужен только ради каталога файлов приложения).
 * - Выход: [libraryStore] для экранов.
 * - Конфигурация: [LibraryConfig].
 * - Зависимости: Android-контекст, диспетчеры корутин.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppGraph(private val context: Context) {

    private val config = LibraryConfig(fileName = "saved_media.json")

    private val appScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    // Однопоточный: MediaStorage не потокобезопасен, а команды должны идти в порядке получения.
    private val ioDispatcher: CoroutineDispatcher by lazy { Dispatchers.IO.limitedParallelism(1) }

    private val storage: MediaStorage by lazy { MediaStorage(File(context.filesDir, config.fileName)) }

    private val libraryService: LibraryService by lazy {
        StorageLibraryService(
            storage = storage,
            // Лямбда, а не ссылка libraryStore::dispatch: ссылка сразу потребовала бы стор,
            // а стор при создании требует сервис, и ленивая инициализация зациклилась бы.
            dispatch = { action -> libraryStore.dispatch(action) },
            scope = appScope,
            io = ioDispatcher
        )
    }

    // Первая загрузка начинается при первом обращении к стору, а не при создании графа:
    // пока интерфейсу стор не нужен, файл не читается.
    val libraryStore: LibraryStore by lazy {
        LibraryStore(
            // Пока не пришёл первый ответ с диска, экран честно показывает загрузку.
            initial = LibraryState(isLoading = true),
            executor = LibraryActionExecutor(),
            scope = appScope
        ).also { store ->
            store.addObserver(libraryService::onAction)
            store.dispatch(LibraryAction.RefreshRequested)
        }
    }
}
