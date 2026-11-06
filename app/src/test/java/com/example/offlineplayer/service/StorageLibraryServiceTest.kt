package com.example.offlineplayer.service

import com.example.offlineplayer.media.MediaStorage
import com.example.offlineplayer.media.SavedMedia
import com.example.offlineplayer.state.LibraryAction
import com.example.offlineplayer.state.LibraryState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URI
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StorageLibraryServiceTest {

    private fun tempFile(): File = File.createTempFile("service", ".json").apply { deleteOnExit() }

    private fun media(title: String, downloadedAtMillis: Long) = SavedMedia(
        url = URI.create("https://youtu.be/$title"),
        title = title,
        downloadedAt = Instant.ofEpochMilli(downloadedAtMillis)
    )

    private class Setup(val file: File, scope: TestScope) {
        val dispatched = mutableListOf<LibraryAction>()
        val service = StorageLibraryService(
            storage = MediaStorage(file),
            dispatch = { dispatched += it },
            scope = scope,
            io = StandardTestDispatcher(scope.testScheduler)
        )
    }

    private fun reloaded(file: File): List<SavedMedia> =
        MediaStorage(file).also { it.load() }.items

    @Test
    fun `refresh reads the file and reports the list with the request id`() = runTest {
        val file = tempFile()
        val a = media("a", 1_000)
        MediaStorage(file).also { it.add(a); it.save() }
        val setup = Setup(file, this)

        setup.service.onAction(LibraryAction.RefreshRequested, LibraryState(loadRequestId = 7))
        advanceUntilIdle()

        assertEquals(listOf<LibraryAction>(LibraryAction.ListLoaded(7, listOf(a))), setup.dispatched)
    }

    @Test
    fun `saved media is persisted without losing the existing records`() = runTest {
        val file = tempFile()
        val a = media("a", 1_000)
        val b = media("b", 2_000)
        MediaStorage(file).also { it.add(a); it.save() }
        val setup = Setup(file, this)

        setup.service.onAction(LibraryAction.MediaSaved(b), LibraryState())
        advanceUntilIdle()

        assertEquals(setOf(a, b), reloaded(file).toSet())
        assertTrue(setup.dispatched.isEmpty())
    }

    @Test
    fun `delete and undo are persisted in order`() = runTest {
        val file = tempFile()
        val a = media("a", 1_000)
        MediaStorage(file).also { it.add(a); it.save() }
        val setup = Setup(file, this)

        setup.service.onAction(LibraryAction.DeleteClicked(a.id), LibraryState())
        advanceUntilIdle()
        assertEquals(emptyList<SavedMedia>(), reloaded(file))

        setup.service.onAction(LibraryAction.UndoDeleteClicked(a), LibraryState())
        advanceUntilIdle()
        assertEquals(listOf(a), reloaded(file))
    }
}
