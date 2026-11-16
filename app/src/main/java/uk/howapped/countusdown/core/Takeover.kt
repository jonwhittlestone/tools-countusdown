package uk.howapped.countusdown.core

/** One moment of the takeover: how opaque the night-blue layer and the message are. */
data class TakeoverFrame(val scrim: Float, val message: Float, val done: Boolean)

/**
 * The slow hand-over when the alarm takes the screen, so it never just cuts away from a programme.
 * Modelled on the screentime extension's fade-to-black (3 s EASE_IN_OUT_QUAD, message from 0.8 s):
 *
 *   0 s ─ night blue eases in over whatever is on screen ─ 3 s
 *         0.8 s ─ message eases in ─ 3 s ── holds to 7 s ── eases out ── 8 s → countdown fades in
 */
object Takeover {
    const val FADE_MS = 3000L
    const val MESSAGE_DELAY_MS = 800L
    const val MESSAGE_OUT_AT_MS = 7000L
    const val MESSAGE_OUT_MS = 1000L
    const val TOTAL_MS = MESSAGE_OUT_AT_MS + MESSAGE_OUT_MS

    fun frameAt(elapsedMs: Long): TakeoverFrame {
        val scrim = easeInOutQuad(progress(elapsedMs, 0, FADE_MS))
        val messageIn = easeInOutQuad(progress(elapsedMs, MESSAGE_DELAY_MS, FADE_MS - MESSAGE_DELAY_MS))
        val messageOut = easeInOutQuad(progress(elapsedMs, MESSAGE_OUT_AT_MS, MESSAGE_OUT_MS))
        return TakeoverFrame(scrim, messageIn * (1f - messageOut), elapsedMs >= TOTAL_MS)
    }

    private fun progress(t: Long, start: Long, length: Long): Float =
        ((t - start).toFloat() / length).coerceIn(0f, 1f)

    /** Clutter's EASE_IN_OUT_QUAD: slow, faster, slow. */
    private fun easeInOutQuad(x: Float): Float =
        if (x < 0.5f) 2 * x * x else 1 - (-2 * x + 2) * (-2 * x + 2) / 2
}
