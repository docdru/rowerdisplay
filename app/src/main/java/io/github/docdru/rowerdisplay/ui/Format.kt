package io.github.docdru.rowerdisplay.ui

/**
 * Formats whole seconds as m:ss below an hour and h:mm:ss from an hour on,
 * e.g. 128 -> "2:08", 3725 -> "1:02:05". Negative input is clamped to 0.
 */
fun formatClock(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return if (h > 0) "$h:${m.twoDigits()}:${s.twoDigits()}" else "$m:${s.twoDigits()}"
}

/** Formats metres with a thin space as thousands separator, e.g. 1720 -> "1 720". */
fun formatMetres(metres: Int): String {
    val digits = metres.coerceAtLeast(0).toString()
    return digits.reversed().chunked(3).joinToString(" ").reversed()
}

private fun Int.twoDigits(): String = if (this < 10) "0$this" else toString()
