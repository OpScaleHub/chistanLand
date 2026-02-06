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
 * Optimized for Persian learning by providing Urdu fallback for phonics.
 */
class TtsManager(private val context: Context) {
    private var nativeTts: TextToSpeech? = null
    private val _isPersianAvailable = MutableStateFlow(false)
    val isPersianAvailable: StateFlow<Boolean> = _isPersianAvailable.asStateFlow()
    
    private var _isUrduAvailable = false
    private var isEnglishNativeReady = false
    private val TAG = "TtsManager"
    private val PERSIAN_LOCALE = Locale("fa", "IR")
    private val URDU_LOCALE = Locale("ur", "PK")

    init {
        initNativeTts()
    }

    private fun initNativeTts() {
        nativeTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val faResult = nativeTts?.isLanguageAvailable(PERSIAN_LOCALE)
                _isPersianAvailable.value = faResult != null && faResult >= TextToSpeech.LANG_AVAILABLE
                
                val urResult = nativeTts?.isLanguageAvailable(URDU_LOCALE)
                _isUrduAvailable = urResult != null && urResult >= TextToSpeech.LANG_AVAILABLE

                val enResult = nativeTts?.isLanguageAvailable(Locale.US)
                isEnglishNativeReady = enResult != null && enResult >= TextToSpeech.LANG_AVAILABLE
                
                Log.i(TAG, "Native TTS Ready -> Persian: ${_isPersianAvailable.value}, Urdu: $_isUrduAvailable")
                
                setupProgressListener()
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
        Log.d(TAG, "Speaking: '$text' (${locale.language})")
        
        if (locale.language == "fa") {
            when {
                _isPersianAvailable.value -> speakNative(text, PERSIAN_LOCALE)
                _isUrduAvailable -> {
                    Log.i(TAG, "Persian TTS missing, falling back to Urdu for compatible phonics.")
                    speakNative(text, URDU_LOCALE)
                }
                else -> Log.w(TAG, "Neither Persian nor Urdu TTS available.")
            }
        } else {
            speakNative(text, locale)
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
