package com.example.offlineplayer.state

import com.example.offlineplayer.media.SavedMedia
import com.example.offlineplayer.service.LibraryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

/** Поддельный сервис: n-й запрос отвечает через delays[n] мс списком answers[n]. */
private class SlowFakeLibraryService(
    private val dispatch: (LibraryAction) -> Unit,
    private val scope: CoroutineScope,
    private val delays: List<Long>,
    private val answers: List<List<SavedMedia>>
) : LibraryService {

    private var calls = 0

    override fun onAction(action: LibraryAction, state: LibraryState) {
        if (action != LibraryAction.RefreshRequested) return
        val call = calls++
        val requestId = state.loadRequestId
        scope.launch {
            delay(delays[call])
            dispatch(LibraryAction.ListLoaded(requestId, answers[call]))
        }
    }
}

/**
 * Два клика «Обновить» подряд. Цикл стора живёт в обычном (не background) скоупе: `advanceUntilIdle()`
 * не ждёт фоновые задачи `backgroundScope`, и опоздавший ответ тихо остался бы необработанным.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private class TwoRefreshes(testScope: TestScope, delays: List<Long>, answers: List<List<SavedMedia>>) {
    private val storeScope = CoroutineScope(UnconfinedTestDispatcher(testScope.testScheduler))
    val store = LibraryStore(LibraryState(), LibraryActionExecutor(), storeScope)

    init {
        store.addObserver(SlowFakeLibraryService(store::dispatch, testScope, delays, answers)::onAction)
        store.dispatch(LibraryAction.RefreshRequested)
        store.dispatch(LibraryAction.RefreshRequested)
    }

    fun close() = storeScope.cancel()
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRaceTest {

    private val old = SavedMedia(url = URI.create("https://youtu.be/old"), title = "old")
    private val fresh = SavedMedia(url = URI.create("https://youtu.be/new"), title = "new")

    @Test
    fun `slow answer to the first request does not overwrite the answer to the second`() = runTest {
        val race = TwoRefreshes(this, delays = listOf(2_000, 200), answers = listOf(listOf(old), listOf(fresh)))
        try {
            advanceTimeBy(300)
            assertEquals(listOf(fresh), race.store.state.value.items)
            assertFalse(race.store.state.value.isLoading)

            advanceUntilIdle()                               // опоздавший ответ первого запроса уже пришёл
            assertEquals(listOf(fresh), race.store.state.value.items)
            assertFalse(race.store.state.value.isLoading)
        } finally {
            race.close()
        }
    }

    @Test
    fun `loading indicator stays on until the latest request is answered`() = runTest {
        val race = TwoRefreshes(this, delays = listOf(200, 2_000), answers = listOf(listOf(old), listOf(fresh)))
        try {
            advanceTimeBy(300)                               // ответ первого пришёл, второго ещё нет
            assertTrue(race.store.state.value.isLoading)
            assertTrue(race.store.state.value.items.isEmpty())

            advanceUntilIdle()
            assertEquals(listOf(fresh), race.store.state.value.items)
            assertFalse(race.store.state.value.isLoading)
        } finally {
            race.close()
        }
    }
}
