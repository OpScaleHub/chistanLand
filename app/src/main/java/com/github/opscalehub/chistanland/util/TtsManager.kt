package com.github.opscalehub.chistanland.util

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

/**
 * Manages Text-to-Speech functionality with robust lifecycle handling.
 */
class TtsManager(private val context: Context) {
    private var nativeTts: TextToSpeech? = null
    private val _isPersianAvailable = MutableStateFlow(false)
    val isPersianAvailable: StateFlow<Boolean> = _isPersianAvailable.asStateFlow()
    
    private var _isUrduAvailable = false
    private val TAG = "TtsManager"
    private val PERSIAN_LOCALE = Locale("fa", "IR")
    private val URDU_LOCALE = Locale("ur", "PK")
    
    private val initDeferred = CompletableDeferred<Boolean>()
    private var isReleased = false

    init {
        initNativeTts()
    }

    private fun initNativeTts() {
        nativeTts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val faResult = nativeTts?.isLanguageAvailable(PERSIAN_LOCALE)
                _isPersianAvailable.value = faResult != null && faResult >= TextToSpeech.LANG_AVAILABLE
                
                val urResult = nativeTts?.isLanguageAvailable(URDU_LOCALE)
                _isUrduAvailable = urResult != null && urResult >= TextToSpeech.LANG_AVAILABLE

                // Optimized for kids: Slightly higher pitch and slightly slower rate
                nativeTts?.setPitch(1.1f) 
                nativeTts?.setSpeechRate(0.85f) 

                Log.i(TAG, "TTS Ready -> Persian: ${_isPersianAvailable.value}, Urdu: $_isUrduAvailable")
                
                setupProgressListener()
                initDeferred.complete(true)
            } else {
                Log.e(TAG, "TTS Init failed: $status")
                initDeferred.complete(false)
            }
        }
    }

    private var currentContinuation: kotlinx.coroutines.CancellableContinuation<Unit>? = null

    private fun setupProgressListener() {
        nativeTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { resumeContinuation() }
            override fun onError(utteranceId: String?) { resumeContinuation() }
        })
    }

    private fun resumeContinuation() {
        currentContinuation?.let {
            if (it.isActive) it.resume(Unit)
            currentContinuation = null
        }
    }

    suspend fun speak(text: String, locale: Locale = PERSIAN_LOCALE) {
        if (isReleased) return

        val isReady = try { initDeferred.await() } catch (e: Exception) { false }
        if (!isReady || nativeTts == null) return

        if (locale.language == "fa") {
            when {
                _isPersianAvailable.value -> speakNative(text, PERSIAN_LOCALE)
                _isUrduAvailable -> speakNative(text, URDU_LOCALE)
                else -> Log.w(TAG, "No TTS engine available for Persian/Urdu")
            }
        } else {
            speakNative(text, locale)
        }
    }

    private suspend fun speakNative(text: String, locale: Locale) = suspendCancellableCoroutine<Unit> { continuation ->
        if (isReleased) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }
        currentContinuation = continuation
        
        try {
            nativeTts?.language = locale
            val id = UUID.randomUUID().toString()
            nativeTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        } catch (e: Exception) {
            resumeContinuation()
        }
        
        continuation.invokeOnCancellation {
            stop()
        }
    }

    fun stop() {
        try {
            nativeTts?.stop()
            currentContinuation?.let {
                if (it.isActive) it.resume(Unit)
                currentContinuation = null
            }
        } catch (e: Exception) {}
    }

    fun openTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (e2: Exception) {}
        }
    }

    fun release() {
        isReleased = true
        stop()
        try {
            nativeTts?.shutdown()
            nativeTts = null
        } catch (e: Exception) {}
    }
}
