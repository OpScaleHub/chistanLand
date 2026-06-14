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

    /** True when our dedicated offline Persian engine (AvaCore) is driving synthesis. */
    private val _isAvaCoreActive = MutableStateFlow(false)
    val isAvaCoreActive: StateFlow<Boolean> = _isAvaCoreActive.asStateFlow()

    private var _isUrduAvailable = false
    private val TAG = "TtsManager"
    private val PERSIAN_LOCALE = Locale("fa", "IR")
    private val URDU_LOCALE = Locale("ur", "PK")

    private val initDeferred = CompletableDeferred<Boolean>()
    private var isReleased = false

    /** Whether AvaCore (our offline Persian neural TTS) is installed on this device. */
    private fun isAvaCoreInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(AVACORE_PACKAGE, 0)
        true
    } catch (e: Exception) {
        false
    }

    private val ttsInitListener: TextToSpeech.OnInitListener = TextToSpeech.OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS) {
            val faResult = nativeTts?.isLanguageAvailable(PERSIAN_LOCALE)
            _isPersianAvailable.value = faResult != null && faResult >= TextToSpeech.LANG_AVAILABLE

            val urResult = nativeTts?.isLanguageAvailable(URDU_LOCALE)
            _isUrduAvailable = urResult != null && urResult >= TextToSpeech.LANG_AVAILABLE

            // Optimized for kids: Slightly higher pitch and slightly slower rate
            nativeTts?.setPitch(1.1f)
            nativeTts?.setSpeechRate(0.85f)

            Log.i(TAG, "TTS Ready -> engine=${nativeTts?.defaultEngine}, Persian: ${_isPersianAvailable.value}, Urdu: $_isUrduAvailable")

            setupProgressListener()
            initDeferred.complete(true)
        } else {
            Log.e(TAG, "TTS Init failed: $status")
            // If we asked for AvaCore explicitly and it failed, retry once with the system default.
            if (_isAvaCoreActive.value) {
                Log.w(TAG, "AvaCore engine init failed; falling back to system default engine.")
                _isAvaCoreActive.value = false
                nativeTts = TextToSpeech(context.applicationContext, ttsInitListener)
            } else {
                initDeferred.complete(false)
            }
        }
    }

    private fun initNativeTts() {
        // Prefer AvaCore explicitly so Persian works even when it isn't the system-default engine.
        val avaCorePresent = isAvaCoreInstalled()
        _isAvaCoreActive.value = avaCorePresent

        nativeTts = if (avaCorePresent) {
            Log.i(TAG, "Requesting AvaCore as Persian TTS engine ($AVACORE_PACKAGE)")
            TextToSpeech(context.applicationContext, ttsInitListener, AVACORE_PACKAGE)
        } else {
            TextToSpeech(context.applicationContext, ttsInitListener)
        }
    }

    // Declared last so every property above (esp. ttsInitListener) is initialized before we
    // construct the engine — Kotlin runs init blocks/initializers in declaration order.
    init {
        initNativeTts()
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

    companion object {
        /** Our dedicated offline Persian neural TTS engine (Piper VITS via Sherpa-ONNX). */
        const val AVACORE_PACKAGE = "com.github.opscalehub.avacore"
    }
}
