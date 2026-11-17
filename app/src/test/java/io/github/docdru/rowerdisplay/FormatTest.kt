package io.github.docdru.rowerdisplay

import io.github.docdru.rowerdisplay.ui.formatClock
import io.github.docdru.rowerdisplay.ui.formatMetres
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun clock() {
        assertEquals("0:00", formatClock(0))
        assertEquals("0:59", formatClock(59))
        assertEquals("1:00", formatClock(60))
        assertEquals("2:08", formatClock(128))
        assertEquals("59:59", formatClock(3599))
        assertEquals("1:00:00", formatClock(3600))
        assertEquals("1:02:05", formatClock(3725))
        assertEquals("0:00", formatClock(-5))
    }

    @Test
    fun metres() {
        assertEquals("0", formatMetres(0))
        assertEquals("720", formatMetres(720))
        assertEquals("1 720", formatMetres(1720))
        assertEquals("12 345", formatMetres(12345))
        assertEquals("1 000 000", formatMetres(1_000_000))
        assertEquals("0", formatMetres(-3))
    }
}
