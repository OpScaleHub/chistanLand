package com.github.opscalehub.chistanland.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AudioManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()
    private var lastErrorTime = 0L
    private val TAG = "AudioManager"

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
     * Wrapped in try-catch to prevent crashes on unstable emulator media stacks.
     */
    suspend fun playSound(resourceName: String) {
        try {
            val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
            if (resId == 0) return

            mediaPlayer?.let {
                try {
                    if (it.isPlaying) it.stop()
                } catch (e: Exception) {}
                it.release()
            }

            return suspendCancellableCoroutine { continuation ->
                try {
                    val mp = MediaPlayer.create(context, resId)
                    mediaPlayer = mp

                    if (mp == null) {
                        if (continuation.isActive) continuation.resume(Unit)
                        return@suspendCancellableCoroutine
                    }

                    mp.setOnCompletionListener {
                        it.release()
                        if (mediaPlayer == it) mediaPlayer = null
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    mp.setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer Error: what=$what, extra=$extra")
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
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize MediaPlayer: ${e.message}")
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Outer playSound failure: ${e.message}")
        }
    }

    /**
     * Plays short SFX like button clicks or errors immediately.
     */
    fun playSoundAsync(resourceName: String) {
        try {
            if (resourceName == "error_sound") {
                val now = System.currentTimeMillis()
                if (now - lastErrorTime < 800) return
                lastErrorTime = now
            }

            val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
            if (resId == 0) return

            val soundId = soundMap.getOrPut(resourceName) {
                soundPool.load(context, resId, 1)
            }

            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        } catch (e: Exception) {
            Log.e(TAG, "SoundPool play failure: ${e.message}")
        }
    }

    fun release() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {}
        mediaPlayer = null
        try {
            soundPool.release()
        } catch (e: Exception) {}
    }
}
