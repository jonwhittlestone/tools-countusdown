package uk.howapped.countusdown.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * The real [ClipPlayer]: one ExoPlayer for voice cues and one looping ExoPlayer per ambient
 * layer. Layers fade in and out over a few seconds so the soundscape thickens without a jolt,
 * and the bed ducks under each announcement so the voice is always the clearest thing.
 */
class ExoClipPlayer(context: Context) : ClipPlayer {
    private val handler = Handler(Looper.getMainLooper())
    private val speech = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
        .build()
    private val music = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private val voice = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(speech, /* handleAudioFocus = */ false)
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                duck = if (isPlaying) DUCKED else 1f
            }
        })
    }

    private val bed = BED_LAYERS.map { name ->
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(music, false)
            setMediaItem(MediaItem.fromUri("asset:///audio/bed/$name.ogg"))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            prepare()
        }
    }
    private val levels = FloatArray(bed.size)
    private val targets = FloatArray(bed.size)
    private var duck = 1f

    override fun playVoice(key: String) {
        voice.setMediaItem(MediaItem.fromUri("asset:///audio/voice/$key.ogg"))
        voice.prepare()
        voice.play()
    }

    override fun setBedLayers(count: Int) {
        for (i in targets.indices) targets[i] = if (i < count) BED_VOLUME else 0f
        handler.removeCallbacks(fade)
        handler.post(fade)
    }

    /** Steps every layer's volume towards its target, a little at a time. */
    private val fade = object : Runnable {
        override fun run() {
            var moving = false
            for (i in bed.indices) {
                val step = BED_VOLUME / FADE_STEPS
                levels[i] = when {
                    levels[i] < targets[i] -> (levels[i] + step).coerceAtMost(targets[i])
                    levels[i] > targets[i] -> (levels[i] - step).coerceAtLeast(targets[i])
                    else -> levels[i]
                }
                if (levels[i] != targets[i]) moving = true
                val player = bed[i]
                player.volume = levels[i] * duck
                if (levels[i] > 0f && !player.isPlaying) player.play()
                if (levels[i] == 0f && player.isPlaying) player.pause()
            }
            // Keep running while fading, and keep applying the duck while a voice plays.
            if (moving || voice.isPlaying || duck != 1f) handler.postDelayed(this, FADE_TICK_MS)
        }
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        voice.release()
        bed.forEach { it.release() }
    }

    companion object {
        val BED_LAYERS = listOf("layer1_rain", "layer2_pad", "layer3_pulse")
        const val BED_VOLUME = 0.6f
        const val DUCKED = 0.35f
        const val FADE_STEPS = 40 // x 100 ms = a 4 s fade
        const val FADE_TICK_MS = 100L
    }
}
