package io.github.docdru.rowerdisplay.ui

/** Formats whole seconds as m:ss, e.g. 128 -> "2:08". Negative input is clamped to 0. */
fun formatMinutesSeconds(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

/** Formats metres with a thin space as thousands separator, e.g. 1720 -> "1 720". */
fun formatMetres(metres: Int): String {
    val digits = metres.coerceAtLeast(0).toString()
    return digits.reversed().chunked(3).joinToString(" ").reversed()
}
