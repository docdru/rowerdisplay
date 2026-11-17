package io.github.docdru.rowerdisplay.ble

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RowerMatcherTest {
    private val other = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")

    @Test
    fun uuidsAreTheFtmsOnes() {
        assertEquals("00001826-0000-1000-8000-00805f9b34fb", RowerMatcher.FTMS_SERVICE.toString())
        assertEquals("00002ad1-0000-1000-8000-00805f9b34fb", RowerMatcher.ROWER_DATA.toString())
        assertEquals("00002902-0000-1000-8000-00805f9b34fb", RowerMatcher.CLIENT_CONFIG.toString())
    }

    @Test
    fun domyosRowerByName() {
        // The exact name from the owner's nRF Connect log, and an upper-case variant.
        assertTrue(RowerMatcher.isRower("Domyos-ROW-1580", emptyList(), null))
        assertTrue(RowerMatcher.isRower("DOMYOS-ROW-1580", emptyList(), null))
    }

    @Test
    fun ftmsServiceDataSaysRower() {
        // flags 0x01, machine type 0x0010 (rower).
        val data = byteArrayOf(0x01, 0x10, 0x00)
        assertTrue(RowerMatcher.isRower(null, listOf(RowerMatcher.FTMS_SERVICE), data))
    }

    @Test
    fun ftmsBikeIsSkipped() {
        // machine type 0x0020 (indoor bike).
        val data = byteArrayOf(0x01, 0x20, 0x00)
        assertFalse(RowerMatcher.isRower("Some bike", listOf(RowerMatcher.FTMS_SERVICE), data))
    }

    @Test
    fun ftmsServiceWithoutServiceData() {
        assertTrue(RowerMatcher.isRower(null, listOf(RowerMatcher.FTMS_SERVICE), null))
    }

    @Test
    fun unrelatedDevicesAreSkipped() {
        assertFalse(RowerMatcher.isRower("Heart strap", listOf(other), null))
        assertFalse(RowerMatcher.isRower(null, emptyList(), null))
        // Too short service data falls back to the advertised services.
        assertFalse(RowerMatcher.isRower(null, emptyList(), byteArrayOf(0x01)))
    }
}
