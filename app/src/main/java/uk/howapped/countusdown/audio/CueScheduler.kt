package uk.howapped.countusdown.audio

import java.time.Instant
import uk.howapped.countusdown.core.Countdown

/** What the scheduler drives: ExoPlayer on the TV, a fake in tests. */
interface ClipPlayer {
    fun playVoice(key: String)
    fun setBedLayers(count: Int)
}

class CueScheduler(
    private val countdown: Countdown,
    private val picker: VoicePicker,
    private val player: ClipPlayer,
) {
    fun tick(now: Instant): Unit = TODO("commit #15")
}
