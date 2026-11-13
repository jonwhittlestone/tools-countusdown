package uk.howapped.countusdown

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.delay
import uk.howapped.countusdown.audio.CountdownService
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.core.Phase
import uk.howapped.countusdown.lock.CodeMatcher
import uk.howapped.countusdown.lock.LockStore
import uk.howapped.countusdown.lock.Pad
import uk.howapped.countusdown.schedule.Runs
import uk.howapped.countusdown.ui.MenuItem
import uk.howapped.countusdown.ui.ParentMenu
import uk.howapped.countusdown.ui.CountdownScreen
import uk.howapped.countusdown.ui.uiStateAt

/**
 * The full-screen countdown. Which run it shows, in order of preference:
 *  1. the one in the launching intent (the 07:45 alarm, or a preset started by hand),
 *  2. the scheduled run already in progress (opened from the launcher at 07:52),
 *  3. otherwise a School run starting now (`--ei duration_minutes N --ei grace_minutes G` to test).
 */
class MainActivity : ComponentActivity() {
    private val clock: Clock = Clock.systemDefaultZone()
    private var countdown by mutableStateOf<Countdown?>(null)

    private val matcher by lazy { CodeMatcher(LockStore.code(this)) }

    /** Set when a parent exits with the code: the countdown must not bounce back. */
    private var exited = false

    /** True while the parent menu (opened by the D-pad code) is showing. */
    var parentMenuOpen by mutableStateOf(false)
        private set

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

        // Back does nothing during a countdown (it only closes the parent menu). Home can't be
        // blocked by an app; that's handled by bouncing back (commit #20).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                parentMenuOpen = false
            }
        })

        // Opening the app is a good moment to make sure tomorrow's alarm is armed.
        Runs.arm(this, clock)
        start(resolve(intent))

        setContent {
            val current = countdown ?: return@setContent
            var now by remember { mutableStateOf(Instant.now(clock)) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1000)
                    now = Instant.now(clock)
                }
            }
            val state = current.uiStateAt(now, clock.zone)
            // Once the run is over, hand the TV back.
            LaunchedEffect(state.phase) { if (state.phase == Phase.DONE) finish() }
            CountdownScreen(state)
            if (parentMenuOpen) ParentMenu(parentMenuItems(), onDismiss = { parentMenuOpen = false })
        }
    }

    /** Left mid-run without the code (Home on the remote): ask the service to bring us back. */
    override fun onStop() {
        super.onStop()
        val running = countdown ?: return
        val stillRunning = running.phaseAt(Instant.now(clock)) != Phase.DONE
        if (!exited && !isFinishing && !isChangingConfigurations && stillRunning) {
            startService(CountdownService.bounceIntent(this))
        }
    }

    /** The 07:45 alarm can arrive while a manual run is showing: the new run replaces it. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        CountdownService.countdownFrom(intent)?.let(::start)
    }

    /**
     * While the parent menu is closed, every D-pad press goes to the code matcher and nothing
     * else: no focus moves, no hint that anything is listening. OK/Enter are swallowed too.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (parentMenuOpen) return super.dispatchKeyEvent(event)
        val pad = Pad.from(event.keyCode)
        if (pad != null) {
            if (event.action == KeyEvent.ACTION_UP && matcher.feed(pad)) parentMenuOpen = true
            return true
        }
        if (event.keyCode in SWALLOWED) return true
        return super.dispatchKeyEvent(event)
    }

    private fun parentMenuItems(): List<MenuItem> = buildList {
        add(MenuItem("Exit countdown") { exit(skipToday = false) })
        if (Runs.active(this@MainActivity, clock) != null || isScheduledDay()) {
            add(MenuItem("Exit and skip today") { exit(skipToday = true) })
        }
        add(MenuItem("Cancel") { parentMenuOpen = false })
    }

    private fun isScheduledDay(): Boolean =
        LocalDate.now(clock).dayOfWeek in Runs.SCHOOL_SCHEDULE.days

    /** The only way out: stops the audio, and stops the countdown coming back. */
    private fun exit(skipToday: Boolean) {
        exited = true
        if (skipToday) Runs.skip(this, LocalDate.now(clock))
        startService(CountdownService.stopIntent(this))
        finish()
    }

    private fun resolve(intent: Intent): Countdown =
        CountdownService.countdownFrom(intent)
            ?: Runs.active(this, clock)
            ?: Countdown(
                Runs.SCHOOL_RUN.copy(
                    durationMinutes = intent.getIntExtra(EXTRA_DURATION, Runs.SCHOOL_RUN.durationMinutes),
                    graceMinutes = intent.getIntExtra(EXTRA_GRACE, Runs.SCHOOL_RUN.graceMinutes),
                ),
                Instant.now(clock),
            )

    private fun start(next: Countdown) {
        countdown = next
        // The service owns the audio; the activity only draws. Both work from the same startedAt.
        // Starting the service with the same run twice is harmless: it ignores duplicates.
        startForegroundService(CountdownService.intentFor(this, next))
    }

    companion object {
        private val SWALLOWED = setOf(
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SEARCH,
        )
        const val EXTRA_DURATION = "duration_minutes"
        const val EXTRA_GRACE = "grace_minutes"
    }
}
