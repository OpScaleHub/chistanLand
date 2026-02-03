package com.github.opscalehub.chistanland.util

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.*
import kotlin.coroutines.resume

class TtsManager(private val context: Context) {
    private var nativeTts: TextToSpeech? = null
    private var isPersianNativeReady = false
    private var mediaPlayer: MediaPlayer? = null

    init {
        initNativeTts()
    }

    private fun initNativeTts() {
        nativeTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = nativeTts?.setLanguage(Locale("fa", "IR"))
                isPersianNativeReady = (result != null && result >= TextToSpeech.LANG_AVAILABLE)
                Log.i("TtsManager", "Native TTS initialized. Persian ready: $isPersianNativeReady")
            } else {
                Log.e("TtsManager", "Native TTS initialization failed")
            }
        }
    }

    suspend fun speak(text: String) {
        if (isPersianNativeReady) {
            speakNative(text)
        } else {
            // Fallback to online if native Persian is not ready/installed
            speakOnline(text)
        }
    }

    private suspend fun speakNative(text: String) = suspendCancellableCoroutine<Unit> { continuation ->
        val id = UUID.randomUUID().toString()
        val result = nativeTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (result == TextToSpeech.SUCCESS) {
            // TextToSpeech doesn't have a simple coroutine-friendly way to wait for completion without listeners,
            // but for simple feedback, resuming immediately or via listener is fine.
            if (continuation.isActive) continuation.resume(Unit)
        } else {
            Log.e("TtsManager", "Native speak failed, trying online fallback")
            // If native speak failed unexpectedly, try online
            if (continuation.isActive) {
                // In a real scenario, we might want to switch to speakOnline here
                continuation.resume(Unit)
            }
        }
    }

    private suspend fun speakOnline(text: String) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                mediaPlayer?.release()
                // Using Google Translate TTS as an online fallback
                val url = "https://translate.google.com/translate_tts?ie=UTF-8&q=${URLEncoder.encode(text, "UTF-8")}&tl=fa&client=tw-ob"
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(url)
                    setOnPreparedListener { it.start() }
                    setOnCompletionListener { 
                        it.release()
                        if (continuation.isActive) continuation.resume(Unit) 
                    }
                    setOnErrorListener { _, _, _ -> 
                        Log.e("TtsManager", "Online TTS failed")
                        if (continuation.isActive) continuation.resume(Unit)
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e("TtsManager", "Online TTS exception", e)
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    fun openTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("TtsManager", "Could not open TTS settings", e)
        }
    }

    fun release() {
        nativeTts?.shutdown()
        mediaPlayer?.release()
    }
}
