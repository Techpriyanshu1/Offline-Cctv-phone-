package com.example.motion

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlertPlayer(private val context: Context) {

    private var toneGen: ToneGenerator? = null
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 85)
        } catch (e: Exception) {
            try {
                toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            } catch (e2: Exception) {}
        }
    }

    fun playMotionChime() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
        } catch (e: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 150, 100, 150), -1)
            }
        } catch (e: Exception) {}
    }

    fun playAlertSiren() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
        } catch (e: Exception) {}
    }

    fun release() {
        try {
            toneGen?.release()
        } catch (e: Exception) {}
        toneGen = null
    }
}
