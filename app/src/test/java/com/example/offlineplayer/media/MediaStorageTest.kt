package com.example.offlineplayer.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

class MediaStorageTest {

    private fun tempFile(): File = File.createTempFile("media", ".json").apply { deleteOnExit() }

    @Test
    fun `loading missing file results in empty list`() {
        val file = tempFile().apply { delete() }
        val storage = MediaStorage(file)
        storage.load()
        assertTrue(storage.items.isEmpty())
    }

    @Test
    fun `save then load restores items`() {
        val file = tempFile()
        val storage = MediaStorage(file)
        storage.add(SavedMedia(url = "https://youtu.be/1", type = MediaType.Video, title = "One"))
        storage.add(SavedMedia(url = "https://youtu.be/2", type = MediaType.Music, title = "Two"))
        storage.save()

        val reloaded = MediaStorage(file)
        reloaded.load()
        assertEquals(storage.items.toSet(), reloaded.items.toSet())
    }

    @Test
    fun `adding item with existing id replaces it`() {
        val storage = MediaStorage(tempFile())
        val original = SavedMedia(url = "https://youtu.be/1", type = MediaType.Video, title = "One")
        storage.add(original)
        storage.add(original.copy(title = "One (updated)"))

        assertEquals(1, storage.items.size)
        assertEquals("One (updated)", storage.items.single().title)
    }

    @Test
    fun `removing by unknown id is a no-op`() {
        val storage = MediaStorage(tempFile())
        storage.add(SavedMedia(url = "https://youtu.be/1", type = MediaType.Video, title = "One"))
        assertFalse(storage.remove(UUID.randomUUID()))
        assertEquals(1, storage.items.size)
    }

    @Test
    fun `broken entries are skipped when loading`() {
        val file = tempFile()
        file.writeText(
            """
            [
              {"url":"https://youtu.be/1","type":"Video","title":"Valid"},
              {"type":"Video","title":"Missing url"},
              {"url":"https://youtu.be/2","type":"NOT_A_TYPE","title":"Bad type"}
            ]
            """.trimIndent()
        )
        val storage = MediaStorage(file)
        storage.load()
        assertEquals(1, storage.items.size)
        assertEquals("Valid", storage.items.single().title)
    }

    @Test
    fun `updateProgress replaces item immutably`() {
        val storage = MediaStorage(tempFile())
        val media = SavedMedia(url = "https://youtu.be/1", type = MediaType.Video, title = "One")
        storage.add(media)

        assertTrue(storage.updateProgress(media.id, 120))
        assertEquals(120, storage.items.single().progressSeconds)
        assertEquals(0, media.progressSeconds)
    }
}
