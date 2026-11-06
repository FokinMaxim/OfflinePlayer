package com.example.offlineplayer.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class RoutesTest {

    @Test
    fun `edit route without an id means creating a new record`() {
        assertNull(EditRoute().mediaId)
        assertNull(EditRoute().mediaUuid)
    }

    @Test
    fun `edit route exposes a valid id as uuid`() {
        val id = UUID.randomUUID()

        assertEquals(id, EditRoute(mediaId = id.toString()).mediaUuid)
    }

    @Test
    fun `edit route with a malformed id has no uuid`() {
        assertNull(EditRoute(mediaId = "not-a-uuid").mediaUuid)
    }

    @Test
    fun `color picker route starts from white by default`() {
        assertEquals(Color.White.toArgb(), ColorPickerRoute().color)
    }
}
