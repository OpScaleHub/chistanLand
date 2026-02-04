package com.github.opscalehub.chistanland.util

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*
import kotlin.coroutines.resume

class TtsManager(private val context: Context) {
    private var nativeTts: TextToSpeech? = null
    private var isPersianNativeReady = false
    private var isEnglishNativeReady = false
    private var mediaPlayer: MediaPlayer? = null
    private val TAG = "TtsManager"
    private val PERSIAN_LOCALE = Locale("fa", "IR")

    init {
        initNativeTts()
    }

    private fun initNativeTts() {
        nativeTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val faResult = nativeTts?.isLanguageAvailable(PERSIAN_LOCALE)
                isPersianNativeReady = faResult != null && faResult >= TextToSpeech.LANG_AVAILABLE
                
                val enResult = nativeTts?.isLanguageAvailable(Locale.US)
                isEnglishNativeReady = enResult != null && enResult >= TextToSpeech.LANG_AVAILABLE
                
                Log.i(TAG, "Native TTS Ready -> Persian: $isPersianNativeReady, English: $isEnglishNativeReady")
                
                setupProgressListener()

                if (isEnglishNativeReady) {
                    GlobalScope.launch {
                        delay(2000)
                        speak("TTS system is active", Locale.US)
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
            if (isPersianNativeReady) {
                speakNative(text, locale)
            } else {
                Log.w(TAG, "Persian Local TTS Missing. Using Online Fallback...")
                speakOnline(text, "fa")
            }
        } else if (locale.language == "en") {
            if (isEnglishNativeReady) {
                speakNative(text, locale)
            } else {
                speakOnline(text, "en")
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

    private suspend fun speakOnline(text: String, langCode: String) = withContext(Dispatchers.IO) {
        try {
            val encodedText = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
            val urlString = "https://translate.google.com/translate_tts?ie=UTF-8&tl=$langCode&client=tw-ob&q=$encodedText"
            
            val tempFile = File(context.cacheDir, "tts_cache.mp3")
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            
            connection.connect()
            if (connection.responseCode == 200) {
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    playFile(tempFile)
                }
            } else {
                Log.e(TAG, "Google TTS Server Error: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Online Fallback failed: ${e.message}")
        }
    }

    private suspend fun playFile(file: File) = suspendCancellableCoroutine<Unit> { continuation ->
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { 
                    it.release()
                    mediaPlayer = null
                    if (continuation.isActive) continuation.resume(Unit)
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    mediaPlayer = null
                    if (continuation.isActive) continuation.resume(Unit)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(Unit)
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
