package com.piania.app.util

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class Metronome(
    private val scope: CoroutineScope
) {
    private var job: Job? = null
    private var currentBpm: Int = 90
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    fun start(bpm: Int) {
        setTempo(bpm)
        if (job?.isActive == true) return

        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 55)
                delay(intervalMs())
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        runCatching { toneGenerator.stopTone() }
    }

    fun setTempo(bpm: Int) {
        currentBpm = bpm.coerceIn(30, 240)
    }

    fun release() {
        stop()
        toneGenerator.release()
    }

    private fun intervalMs(): Long = (60_000.0 / currentBpm).toLong()
}
