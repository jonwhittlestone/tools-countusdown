package uk.howapped.countusdown.ui

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.core.Phase

/** Everything the countdown screen draws, worked out from the clock. */
data class CountdownUiState(
    val phase: Phase,
    /** Whole minutes to the target, rounded up: 22m30s left shows as 23. */
    val minutesLeft: Int,
    /** 1.0 at the start, 0.0 at the target: how full the ring is. */
    val fractionLeft: Float,
    /** The target as a wall-clock time, e.g. "08:15". */
    val leaveAt: String,
)

private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun Countdown.uiStateAt(now: Instant, zone: ZoneId): CountdownUiState {
    val total = Duration.between(startedAt, target).seconds.toFloat()
    val left = Duration.between(now, target).seconds.coerceIn(0, total.toLong())
    return CountdownUiState(
        phase = phaseAt(now),
        minutesLeft = ((left + 59) / 60).toInt(),
        fractionLeft = if (total > 0) left / total else 0f,
        leaveAt = HH_MM.format(target.atZone(zone)),
    )
}
