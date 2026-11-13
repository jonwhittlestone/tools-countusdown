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
    companion object {
        /** The durations a preset may use: the voice library has a clip for each (pipeline/phrases.yaml). */
        val ALLOWED_DURATIONS = listOf(5, 10, 15, 20, 25, 30, 45, 60)
    }
}

/** One run of a preset, started at [startedAt]. */
data class Countdown(val preset: Preset, val startedAt: Instant) {
    /** When we should leave the house. */
    val target: Instant = startedAt.plus(Duration.ofMinutes(preset.durationMinutes.toLong()))

    /** After this we are definitely late. */
    val graceEnd: Instant = target.plus(Duration.ofMinutes(preset.graceMinutes.toLong()))

    /** The dimmed "Time to go" screen stays up this long before the run is over. */
    val doneAt: Instant = graceEnd.plus(LATE_HOLD)

    fun phaseAt(now: Instant): Phase = when {
        now < startedAt -> Phase.PENDING
        now < target -> Phase.CALM
        now < graceEnd -> Phase.GRACE
        now < doneAt -> Phase.LATE
        else -> Phase.DONE
    }

    companion object {
        val LATE_HOLD: Duration = Duration.ofMinutes(10)
    }
}
