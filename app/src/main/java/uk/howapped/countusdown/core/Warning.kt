package uk.howapped.countusdown.core

import java.time.Duration
import java.time.Instant

/** The box's colour, as in the screentime extension: calm, then yellow, then amber. */
enum class WarningColour { GREY, YELLOW, AMBER }

/** One moment of the warning box: what is left, how it is coloured and whether it pulses. */
data class Shown(val left: Duration, val colour: WarningColour, val pulses: Boolean) {
    val label: String get() = "%02d:%02d".format(left.seconds / 60, left.seconds % 60)
}

/**
 * The small "until the countdown" box shown [lead] before a scheduled start.
 * A pure function of now, like the takeover, so a 10-minute warning is tested in milliseconds:
 *
 *   start − lead ─ grey (pulses 5:00–3:01) ─ 60 s yellow ─ 30 s amber ─ start → gone
 *                                             └──── pulses in the last minute ────┘
 */
data class Warning(val lead: Duration) {
    fun at(now: Instant, start: Instant): Shown? {
        val left = Duration.between(now, start)
        if (left <= Duration.ZERO || left > lead) return null
        val s = left.seconds
        val colour = when {
            s <= 30 -> WarningColour.AMBER
            s <= 60 -> WarningColour.YELLOW
            else -> WarningColour.GREY
        }
        return Shown(left, colour, pulses = s in 181..300 || s <= 60)
    }
}
