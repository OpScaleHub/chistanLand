package com.github.opscalehub.chistanland.util

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

/**
 * Manages Text-to-Speech functionality.
 */
class TtsManager(private val context: Context) {
    private var nativeTts: TextToSpeech? = null
    private val _isPersianAvailable = MutableStateFlow(false)
    val isPersianAvailable: StateFlow<Boolean> = _isPersianAvailable.asStateFlow()
    
    private var isEnglishNativeReady = false
    private val TAG = "TtsManager"
    private val PERSIAN_LOCALE = Locale("fa", "IR")

    init {
        initNativeTts()
    }

    private fun initNativeTts() {
        nativeTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val faResult = nativeTts?.isLanguageAvailable(PERSIAN_LOCALE)
                _isPersianAvailable.value = faResult != null && faResult >= TextToSpeech.LANG_AVAILABLE
                
                val enResult = nativeTts?.isLanguageAvailable(Locale.US)
                isEnglishNativeReady = enResult != null && enResult >= TextToSpeech.LANG_AVAILABLE
                
                Log.i(TAG, "Native TTS Ready -> Persian: ${_isPersianAvailable.value}, English: $isEnglishNativeReady")
                
                setupProgressListener()

                if (isEnglishNativeReady) {
                    @Suppress("OPT_IN_USAGE")
                    GlobalScope.launch {
                        delay(2000)
                        if (!_isPersianAvailable.value) {
                            speak("TTS system is active but Persian voice is missing", Locale.US)
                        } else {
                            speak("TTS system is active", Locale.US)
                        }
                    }
                }
            } else {
                Log.e(TAG, "Native TTS initialization failed")
            }
        }
    }

    private var currentContinuation: kotlinx.coroutines.CancellableContinuation<Unit>? = null

    private fun setupProgressListener() {
        nativeTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Native Speech Started: $utteranceId")
            }
            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Native Speech Done: $utteranceId")
                resumeContinuation()
            }
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Native Speech Error: $utteranceId")
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

    suspend fun speak(text: String, locale: Locale = PERSIAN_LOCALE) {
        val isPersian = locale.language == "fa"
        Log.d(TAG, "Speaking: '$text' (${locale.language})")
        
        if (isPersian) {
            if (_isPersianAvailable.value) {
                speakNative(text, locale)
            } else {
                Log.w(TAG, "Persian Local TTS Missing. No voice output.")
            }
        } else if (locale.language == "en") {
            if (isEnglishNativeReady) {
                speakNative(text, locale)
            } else {
                Log.w(TAG, "English Local TTS Missing.")
            }
        }
    }

    private suspend fun speakNative(text: String, locale: Locale) = suspendCancellableCoroutine<Unit> { continuation ->
        currentContinuation = continuation
        nativeTts?.language = locale
        val id = UUID.randomUUID().toString()
        val result = nativeTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        
        if (result != TextToSpeech.SUCCESS) {
            Log.e(TAG, "Native speak failed to queue")
            resumeContinuation()
        }
        
        continuation.invokeOnCancellation {
            nativeTts?.stop()
            currentContinuation = null
        }
    }

    fun openTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open TTS settings", e)
            // Fallback to general settings
            try {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e2: Exception) {}
        }
    }

    fun release() {
        nativeTts?.shutdown()
    }
}
