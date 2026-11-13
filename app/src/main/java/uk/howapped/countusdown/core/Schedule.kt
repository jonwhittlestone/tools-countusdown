package uk.howapped.countusdown.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** When a preset starts by itself, e.g. Monday to Friday at 07:45 (local time). */
data class Schedule(val days: Set<DayOfWeek>, val start: LocalTime) {
    companion object {
        val WEEKDAYS: Set<DayOfWeek> = DayOfWeek.entries.toSet() - DayOfWeek.SATURDAY - DayOfWeek.SUNDAY
    }
}

/** The next time [schedule] starts strictly after [now], skipping any [skip] dates (e.g. "skip today"). */
fun Schedule.nextStart(now: Instant, zone: ZoneId, skip: Set<LocalDate> = emptySet()): Instant? = TODO("commit #17")

/**
 * The start of the run that is going on at [now], if any: lets a reboot or an app restart
 * mid-countdown pick the same run back up instead of starting a new one.
 */
fun Schedule.activeStart(preset: Preset, now: Instant, zone: ZoneId, skip: Set<LocalDate> = emptySet()): Instant? =
    TODO("commit #17")
