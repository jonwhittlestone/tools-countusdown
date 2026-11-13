package uk.howapped.countusdown.core

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class CueScheduleTest {
    private val start = Instant.parse("2026-11-16T07:45:00Z")
    private fun at(hhmm: String) = Instant.parse("2026-11-16T$hhmm:00Z")
    private fun countdown(duration: Int, grace: Int) =
        Countdown(Preset("p", "P", durationMinutes = duration, graceMinutes = grace), start)

    @Test fun schoolRunCadenceTightensNearTheTarget() {
        val cues = countdown(30, 5).cues()
        assertThat(cues.filter { it.kind == CueKind.MINUTES }.map { it.minutesLeft })
            .containsExactly(30, 25, 20, 15, 10, 5, 4, 3, 2, 1).inOrder()
        assertThat(cues.first().at).isEqualTo(at("07:45"))
        assertThat(cues.first { it.minutesLeft == 1 }.at).isEqualTo(at("08:14"))
    }

    @Test fun targetAndLateCuesFollowTheMinutes() {
        val tail = countdown(30, 5).cues().takeLast(2)
        assertThat(tail.map { it.kind }).containsExactly(CueKind.TARGET, CueKind.LATE).inOrder()
        assertThat(tail.map { it.at }).containsExactly(at("08:15"), at("08:20")).inOrder()
    }

    @Test fun tierFollowsMinutesLeft() {
        val tiers = countdown(30, 5).cues().associate { (it.kind to it.minutesLeft) to it.tier }
        assertThat(tiers[CueKind.MINUTES to 15]).isEqualTo(Tier.GENTLE)
        assertThat(tiers[CueKind.MINUTES to 10]).isEqualTo(Tier.NUDGE)
        assertThat(tiers[CueKind.MINUTES to 5]).isEqualTo(Tier.FIRM)
        assertThat(tiers[CueKind.TARGET to 0]).isEqualTo(Tier.FIRM)
    }

    @Test fun shortPresetStartsAtItsOwnDuration() {
        assertThat(countdown(45, 5).cues().map { it.minutesLeft }.take(3)).containsExactly(45, 30, 25).inOrder()
        assertThat(countdown(5, 0).cues().first().minutesLeft).isEqualTo(5)
    }

    @Test fun zeroGraceHasNoSeparateLateCue() {
        assertThat(countdown(5, 0).cues().map { it.kind }.filter { it != CueKind.MINUTES })
            .containsExactly(CueKind.TARGET)
    }

    @Test fun bedThickensThenStops() {
        val c = countdown(30, 5)
        assertThat(c.bedLayersAt(at("07:44"))).isEqualTo(0) // pending
        assertThat(c.bedLayersAt(at("07:45"))).isEqualTo(1) // 30 min left
        assertThat(c.bedLayersAt(at("07:56"))).isEqualTo(2) // 19 min left
        assertThat(c.bedLayersAt(at("08:06"))).isEqualTo(3) // 9 min left: soft pulse joins
        assertThat(c.bedLayersAt(at("08:17"))).isEqualTo(3) // grace
        assertThat(c.bedLayersAt(at("08:20"))).isEqualTo(0) // late: silence
    }
}
