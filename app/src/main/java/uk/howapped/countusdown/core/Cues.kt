package uk.howapped.countusdown.core

import java.time.Duration
import java.time.Instant

/** How firm the wording is: gentle early on, firm in the last five minutes (design doc Q10). */
enum class Tier { GENTLE, NUDGE, FIRM }

/** What a voice cue says: "{X} minutes", "time to leave", or "we're now late". */
enum class CueKind { MINUTES, TARGET, LATE }

data class Cue(val at: Instant, val kind: CueKind, val minutesLeft: Int, val tier: Tier)

/** The minute marks we announce: every five minutes, then every minute for the last five. */
val STANDARD_MARKS = listOf(30, 25, 20, 15, 10, 5, 4, 3, 2, 1)

fun tierFor(minutesLeft: Int): Tier = when {
    minutesLeft > 10 -> Tier.GENTLE
    minutesLeft > 5 -> Tier.NUDGE
    else -> Tier.FIRM
}

/** Every voice announcement for this countdown, in time order. */
fun Countdown.cues(): List<Cue> {
    val duration = preset.durationMinutes
    // Always announce the full duration first, even when it isn't a standard mark (e.g. 45).
    val marks = (listOf(duration) + STANDARD_MARKS.filter { it < duration })
    val minuteCues = marks.map { m ->
        Cue(target.minus(Duration.ofMinutes(m.toLong())), CueKind.MINUTES, m, tierFor(m))
    }
    val targetCue = Cue(target, CueKind.TARGET, 0, Tier.FIRM)
    // With no grace, "time to leave" is the last word; a "we're late" cue at the same moment would clash.
    val lateCue = Cue(graceEnd, CueKind.LATE, 0, Tier.FIRM).takeIf { preset.graceMinutes > 0 }
    return minuteCues + listOfNotNull(targetCue, lateCue)
}

/** How many ambient bed layers play at [now]: the soundscape thickens as the target nears. */
fun Countdown.bedLayersAt(now: Instant): Int = when (phaseAt(now)) {
    Phase.CALM -> {
        val secondsLeft = Duration.between(now, target).seconds
        when {
            secondsLeft > 20 * 60 -> 1
            secondsLeft > 10 * 60 -> 2
            else -> 3
        }
    }
    Phase.GRACE -> 3
    Phase.PENDING, Phase.LATE, Phase.DONE -> 0
}
