package com.example.offlineplayer.media

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URI
import java.time.Instant

class SavedMediaJsonTest {

    private fun validJson() = JSONObject().apply {
        put("id", "11111111-1111-1111-1111-111111111111")
        put("url", "https://youtu.be/abc")
        put("type", "Video")
        put("title", "Title")
        put("downloadedAtEpochMillis", 1_700_000_000_000)
    }

    @Test
    fun `valid minimal json is parsed`() {
        assertNotNull(SavedMedia.parse(validJson()))
    }

    @Test
    fun `round trip preserves object built with defaults`() {
        val media = SavedMedia(url = URI.create("https://youtu.be/abc"), type = MediaType.Video, title = "Test video")
        assertEquals(media, SavedMedia.parse(media.json))
    }

    @Test
    fun `round trip preserves object with custom values`() {
        val media = SavedMedia(
            url = URI.create("https://youtu.be/xyz"),
            type = MediaType.Music,
            title = "Test track",
            position = Position(progressSeconds = 42, durationSeconds = 210),
            downloadedAt = Instant.ofEpochMilli(1_700_000_000_123),
            downloadStatus = DownloadStatus.Completed,
            sourceTitle = "Some Channel",
            isFavorite = true,
            isWatched = true,
            tagColor = Color(0xFF268BD2),
            watchByEpochMillis = 1_700_000_000_000
        )
        assertEquals(media, SavedMedia.parse(media.json))
    }

    @Test
    fun `default values are not written to json`() {
        val media = SavedMedia(url = URI.create("https://youtu.be/abc"), type = MediaType.Video, title = "Test")
        val json = media.json
        assertFalse(json.has("isOnlyAudio"))
        assertFalse(json.has("downloadStatus"))
        assertFalse(json.has("durationSeconds"))
        assertFalse(json.has("sourceTitle"))
        assertFalse(json.has("tagColor"))
        assertFalse(json.has("watchByEpochMillis"))
    }

    @Test
    fun `non-default values are written to json`() {
        val media = SavedMedia(
            url = URI.create("https://youtu.be/abc"),
            type = MediaType.Video,
            title = "Test",
            isOnlyAudio = true,
            position = Position(progressSeconds = 42, durationSeconds = 210),
            downloadedAt = Instant.ofEpochMilli(1_700_000_000_123),
            downloadStatus = DownloadStatus.Failed,
            tagColor = Color(0xFFDC322F),
            watchByEpochMillis = 1_700_000_000_000
        )
        val json = media.json
        assertEquals("https://youtu.be/abc", json.getString("url"))
        assertEquals(1_700_000_000_123, json.getLong("downloadedAtEpochMillis"))
        assertEquals(42, json.getInt("progressSeconds"))
        assertEquals(210, json.getInt("durationSeconds"))
        assertEquals(true, json.getBoolean("isOnlyAudio"))
        assertEquals("Failed", json.getString("downloadStatus"))
        assertEquals(Color(0xFFDC322F).toArgb(), json.getInt("tagColor"))
        assertEquals(1_700_000_000_000, json.getLong("watchByEpochMillis"))
    }

    @Test
    fun `negative position values are coerced to zero`() {
        val json = validJson().apply {
            put("progressSeconds", -5)
            put("durationSeconds", -10)
        }
        assertEquals(Position(progressSeconds = 0, durationSeconds = 0), SavedMedia.parse(json)?.position)
    }

    @Test
    fun `parse returns null when url is missing`() {
        assertNull(SavedMedia.parse(validJson().apply { remove("url") }))
    }

    @Test
    fun `parse returns null for url without scheme or host`() {
        assertNull(SavedMedia.parse(validJson().apply { put("url", "youtu.be/abc") }))
        assertNull(SavedMedia.parse(validJson().apply { put("url", "https://") }))
    }

    @Test
    fun `parse returns null for url with illegal characters`() {
        assertNull(SavedMedia.parse(validJson().apply { put("url", "https://exa mple.com/abc") }))
    }

    @Test
    fun `parse returns null for unknown type`() {
        assertNull(SavedMedia.parse(validJson().apply { put("type", "GIF") }))
    }

    @Test
    fun `parse returns null when id is missing`() {
        assertNull(SavedMedia.parse(validJson().apply { remove("id") }))
    }

    @Test
    fun `parse returns null for malformed id`() {
        assertNull(SavedMedia.parse(validJson().apply { put("id", "not-a-uuid") }))
        assertNull(SavedMedia.parse(validJson().apply { put("id", "1-1-1-1-1") }))
    }

    @Test
    fun `parse returns null when downloadedAt is missing or not a number`() {
        assertNull(SavedMedia.parse(validJson().apply { remove("downloadedAtEpochMillis") }))
        assertNull(SavedMedia.parse(validJson().apply { put("downloadedAtEpochMillis", "yesterday") }))
    }
}
