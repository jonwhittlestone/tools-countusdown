package uk.howapped.countusdown.core

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test
import uk.howapped.countusdown.ui.uiStateAt

/** "Leave the house : 5s": a whole run in seconds, for smoke testing with the remote. */
class FiveSecondTestTest {
    private val start = Instant.parse("2026-11-16T09:00:00Z")
    private val run = Countdown(Preset("leave-5s", "Leave the house : 5s", Preset.FIVE_SECONDS, 0), start)

    @Test fun lastsFiveSecondsAndEndsHalfAMinuteLater() {
        assertThat(run.target).isEqualTo(start.plusSeconds(5))
        assertThat(run.phaseAt(start.plusSeconds(4))).isEqualTo(Phase.CALM)
        assertThat(run.phaseAt(start.plusSeconds(5))).isEqualTo(Phase.LATE)
        assertThat(run.phaseAt(start.plusSeconds(35))).isEqualTo(Phase.DONE)
    }

    @Test fun saysOnlyTimeToLeave() {
        // There are no recorded "N minutes" clips below one minute.
        assertThat(run.cues().map { it.kind }).containsExactly(CueKind.TARGET)
        assertThat(run.cues().single().at).isEqualTo(run.target)
    }

    @Test fun theScreenCountsSeconds() {
        val state = run.uiStateAt(start.plusMillis(1500), ZoneOffset.UTC)
        assertThat(state.secondsLeft).isEqualTo(4)
        val normal = Countdown(Preset("school", "School run", 30, 5), start).uiStateAt(start, ZoneOffset.UTC)
        assertThat(normal.secondsLeft).isNull()
    }
}
