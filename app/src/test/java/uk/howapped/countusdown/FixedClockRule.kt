package uk.howapped.countusdown

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.rules.ExternalResource

/**
 * Pins [AppClock] for one test. Give it a lower `order` than the Compose rule so the clock is
 * set before the activity launches.
 */
class FixedClockRule(at: String, zone: String = "Europe/London") : ExternalResource() {
    private val fixed = Clock.fixed(Instant.parse(at), ZoneId.of(zone))
    override fun before() { AppClock.clock = fixed }
    override fun after() { AppClock.clock = Clock.systemDefaultZone() }

    companion object {
        /** Monday 16 Nov 2026, 07:50 in London: five minutes into the school run. */
        const val MID_SCHOOL_RUN = "2026-11-16T07:50:00Z"
        /** Saturday 21 Nov 2026, 10:00: nothing scheduled today. */
        const val SATURDAY = "2026-11-21T10:00:00Z"
    }
}
