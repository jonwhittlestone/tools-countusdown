package uk.howapped.countusdown.ui

import java.time.Instant
import java.time.ZoneId
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

fun Countdown.uiStateAt(now: Instant, zone: ZoneId): CountdownUiState = TODO("commit #7")
