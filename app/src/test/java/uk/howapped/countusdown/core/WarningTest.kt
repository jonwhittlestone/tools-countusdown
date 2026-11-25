package uk.howapped.countusdown.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** The small box before a scheduled start: what it shows at each moment, as a pure function of now. */
class WarningTest {
    private val start = Instant.parse("2026-11-26T07:45:00Z")
    private val fiveMin = Warning(Duration.ofMinutes(5))
    private fun left(seconds: Long) = start.minusSeconds(seconds)

    @Test fun nothingBeforeTheWarningWindow() {
        assertThat(fiveMin.at(left(301), start)).isNull()
    }

    @Test fun itAppearsExactlyTheWarnTimeBeforeTheStart() {
        val shown = fiveMin.at(left(300), start)!!
        assertThat(shown.left).isEqualTo(Duration.ofMinutes(5))
    }

    @Test fun itCountsDownToTheStart() {
        assertThat(fiveMin.at(left(90), start)!!.left).isEqualTo(Duration.ofSeconds(90))
    }

    @Test fun itIsGoneAtTheStartSoTheTakeoverCanFadeIn() {
        assertThat(fiveMin.at(start, start)).isNull()
        assertThat(fiveMin.at(start.plusSeconds(1), start)).isNull()
    }

    @Test fun offNeverShows() {
        val off = Warning(Duration.ZERO)
        assertThat(off.at(left(1), start)).isNull()
        assertThat(off.at(left(120), start)).isNull()
    }

    @Test fun greyThenYellowInTheLastMinuteThenAmberInTheLast30Seconds() {
        assertThat(fiveMin.at(left(61), start)!!.colour).isEqualTo(WarningColour.GREY)
        assertThat(fiveMin.at(left(60), start)!!.colour).isEqualTo(WarningColour.YELLOW)
        assertThat(fiveMin.at(left(31), start)!!.colour).isEqualTo(WarningColour.YELLOW)
        assertThat(fiveMin.at(left(30), start)!!.colour).isEqualTo(WarningColour.AMBER)
        assertThat(fiveMin.at(left(1), start)!!.colour).isEqualTo(WarningColour.AMBER)
    }

    @Test fun itPulsesFromFiveToThreeMinutesAndAgainInTheLastMinute() {
        assertThat(fiveMin.at(left(300), start)!!.pulses).isTrue()
        assertThat(fiveMin.at(left(181), start)!!.pulses).isTrue()
        assertThat(fiveMin.at(left(180), start)!!.pulses).isFalse()
        assertThat(fiveMin.at(left(61), start)!!.pulses).isFalse()
        assertThat(fiveMin.at(left(60), start)!!.pulses).isTrue()
        assertThat(fiveMin.at(left(1), start)!!.pulses).isTrue()
    }

    @Test fun aTwoMinuteWarningStartsGreyAndStillPulsesInTheLastMinute() {
        val twoMin = Warning(Duration.ofMinutes(2))
        assertThat(twoMin.at(left(121), start)).isNull()
        val first = twoMin.at(left(120), start)!!
        assertThat(first.colour).isEqualTo(WarningColour.GREY)
        assertThat(first.pulses).isFalse()
        assertThat(twoMin.at(left(45), start)!!.pulses).isTrue()
    }

    @Test fun theBoxReadsMinutesAndSeconds() {
        assertThat(fiveMin.at(left(300), start)!!.label).isEqualTo("05:00")
        assertThat(fiveMin.at(left(65), start)!!.label).isEqualTo("01:05")
        assertThat(fiveMin.at(left(9), start)!!.label).isEqualTo("00:09")
    }
}
