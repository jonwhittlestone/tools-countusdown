package uk.howapped.countusdown.audio

import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import org.junit.Test
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.core.Preset

class CueSchedulerTest {
    private class FakePlayer : ClipPlayer {
        val played = mutableListOf<String>()
        val layers = mutableListOf<Int>()
        override fun playVoice(key: String) { played += key }
        override fun setBedLayers(count: Int) { layers += count }
    }

    private val start = Instant.parse("2026-11-16T07:45:00Z")
    private val countdown = Countdown(Preset("school", "School run", 30, 5), start)
    private val manifest = AudioManifest(
        voices = listOf("a", "b"),
        variants = mapOf("gentle" to 1, "nudge" to 1, "firm" to 1, "target" to 1, "late" to 1),
        clips = emptySet(),
    )

    /** Ticks once a second from [from] to [to], like the service does. */
    private fun run(from: Instant, to: Instant): FakePlayer {
        val player = FakePlayer()
        val scheduler = CueScheduler(countdown, VoicePicker(manifest, seed = 0), player)
        var now = from
        while (now <= to) { scheduler.tick(now); now = now.plusSeconds(1) }
        return player
    }

    @Test fun playsEveryCueOnceInOrderOverTheWholeRun() {
        val player = run(start, start.plus(Duration.ofMinutes(50)))
        assertThat(player.played.map { it.substringBeforeLast('_') }).containsExactly(
            "minutes_gentle_0_30", "minutes_gentle_0_25", "minutes_gentle_0_20", "minutes_gentle_0_15",
            "minutes_nudge_0_10", "minutes_firm_0_05", "minutes_firm_0_04", "minutes_firm_0_03",
            "minutes_firm_0_02", "minutes_firm_0_01", "target_0", "late_0",
        ).inOrder()
    }

    @Test fun startingMidwayDoesNotReplayMissedCues() {
        // Opened at 07:52:10: the 30 and 25 cues are long gone; the next one due is 20 at 07:55.
        val player = run(Instant.parse("2026-11-16T07:52:10Z"), Instant.parse("2026-11-16T07:55:05Z"))
        assertThat(player.played.map { it.substringBeforeLast('_') }).containsExactly("minutes_gentle_0_20")
    }

    @Test fun aCueJustMissedStillPlays() {
        // A few seconds late (e.g. the service took a moment to start) is still worth saying.
        val player = run(Instant.parse("2026-11-16T07:45:04Z"), Instant.parse("2026-11-16T07:45:06Z"))
        assertThat(player.played).hasSize(1)
    }

    @Test fun bedLayersAreSetOnlyWhenTheyChange() {
        val player = run(start, start.plus(Duration.ofMinutes(50)))
        assertThat(player.layers).containsExactly(1, 2, 3, 0).inOrder()
    }
}
