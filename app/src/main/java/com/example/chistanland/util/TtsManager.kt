package com.github.opscalehub.chistanland.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
// import com.k2fsa.sherpa.onnx.OfflineTts
// import com.k2fsa.sherpa.onnx.OfflineTtsConfig
// import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder
import java.util.*
import kotlin.coroutines.resume

class TtsManager(private val context: Context) {
    private var nativeTts: TextToSpeech? = null
    // private var offlineTts: OfflineTts? = null
    private var isPersianNativeReady = false
    private var mediaPlayer: MediaPlayer? = null

    init {
        // initSherpaOnnx() // Disabled until dependency is resolved
        initNativeTts()
    }

    private fun initSherpaOnnx() {
        /*
        try {
            val assetManager = context.assets
            val modelDir = "vits-persian"
            
            val files = assetManager.list(modelDir)
            if (files.isNullOrEmpty()) {
                Log.w("TtsManager", "Sherpa-ONNX models not found in assets/$modelDir")
                return
            }

            val config = OfflineTtsConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "$modelDir/model.onnx",
                    lexicon = "$modelDir/lexicon.txt",
                    tokens = "$modelDir/tokens.txt",
                    dataDir = modelDir
                ),
                numThreads = 1,
                debug = true
            )
            offlineTts = OfflineTts(assetManager, config)
            Log.i("TtsManager", "Sherpa-ONNX Offline TTS initialized successfully")
        } catch (e: Exception) {
            Log.e("TtsManager", "Failed to init Sherpa-ONNX", e)
        }
        */
    }

    private fun initNativeTts() {
        nativeTts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = nativeTts?.setLanguage(Locale("fa"))
                isPersianNativeReady = (result != null && result >= TextToSpeech.LANG_AVAILABLE)
            }
        }, "com.google.android.tts")
    }

    suspend fun speak(text: String) {
        /*
        // Priority 1: Offline Sherpa-ONNX (Local/Embedded)
        if (offlineTts != null) {
            speakOffline(text)
            return
        }
        */

        // Priority 2: Native Android TTS (Offline if downloaded)
        if (isPersianNativeReady) {
            speakNative(text)
            return
        }

        // Priority 3: Online Fallback (Last resort)
        Log.w("TtsManager", "Using Online Fallback...")
        speakOnline(text)
    }

    private suspend fun speakOffline(text: String) = withContext(Dispatchers.Default) {
        /*
        try {
            val audio = offlineTts?.generate(text, sid = 0, speed = 1.0f) ?: return@withContext
            
            val sampleRate = audio.sampleRate
            val samples = audio.samples
            
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            
            suspendCancellableCoroutine<Unit> { continuation ->
                audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(track: AudioTrack?) {
                        track?.release()
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    override fun onPeriodicNotification(track: AudioTrack?) {}
                })
                audioTrack.notificationMarkerPosition = samples.size
                audioTrack.play()
                
                continuation.invokeOnCancellation {
                    audioTrack.stop()
                    audioTrack.release()
                }
            }
        } catch (e: Exception) {
            Log.e("TtsManager", "Offline speak failed", e)
        }
        */
    }

    private suspend fun speakNative(text: String) = suspendCancellableCoroutine<Unit> { continuation ->
        val id = UUID.randomUUID().toString()
        nativeTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (continuation.isActive) continuation.resume(Unit)
    }

    private suspend fun speakOnline(text: String) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                mediaPlayer?.release()
                val url = "https://translate.google.com/translate_tts?ie=UTF-8&q=${URLEncoder.encode(text, "UTF-8")}&tl=fa&client=tw-ob"
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(url)
                    setOnPreparedListener { it.start() }
                    setOnCompletionListener { 
                        it.release()
                        if (continuation.isActive) continuation.resume(Unit) 
                    }
                    setOnErrorListener { _, _, _ -> 
                        if (continuation.isActive) continuation.resume(Unit)
                        true 
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    fun release() {
        nativeTts?.shutdown()
        // offlineTts?.release()
        mediaPlayer?.release()
    }
}
