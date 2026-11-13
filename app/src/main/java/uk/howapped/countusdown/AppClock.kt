package uk.howapped.countusdown

import java.time.Clock

/** The one clock the app reads. Tests swap in a fixed clock (e.g. a quiet Saturday). */
object AppClock {
    var clock: Clock = Clock.systemDefaultZone()
}
