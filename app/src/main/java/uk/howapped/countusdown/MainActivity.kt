package uk.howapped.countusdown

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.delay
import uk.howapped.countusdown.audio.CountdownService
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.core.Phase
import uk.howapped.countusdown.core.Preset
import uk.howapped.countusdown.ui.CountdownScreen
import uk.howapped.countusdown.ui.uiStateAt

/**
 * Starts a countdown when opened. Until presets and scheduling land (F5, F7), it runs
 * "School run" from now; `--ei duration_minutes N --ei grace_minutes G` overrides it for testing.
 */
class MainActivity : ComponentActivity() {
    private val clock: Clock = Clock.systemDefaultZone()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The countdown must stay visible for the whole run: no screensaver, no sleep.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Google TV is often showing its ambient screensaver (a "dream") when the countdown
        // starts; launching an activity alone leaves it behind the dream. Turning the screen
        // on wakes the device, which ends the dream (design doc 03.06).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        val preset = Preset(
            id = "school",
            name = "School run",
            durationMinutes = intent.getIntExtra(EXTRA_DURATION, 30),
            graceMinutes = intent.getIntExtra(EXTRA_GRACE, 5),
        )
        val countdown = Countdown(preset, Instant.now(clock))
        // The service owns the audio; the activity only draws. Both work from the same startedAt.
        startForegroundService(CountdownService.intentFor(this, countdown))

        setContent {
            var now by remember { mutableStateOf(Instant.now(clock)) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1000)
                    now = Instant.now(clock)
                }
            }
            val state = countdown.uiStateAt(now, clock.zone)
            // Once the run is over, hand the TV back.
            LaunchedEffect(state.phase) { if (state.phase == Phase.DONE) finish() }
            CountdownScreen(state)
        }
    }

    companion object {
        const val EXTRA_DURATION = "duration_minutes"
        const val EXTRA_GRACE = "grace_minutes"
    }
}
