package com.github.opscalehub.chistanland.util

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
    private var isEnglishNativeReady = false
    private var mediaPlayer: MediaPlayer? = null
    private val TAG = "TtsManager"

    init {
        initNativeTts()
    }

    private fun initNativeTts() {
        nativeTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initial check for languages
                val faResult = nativeTts?.setLanguage(Locale("fa", "IR"))
                isPersianNativeReady = (faResult != null && faResult >= TextToSpeech.LANG_AVAILABLE)
                
                val enResult = nativeTts?.setLanguage(Locale.US)
                isEnglishNativeReady = (enResult != null && enResult >= TextToSpeech.LANG_AVAILABLE)
                
                Log.i(TAG, "Native TTS initialized. Persian Ready: $isPersianNativeReady, English Ready: $isEnglishNativeReady")
                
                setupProgressListener()
            } else {
                Log.e(TAG, "Native TTS initialization failed with status: $status")
            }
        }
    }

    private var currentContinuation: kotlinx.coroutines.CancellableContinuation<Unit>? = null

    private fun setupProgressListener() {
        nativeTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Native speech started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Native speech done: $utteranceId")
                resumeContinuation()
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Native speech error: $utteranceId")
                resumeContinuation()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "Native speech error code $errorCode: $utteranceId")
                resumeContinuation()
            }
        })
    }

    private fun resumeContinuation() {
        currentContinuation?.let {
            if (it.isActive) it.resume(Unit)
            currentContinuation = null
        }
    }

    suspend fun speak(text: String, locale: Locale = Locale("fa", "IR")) {
        val isPersian = locale.language == "fa"
        Log.d(TAG, "Request to speak: '$text' in ${locale.displayName}")
        
        if (isPersian) {
            if (isPersianNativeReady) {
                speakNative(text, locale)
            } else {
                Log.w(TAG, "Native Persian not ready. Falling back to Online Google TTS...")
                speakOnline(text, "fa")
            }
        } else if (locale.language == "en") {
            if (isEnglishNativeReady) {
                speakNative(text, locale)
            } else {
                Log.w(TAG, "Native English not ready. Falling back to Online Google TTS...")
                speakOnline(text, "en")
            }
        } else {
            speakOnline(text, locale.language)
        }
    }

    private suspend fun speakNative(text: String, locale: Locale) = suspendCancellableCoroutine<Unit> { continuation ->
        currentContinuation = continuation
        nativeTts?.language = locale
        
        val id = UUID.randomUUID().toString()
        val result = nativeTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        
        if (result != TextToSpeech.SUCCESS) {
            Log.e(TAG, "Native speak failed to queue. Result: $result")
            resumeContinuation()
        }
        
        continuation.invokeOnCancellation {
            nativeTts?.stop()
            currentContinuation = null
        }
    }

    private suspend fun speakOnline(text: String, langCode: String) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null

                val encodedText = URLEncoder.encode(text, "UTF-8")
                // Using client=gtx which is often more reliable than tw-ob
                val url = "https://translate.google.com/translate_tts?ie=UTF-8&tl=$langCode&client=gtx&q=$encodedText"
                Log.d(TAG, "Online TTS URL: $url")
                
                val headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, Uri.parse(url), headers)
                    
                    setOnPreparedListener { 
                        Log.d(TAG, "MediaPlayer prepared, starting online playback")
                        it.start() 
                    }
                    setOnCompletionListener { 
                        Log.d(TAG, "MediaPlayer online playback completed")
                        it.release()
                        mediaPlayer = null
                        if (continuation.isActive) continuation.resume(Unit) 
                    }
                    setOnErrorListener { mp, what, extra -> 
                        Log.e(TAG, "MediaPlayer Error: what=$what extra=$extra")
                        mp.release()
                        mediaPlayer = null
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
                Log.e(TAG, "Online TTS exception: ${e.message}")
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
            Log.e(TAG, "Could not open TTS settings", e)
        }
    }

    fun release() {
        nativeTts?.shutdown()
        mediaPlayer?.release()
    }
}
