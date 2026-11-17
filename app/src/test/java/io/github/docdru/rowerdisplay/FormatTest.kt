package io.github.docdru.rowerdisplay

import io.github.docdru.rowerdisplay.ui.formatMetres
import io.github.docdru.rowerdisplay.ui.formatMinutesSeconds
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun minutesSeconds() {
        assertEquals("0:00", formatMinutesSeconds(0))
        assertEquals("2:08", formatMinutesSeconds(128))
        assertEquals("60:00", formatMinutesSeconds(3600))
        assertEquals("0:00", formatMinutesSeconds(-5))
    }

    @Test
    fun metres() {
        assertEquals("0", formatMetres(0))
        assertEquals("720", formatMetres(720))
        assertEquals("1 720", formatMetres(1720))
        assertEquals("12 345", formatMetres(12345))
        assertEquals("1 000 000", formatMetres(1_000_000))
    }
}
