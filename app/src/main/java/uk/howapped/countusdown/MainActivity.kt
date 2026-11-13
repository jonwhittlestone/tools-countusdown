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
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import uk.howapped.countusdown.audio.CountdownService
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.core.Phase
import uk.howapped.countusdown.lock.CodeMatcher
import uk.howapped.countusdown.lock.LockStore
import uk.howapped.countusdown.lock.Pad
import uk.howapped.countusdown.presets.PresetStore
import uk.howapped.countusdown.schedule.Runs
import uk.howapped.countusdown.ui.CodeEntry
import uk.howapped.countusdown.ui.CountdownScreen
import uk.howapped.countusdown.ui.IdleScreen
import uk.howapped.countusdown.ui.MenuItem
import uk.howapped.countusdown.ui.ParentMenu
import uk.howapped.countusdown.ui.PresetEditor
import uk.howapped.countusdown.ui.uiStateAt

/**
 * The one screen. Which run it shows, in order of preference:
 *  1. the one in the launching intent (a scheduled alarm, or a preset started from the menu),
 *  2. the scheduled run already in progress (opened from the launcher at 07:52),
 *  3. a test run from `--ei duration_minutes N [--ei grace_minutes G]`,
 *  4. otherwise nothing: a calm idle screen saying what's next.
 * During a run the remote is locked; the parent's D-pad code opens the parent menu.
 */
class MainActivity : ComponentActivity() {
    private val clock: Clock get() = AppClock.clock
    private var countdown by mutableStateOf<Countdown?>(null)

    private enum class Overlay { NONE, MENU, EDITOR, CODE }
    private var overlay by mutableStateOf(Overlay.NONE)

    /** True while the parent menu (opened by the D-pad code) is showing. */
    val parentMenuOpen: Boolean get() = overlay == Overlay.MENU

    private var matcher = CodeMatcher(LockStore.DEFAULT_CODE)

    /** Set when a parent exits with the code: the countdown must not bounce back. */
    private var exited = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        matcher = CodeMatcher(LockStore.code(this))
        // The countdown must stay visible for the whole run: no screensaver, no sleep.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Google TV is often showing its ambient screensaver (a "dream") when the countdown
        // starts; turning the screen on wakes the device, which ends the dream (design doc 03.06).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        // Back closes an overlay; during a run it does nothing else. Idle, it leaves as normal.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    overlay != Overlay.NONE -> overlay = Overlay.NONE
                    countdown == null -> finish()
                }
            }
        })

        // Opening the app is a good moment to make sure the next alarm is armed.
        Runs.arm(this, clock)
        resolve(intent)?.let(::start)

        setContent {
            val current = countdown
            if (current == null) {
                IdleScreen(nextLabel())
            } else {
                var now by remember { mutableStateOf(Instant.now(clock)) }
                LaunchedEffect(current) {
                    while (true) {
                        now = Instant.now(clock)
                        delay(1000)
                    }
                }
                val state = current.uiStateAt(now, clock.zone)
                // Once the run is over, hand the TV back.
                LaunchedEffect(state.phase) { if (state.phase == Phase.DONE) finish() }
                CountdownScreen(state)
            }
            when (overlay) {
                Overlay.MENU -> ParentMenu(parentMenuItems(), onDismiss = { overlay = Overlay.NONE })
                Overlay.EDITOR -> PresetEditor(
                    PresetStore(this).all(),
                    onSave = {
                        PresetStore(this).save(it)
                        Runs.arm(this, clock)
                        overlay = Overlay.NONE
                    },
                    onClose = { overlay = Overlay.NONE },
                )
                Overlay.CODE -> CodeEntry(
                    onDone = {
                        LockStore.setCode(this, it)
                        matcher = CodeMatcher(it)
                        overlay = Overlay.NONE
                    },
                    onCancel = { overlay = Overlay.NONE },
                )
                Overlay.NONE -> Unit
            }
        }
    }

    /** The 07:45 alarm can arrive while a manual run is showing: the new run replaces it. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        CountdownService.countdownFrom(intent)?.let(::start)
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

    /**
     * While no overlay is open, every D-pad press goes to the code matcher and nothing else:
     * no focus moves, no hint that anything is listening. OK/Enter/Menu are swallowed too.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (overlay != Overlay.NONE) return super.dispatchKeyEvent(event)
        val pad = Pad.from(event.keyCode)
        if (pad != null) {
            if (event.action == KeyEvent.ACTION_UP && matcher.feed(pad)) overlay = Overlay.MENU
            return true
        }
        if (event.keyCode in SWALLOWED) return true
        return super.dispatchKeyEvent(event)
    }

    private fun parentMenuItems(): List<MenuItem> = buildList {
        val running = countdown
        if (running != null) {
            add(MenuItem("Exit countdown") { exit(skipToday = false) })
            if (Runs.scheduledToday(this@MainActivity, clock)) {
                add(MenuItem("Exit and skip today") { exit(skipToday = true) })
            }
        } else if (Runs.scheduledToday(this@MainActivity, clock)) {
            add(MenuItem("Skip today's countdown") {
                Runs.skip(this@MainActivity, LocalDate.now(clock))
                overlay = Overlay.NONE
            })
        }
        PresetStore(this@MainActivity).all().forEach { p ->
            add(MenuItem("Start ${p.preset.name} (${p.preset.durationMinutes} min)") {
                overlay = Overlay.NONE
                start(Countdown(p.preset, Instant.now(clock)))
            })
        }
        add(MenuItem("Edit presets") { overlay = Overlay.EDITOR })
        add(MenuItem("Change unlock code") { overlay = Overlay.CODE })
        add(MenuItem("Close menu") { overlay = Overlay.NONE })
    }

    /** "Next: School run · Mon 07:45" for the idle screen. */
    private fun nextLabel(): String? = Runs.next(this, clock)?.let { (p, at) ->
        "Next: ${p.preset.name} · ${NEXT.format(at.atZone(clock.zone))}"
    }

    /** The only way out: stops the audio, and stops the countdown coming back. */
    private fun exit(skipToday: Boolean) {
        exited = true
        if (skipToday) Runs.skip(this, LocalDate.now(clock))
        startService(CountdownService.stopIntent(this))
        finish()
    }

    private fun resolve(intent: Intent): Countdown? =
        CountdownService.countdownFrom(intent)
            ?: Runs.active(this, clock)
            ?: intent.takeIf { it.hasExtra(EXTRA_DURATION) }?.let {
                val school = PresetStore.SEEDS.first().preset
                Countdown(
                    school.copy(
                        durationMinutes = it.getIntExtra(EXTRA_DURATION, school.durationMinutes),
                        graceMinutes = it.getIntExtra(EXTRA_GRACE, school.graceMinutes),
                    ),
                    Instant.now(clock),
                )
            }

    private fun start(next: Countdown) {
        exited = false
        countdown = next
        // The service owns the audio; the activity only draws. Both work from the same startedAt.
        startForegroundService(CountdownService.intentFor(this, next))
    }

    companion object {
        private val NEXT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE HH:mm")
        private val SWALLOWED = setOf(
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SEARCH,
        )
        const val EXTRA_DURATION = "duration_minutes"
        const val EXTRA_GRACE = "grace_minutes"
    }
}
