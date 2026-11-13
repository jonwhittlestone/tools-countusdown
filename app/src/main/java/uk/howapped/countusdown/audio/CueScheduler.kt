package uk.howapped.countusdown.audio

import java.time.Duration
import java.time.Instant
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.core.bedLayersAt
import uk.howapped.countusdown.core.cues

/** What the scheduler drives: ExoPlayer on the TV, a fake in tests. */
interface ClipPlayer {
    fun playVoice(key: String)
    fun setBedLayers(count: Int)
}

/**
 * Called once a second with the time. Plays each cue when it falls due and keeps the
 * ambient bed at the right thickness. Pure logic with no Android in sight, so a test
 * can run a whole 30-minute school run in milliseconds with a fake clock.
 */
class CueScheduler(
    private val countdown: Countdown,
    private val picker: VoicePicker,
    private val player: ClipPlayer,
) {
    private val pending = countdown.cues().toMutableList()
    private var layers = -1

    fun tick(now: Instant) {
        // Cues that fell due since the last tick. Ones long past (the app opened mid-countdown)
        // are dropped silently rather than read out in a rush.
        while (pending.isNotEmpty() && !pending.first().at.isAfter(now)) {
            val cue = pending.removeAt(0)
            val ref = picker.pick(cue)
            if (Duration.between(cue.at, now) <= STALE_AFTER) player.playVoice(ref.key)
        }
        val wanted = countdown.bedLayersAt(now)
        if (wanted != layers) {
            layers = wanted
            player.setBedLayers(wanted)
        }
    }

    companion object {
        val STALE_AFTER: Duration = Duration.ofSeconds(30)
    }
}
