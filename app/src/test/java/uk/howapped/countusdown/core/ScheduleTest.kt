package uk.howapped.countusdown.core

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Test

class ScheduleTest {
    private val london = ZoneId.of("Europe/London")
    private val school = Schedule(Schedule.WEEKDAYS, LocalTime.of(7, 45))
    private val preset = Preset("school", "School run", 30, 5)

    /** "2026-11-16 07:00" in London. 16 Nov 2026 is a Monday. */
    private fun local(s: String): Instant = java.time.LocalDateTime.parse(s.replace(' ', 'T')).atZone(london).toInstant()

    @Test fun laterTheSameMorning() =
        assertThat(school.nextStart(local("2026-11-16 07:00"), london)).isEqualTo(local("2026-11-16 07:45"))

    @Test fun onceStartedTheNextIsTomorrow() =
        assertThat(school.nextStart(local("2026-11-16 07:45"), london)).isEqualTo(local("2026-11-17 07:45"))

    @Test fun fridayAfternoonSkipsTheWeekend() =
        assertThat(school.nextStart(local("2026-11-20 16:00"), london)).isEqualTo(local("2026-11-23 07:45"))

    @Test fun skippedDaysAreSkipped() =
        assertThat(school.nextStart(local("2026-11-16 07:00"), london, skip = setOf(LocalDate.of(2026, 11, 16))))
            .isEqualTo(local("2026-11-17 07:45"))

    @Test fun keepsLocalTimeAcrossTheClocksChanging() =
        // The clocks go forward on Sunday 28 March 2027: Monday's start is still 07:45 local (06:45 UTC).
        assertThat(school.nextStart(local("2027-03-26 09:00"), london)).isEqualTo(Instant.parse("2027-03-29T06:45:00Z"))

    @Test fun noDaysMeansNeverScheduled() =
        assertThat(Schedule(emptySet(), LocalTime.of(7, 45)).nextStart(local("2026-11-16 07:00"), london)).isNull()

    @Test fun aRunInProgressIsFoundAgain() {
        assertThat(school.activeStart(preset, local("2026-11-16 07:52"), london)).isEqualTo(local("2026-11-16 07:45"))
        // Still on the dim "Time to go" screen at 08:25 (grace ended 08:20, done at 08:30).
        assertThat(school.activeStart(preset, local("2026-11-16 08:25"), london)).isEqualTo(local("2026-11-16 07:45"))
    }

    @Test fun noRunInProgressOutsideTheWindowOrAtWeekends() {
        assertThat(school.activeStart(preset, local("2026-11-16 07:30"), london)).isNull()
        assertThat(school.activeStart(preset, local("2026-11-16 08:30"), london)).isNull()
        assertThat(school.activeStart(preset, local("2026-11-21 07:52"), london)).isNull() // Saturday
        assertThat(school.activeStart(preset, local("2026-11-16 07:52"), london, setOf(LocalDate.of(2026, 11, 16)))).isNull()
    }
}
