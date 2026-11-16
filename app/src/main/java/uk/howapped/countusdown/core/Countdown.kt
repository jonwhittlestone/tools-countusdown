package uk.howapped.countusdown.core

import java.time.Duration
import java.time.Instant

/** Where a countdown is, relative to its target ("leave the house") time. */
enum class Phase { PENDING, CALM, GRACE, LATE, DONE }

/**
 * A reusable countdown recipe, e.g. "School run": 30 minutes to the target,
 * then 5 minutes of grace before we're definitely late.
 */
data class Preset(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val graceMinutes: Int,
) {
    /** The 5-second smoke-test run ("Leave the house : 5s"), stored as a length of 0 minutes. */
    val isTest: Boolean get() = durationMinutes == FIVE_SECONDS

    /** From the start to "time to leave". */
    val length: Duration get() =
        if (isTest) Duration.ofSeconds(5) else Duration.ofMinutes(durationMinutes.toLong())

    /** "5 s" or "30 min", for menus. */
    val lengthLabel: String get() = if (isTest) "5 s" else "$durationMinutes min"

    companion object {
        /** A length of 0 means the 5-second test: no minute announcements, just "time to leave". */
        const val FIVE_SECONDS = 0

        /** The durations a preset may use: the voice library has a clip for each (pipeline/phrases.yaml). */
        val ALLOWED_DURATIONS = listOf(FIVE_SECONDS, 5, 10, 15, 20, 25, 30, 45, 60)
    }
}

/** One run of a preset, started at [startedAt]. */
data class Countdown(val preset: Preset, val startedAt: Instant) {
    /** When we should leave the house. */
    val target: Instant = startedAt.plus(preset.length)

    /** After this we are definitely late. */
    val graceEnd: Instant = target.plus(Duration.ofMinutes(preset.graceMinutes.toLong()))

    /** The dimmed "Time to go" screen stays up this long before the run is over (briefly for a test). */
    val doneAt: Instant = graceEnd.plus(if (preset.isTest) TEST_HOLD else LATE_HOLD)

    fun phaseAt(now: Instant): Phase = when {
        now < startedAt -> Phase.PENDING
        now < target -> Phase.CALM
        now < graceEnd -> Phase.GRACE
        now < doneAt -> Phase.LATE
        else -> Phase.DONE
    }

    companion object {
        val LATE_HOLD: Duration = Duration.ofMinutes(10)
        val TEST_HOLD: Duration = Duration.ofSeconds(30)
    }
}
