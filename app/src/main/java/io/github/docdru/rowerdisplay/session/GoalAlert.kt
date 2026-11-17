package io.github.docdru.rowerdisplay.session

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** One beep and one vibration when the goal is reached. Failures are ignored. */
class GoalAlert(context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    fun play() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, DURATION_MS)
            handler.postDelayed({ tone.release() }, DURATION_MS + 200L)
        }
        runCatching {
            val effect = VibrationEffect.createOneShot(
                DURATION_MS.toLong(),
                VibrationEffect.DEFAULT_AMPLITUDE,
            )
            vibrator()?.vibrate(effect)
        }
    }

    private fun vibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Vibrator::class.java)
    }

    private companion object {
        const val VOLUME = 90
        const val DURATION_MS = 500
    }
}
