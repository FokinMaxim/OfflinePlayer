package com.example.offlineplayer.ui.list

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaFormattingTest {

    @Test
    fun `unknown duration is shown as dash`() {
        assertEquals("—", formatDuration(null))
    }

    @Test
    fun `duration below an hour is minutes and seconds`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:59", formatDuration(59))
        assertEquals("1:05", formatDuration(65))
        assertEquals("59:59", formatDuration(3599))
    }

    @Test
    fun `duration from an hour includes hours`() {
        assertEquals("1:00:00", formatDuration(3600))
        assertEquals("1:02:05", formatDuration(3725))
    }

    @Test
    fun `dark text on light background and light text on dark`() {
        assertEquals(Color.Black, readableContentColor(Color.White))
        assertEquals(Color.Black, readableContentColor(Color(0xFFCFE6B8)))
        assertEquals(Color.White, readableContentColor(Color.Black))
        assertEquals(Color.White, readableContentColor(Color(0xFF073642)))
    }
}
