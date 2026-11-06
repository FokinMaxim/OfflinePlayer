package com.example.offlineplayer.state

import com.example.offlineplayer.media.SavedMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.time.Instant
import java.util.UUID

class LibraryActionExecutorTest {

    private val executor = LibraryActionExecutor()

    private fun media(title: String, downloadedAtMillis: Long) = SavedMedia(
        url = URI.create("https://youtu.be/$title"),
        title = title,
        downloadedAt = Instant.ofEpochMilli(downloadedAtMillis)
    )

    private val a = media("a", 1_000)
    private val b = media("b", 2_000)
    private val c = media("c", 3_000)

    @Test
    fun `delete removes the item and remembers it for undo`() {
        val before = LibraryState(items = listOf(b, a))

        val after = executor.execute(before, LibraryAction.DeleteClicked(a.id))

        assertEquals(listOf(b), after.items)
        assertEquals(a, after.lastDeleted)
    }

    @Test
    fun `delete of an unknown id leaves the state unchanged`() {
        val before = LibraryState(items = listOf(b, a))

        assertSame(before, executor.execute(before, LibraryAction.DeleteClicked(UUID.randomUUID())))
    }

    @Test
    fun `undo puts the item back in its place and clears the offer`() {
        val before = LibraryState(items = listOf(c, a), lastDeleted = b)

        val after = executor.execute(before, LibraryAction.UndoDeleteClicked(b))

        assertEquals(listOf(c, b, a), after.items)
        assertNull(after.lastDeleted)
    }

    @Test
    fun `saving replaces an existing item and keeps newest first`() {
        val before = LibraryState(items = listOf(b, a))

        val after = executor.execute(before, LibraryAction.MediaSaved(a.copy(title = "a-edited")))

        assertEquals(listOf("b", "a-edited"), after.items.map { it.title })
    }

    @Test
    fun `saving a new item inserts it by download time`() {
        val before = LibraryState(items = listOf(b, a))

        val after = executor.execute(before, LibraryAction.MediaSaved(c))

        assertEquals(listOf(c, b, a), after.items)
    }

    @Test
    fun `refresh starts loading and numbers the request`() {
        val after = executor.execute(LibraryState(error = "old"), LibraryAction.RefreshRequested)

        assertTrue(after.isLoading)
        assertEquals(1, after.loadRequestId)
        assertNull(after.error)
    }

    @Test
    fun `loaded list is sorted and stops loading`() {
        val before = LibraryState(isLoading = true, loadRequestId = 1)

        val after = executor.execute(before, LibraryAction.ListLoaded(1, listOf(a, c, b)))

        assertEquals(listOf(c, b, a), after.items)
        assertFalse(after.isLoading)
    }

    @Test
    fun `load failure stops loading and keeps the message`() {
        val before = LibraryState(isLoading = true, loadRequestId = 1)

        val after = executor.execute(before, LibraryAction.LoadFailed(1, "нет доступа"))

        assertEquals("нет доступа", after.error)
        assertFalse(after.isLoading)
    }

    @Test
    fun `answer to a stale request is ignored`() {
        val before = LibraryState(items = listOf(c), isLoading = true, loadRequestId = 2)

        assertSame(before, executor.execute(before, LibraryAction.ListLoaded(1, listOf(a))))
        assertSame(before, executor.execute(before, LibraryAction.LoadFailed(1, "поздно")))
    }

    @Test
    fun `error can be dismissed`() {
        val after = executor.execute(LibraryState(error = "x"), LibraryAction.ErrorDismissed)

        assertNull(after.error)
    }
}
