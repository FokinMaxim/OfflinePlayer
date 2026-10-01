package com.example.offlineplayer.media

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SavedMediaJsonTest {

    @Test
    fun `round trip preserves object built with defaults`() {
        val media = SavedMedia(url = "https://youtu.be/abc", type = MediaType.Video, title = "Test video")
        assertEquals(media, SavedMedia.parse(media.json))
    }

    @Test
    fun `round trip preserves object with custom values`() {
        val media = SavedMedia(
            url = "https://youtu.be/xyz",
            type = MediaType.Music,
            title = "Test track",
            progressSeconds = 42,
            durationSeconds = 210,
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
        val media = SavedMedia(url = "https://youtu.be/abc", type = MediaType.Video, title = "Test")
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
            url = "https://youtu.be/abc",
            type = MediaType.Video,
            title = "Test",
            isOnlyAudio = true,
            downloadStatus = DownloadStatus.Failed,
            tagColor = Color(0xFFDC322F),
            watchByEpochMillis = 1_700_000_000_000
        )
        val json = media.json
        assertEquals(true, json.getBoolean("isOnlyAudio"))
        assertEquals("Failed", json.getString("downloadStatus"))
        assertEquals(Color(0xFFDC322F).toArgb(), json.getInt("tagColor"))
        assertEquals(1_700_000_000_000, json.getLong("watchByEpochMillis"))
    }

    @Test
    fun `parse returns null when url is missing`() {
        val json = JSONObject().apply {
            put("type", "Video")
            put("title", "No url")
        }
        assertNull(SavedMedia.parse(json))
    }

    @Test
    fun `parse returns null for unknown type`() {
        val json = JSONObject().apply {
            put("url", "https://youtu.be/abc")
            put("title", "Weird type")
            put("type", "GIF")
        }
        assertNull(SavedMedia.parse(json))
    }
}
