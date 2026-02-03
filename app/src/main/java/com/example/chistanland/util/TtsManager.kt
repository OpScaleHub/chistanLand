package com.github.opscalehub.chistanland.util

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.*
import kotlin.coroutines.resume

/**
 * TtsManager handles Text-to-Speech functionality for Persian language.
 * 
 * Note: Persian (Farsi) offline support depends on the Google TTS Engine 
 * having the voice data downloaded on the device.
 */
class TtsManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isPersianReady = false
    private var mediaPlayer: MediaPlayer? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                checkPersianSupport()
            } else {
                Log.e("TtsManager", "TTS Initialization failed!")
            }
        }
    }

    private fun checkPersianSupport() {
        val faLocale = Locale("fa", "IR")
        val result = tts?.setLanguage(faLocale)
        
        // result >= 0 means LANG_AVAILABLE, LANG_COUNTRY_AVAILABLE, or LANG_COUNTRY_VAR_AVAILABLE
        isPersianReady = (result != null && result >= TextToSpeech.LANG_AVAILABLE)
        
        Log.d("TtsManager", "Persian Support Status: $result (Ready: $isPersianReady)")
        
        tts?.setSpeechRate(0.9f)
        tts?.setPitch(1.0f)
    }

    suspend fun speak(text: String) {
        if (isPersianReady) {
            speakNative(text)
        } else {
            Log.w("TtsManager", "Native Persian not ready. Using Online Fallback...")
            speakOnline(text)
        }
    }

    private suspend fun speakNative(text: String) = suspendCancellableCoroutine<Unit> { continuation ->
        val utteranceId = UUID.randomUUID().toString()
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                Log.d("TtsManager", "Started speaking native: $text")
            }
            override fun onDone(id: String?) { 
                if (id == utteranceId && continuation.isActive) continuation.resume(Unit) 
            }
            override fun onError(id: String?) { 
                Log.e("TtsManager", "Native TTS Error for: $text")
                if (id == utteranceId && continuation.isActive) continuation.resume(Unit) 
            }
        })

        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        
        if (result == TextToSpeech.ERROR) {
            Log.e("TtsManager", "Failed to invoke speak() method")
            if (continuation.isActive) continuation.resume(Unit)
        }

        continuation.invokeOnCancellation { tts?.stop() }
    }

    private suspend fun speakOnline(text: String) = withContext(Dispatchers.Main) {
        return@withContext suspendCancellableCoroutine<Unit> { continuation ->
            try {
                mediaPlayer?.release()
                val encodedText = URLEncoder.encode(text, "UTF-8")
                // Using Google Translate TTS endpoint as fallback
                val url = "https://translate.google.com/translate_tts?ie=UTF-8&q=$encodedText&tl=fa&client=tw-ob"
                
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    // Correct way to set data source for URLs
                    setDataSource(url)
                    
                    setOnPreparedListener { 
                        it.start() 
                    }
                    setOnCompletionListener {
                        it.release()
                        mediaPlayer = null
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("TtsManager", "Online TTS Error: what=$what, extra=$extra")
                        if (continuation.isActive) continuation.resume(Unit)
                        true
                    }
                    prepareAsync()
                }

                continuation.invokeOnCancellation {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                }
            } catch (e: Exception) {
                Log.e("TtsManager", "Online fallback failed", e)
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    /**
     * Opens Android TTS settings so the user can download Persian voice data.
     */
    fun openTtsSettings() {
        try {
            val intent = Intent()
            intent.action = "com.android.settings.TTS_SETTINGS"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("TtsManager", "Could not open TTS settings", e)
        }
    }

    fun release() {
        tts?.shutdown()
        mediaPlayer?.release()
    }
}
