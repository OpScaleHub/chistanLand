package com.github.opscalehub.chistanland.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AudioManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()
    private var lastErrorTime = 0L

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attrs)
            .build()
    }

    /**
     * Plays a narrative sound (long) and suspends until finished.
     */
    suspend fun playSound(resourceName: String) {
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        if (resId == 0) return

        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }

        return suspendCancellableCoroutine { continuation ->
            val mp = MediaPlayer.create(context, resId)
            mediaPlayer = mp

            if (mp == null) {
                continuation.resume(Unit)
                return@suspendCancellableCoroutine
            }

            mp.setOnCompletionListener {
                it.release()
                if (mediaPlayer == it) mediaPlayer = null
                if (continuation.isActive) continuation.resume(Unit)
            }

            mp.setOnErrorListener { _, _, _ ->
                mp.release()
                if (mediaPlayer == mp) mediaPlayer = null
                if (continuation.isActive) continuation.resume(Unit)
                true
            }

            continuation.invokeOnCancellation {
                try {
                    if (mp.isPlaying) mp.stop()
                } catch (e: Exception) {}
                mp.release()
                if (mediaPlayer == mp) mediaPlayer = null
            }

            mp.start()
        }
    }

    /**
     * Plays short SFX like button clicks or errors immediately.
     */
    fun playSoundAsync(resourceName: String) {
        // Prevent "machine-gun" error sounds to reduce stress
        if (resourceName == "error_sound") {
            val now = System.currentTimeMillis()
            if (now - lastErrorTime < 800) return // Cooldown for error sound
            lastErrorTime = now
        }

        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        if (resId == 0) return

        // Load into SoundPool if not already loaded (simple cache)
        val soundId = soundMap.getOrPut(resourceName) {
            soundPool.load(context, resId, 1)
        }

        // SoundPool.play is non-blocking and very low latency
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        soundPool.release()
    }
}
