package uk.howapped.countusdown.core

import java.time.Instant

/** How firm the wording is: gentle early on, firm in the last five minutes (design doc Q10). */
enum class Tier { GENTLE, NUDGE, FIRM }

/** What a voice cue says: "{X} minutes", "time to leave", or "we're now late". */
enum class CueKind { MINUTES, TARGET, LATE }

data class Cue(val at: Instant, val kind: CueKind, val minutesLeft: Int, val tier: Tier)

/** Every voice announcement for this countdown, in time order. */
fun Countdown.cues(): List<Cue> = TODO("commit #5")

/** How many ambient bed layers play at [now]: the soundscape thickens as the target nears. */
fun Countdown.bedLayersAt(now: Instant): Int = TODO("commit #5")
