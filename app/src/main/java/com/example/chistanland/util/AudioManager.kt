package com.example.chistanland.util

import android.content.Context
import android.media.MediaPlayer

class AudioManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playSound(resourceName: String) {
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        if (resId != 0) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, resId)
            mediaPlayer?.start()
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
