package uk.howapped.countusdown.core

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class PhaseTest {
    private val schoolRun = Preset(id = "school", name = "School run", durationMinutes = 30, graceMinutes = 5)
    private val start = Instant.parse("2026-11-16T07:45:00Z")
    private val countdown = Countdown(schoolRun, start)

    private fun at(hhmmss: String) = Instant.parse("2026-11-16T${hhmmss}Z")

    @Test fun beforeTheStartIsPending() =
        assertThat(countdown.phaseAt(at("07:44:59"))).isEqualTo(Phase.PENDING)

    @Test fun fromTheStartUntilTheTargetIsCalm() {
        assertThat(countdown.phaseAt(at("07:45:00"))).isEqualTo(Phase.CALM)
        assertThat(countdown.phaseAt(at("08:14:59"))).isEqualTo(Phase.CALM)
    }

    @Test fun fromTheTargetUntilGraceEndsIsGrace() {
        assertThat(countdown.phaseAt(at("08:15:00"))).isEqualTo(Phase.GRACE)
        assertThat(countdown.phaseAt(at("08:19:59"))).isEqualTo(Phase.GRACE)
    }

    @Test fun afterGraceIsLateForTenMinutes() {
        assertThat(countdown.phaseAt(at("08:20:00"))).isEqualTo(Phase.LATE)
        assertThat(countdown.phaseAt(at("08:29:59"))).isEqualTo(Phase.LATE)
    }

    @Test fun thenTheCountdownIsDone() =
        assertThat(countdown.phaseAt(at("08:30:00"))).isEqualTo(Phase.DONE)

    @Test fun zeroGraceGoesStraightFromCalmToLate() {
        val football = Countdown(Preset("football", "Football", durationMinutes = 5, graceMinutes = 0), start)
        assertThat(football.phaseAt(at("07:49:59"))).isEqualTo(Phase.CALM)
        assertThat(football.phaseAt(at("07:50:00"))).isEqualTo(Phase.LATE)
    }
}
