package com.example.offlineplayer.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URI
import java.util.UUID

class MediaStorageTest {

    private fun tempFile(): File = File.createTempFile("media", ".json").apply { deleteOnExit() }

    private fun media(url: String, type: MediaType, title: String) =
        SavedMedia(url = URI.create(url), type = type, title = title)

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
        storage.add(media("https://youtu.be/1", MediaType.Video, "One"))
        storage.add(media("https://youtu.be/2", MediaType.Music, "Two"))
        storage.save()

        val reloaded = MediaStorage(file)
        reloaded.load()
        assertEquals(storage.items.toSet(), reloaded.items.toSet())
    }

    @Test
    fun `cyrillic title survives save and load`() {
        val file = tempFile()
        val storage = MediaStorage(file)
        val russian = media("https://youtu.be/1", MediaType.Podcast, "Привет, мир — Ёж и ёлка")
            .copy(sourceTitle = "Канал «Тест»")
        storage.add(russian)
        storage.save()

        val reloaded = MediaStorage(file)
        reloaded.load()

        assertEquals(russian, reloaded.items.single())
    }

    @Test
    fun `adding item with existing id replaces it`() {
        val storage = MediaStorage(tempFile())
        val original = media("https://youtu.be/1", MediaType.Video, "One")
        storage.add(original)
        storage.add(original.copy(title = "One (updated)"))

        assertEquals(1, storage.items.size)
        assertEquals("One (updated)", storage.items.single().title)
    }

    @Test
    fun `removing by unknown id is a no-op`() {
        val storage = MediaStorage(tempFile())
        storage.add(media("https://youtu.be/1", MediaType.Video, "One"))
        assertFalse(storage.remove(UUID.randomUUID()))
        assertEquals(1, storage.items.size)
    }

    @Test
    fun `broken entries are skipped when loading`() {
        val file = tempFile()
        file.writeText(
            """
            [
              {"id":"11111111-1111-1111-1111-111111111111","url":"https://youtu.be/1","type":"Video","title":"Valid","downloadedAtEpochMillis":1700000000000},
              {"id":"22222222-2222-2222-2222-222222222222","type":"Video","title":"Missing url","downloadedAtEpochMillis":1700000000000},
              {"id":"33333333-3333-3333-3333-333333333333","url":"https://youtu.be/2","type":"NOT_A_TYPE","title":"Bad type","downloadedAtEpochMillis":1700000000000},
              {"url":"https://youtu.be/3","type":"Video","title":"Missing id","downloadedAtEpochMillis":1700000000000},
              {"id":"not-a-uuid","url":"https://youtu.be/4","type":"Video","title":"Bad id","downloadedAtEpochMillis":1700000000000}
            ]
            """.trimIndent()
        )
        val storage = MediaStorage(file)
        storage.load()
        assertEquals(1, storage.items.size)
        assertEquals("Valid", storage.items.single().title)
    }

    @Test
    fun `updateProgress replaces item immutably and keeps duration`() {
        val storage = MediaStorage(tempFile())
        val original = media("https://youtu.be/1", MediaType.Video, "One")
            .copy(position = Position(progressSeconds = 0, durationSeconds = 300))
        storage.add(original)

        assertTrue(storage.updateProgress(original.id, 120))
        assertEquals(Position(progressSeconds = 120, durationSeconds = 300), storage.items.single().position)
        assertEquals(0, original.position.progressSeconds)
    }
}
