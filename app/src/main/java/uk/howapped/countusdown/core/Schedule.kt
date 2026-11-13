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
fun Schedule.nextStart(now: Instant, zone: ZoneId, skip: Set<LocalDate> = emptySet()): Instant? {
    if (days.isEmpty()) return null
    val today = now.atZone(zone).toLocalDate()
    // Walk forward day by day; within two weeks there is always a scheduled, non-skipped day
    // unless the skip list covers them all.
    return (0L..14L).asSequence()
        .map { today.plusDays(it) }
        .filter { it.dayOfWeek in days && it !in skip }
        // atZone keeps 07:45 as 07:45 local, whatever the UTC offset that day.
        .map { it.atTime(start).atZone(zone).toInstant() }
        .firstOrNull { it.isAfter(now) }
}

/**
 * The start of the run that is going on at [now], if any: lets a reboot or an app restart
 * mid-countdown pick the same run back up instead of starting a new one.
 */
fun Schedule.activeStart(preset: Preset, now: Instant, zone: ZoneId, skip: Set<LocalDate> = emptySet()): Instant? {
    val today = now.atZone(zone).toLocalDate()
    if (today.dayOfWeek !in days || today in skip) return null
    val start = today.atTime(start).atZone(zone).toInstant()
    val phase = Countdown(preset, start).phaseAt(now)
    return start.takeIf { phase != Phase.PENDING && phase != Phase.DONE }
}
