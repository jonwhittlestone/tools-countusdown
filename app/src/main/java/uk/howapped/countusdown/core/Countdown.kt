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
)

/** One run of a preset, started at [startedAt]. */
data class Countdown(val preset: Preset, val startedAt: Instant) {
    fun phaseAt(now: Instant): Phase = TODO("commit #3")
}
