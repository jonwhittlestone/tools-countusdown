package uk.howapped.countusdown

import android.app.Activity
import android.content.Context
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
import androidx.compose.runtime.withFrameMillis
import uk.howapped.countusdown.audio.CountdownService
import uk.howapped.countusdown.audio.CountdownService.Companion.putCountdown
import uk.howapped.countusdown.core.Takeover
import uk.howapped.countusdown.ui.TakeoverScreen
import uk.howapped.countusdown.ui.uiStateAt

/**
 * The first 8 seconds of a scheduled run. Its window is see-through and sits in its own task, so
 * it opens over whatever is on the TV (Plex, the home screen) and slowly covers it in night blue
 * with a message, instead of cutting straight to the countdown. Then it hands over to
 * MainActivity, which fades the countdown in from the same night blue.
 */
class TakeoverActivity : ComponentActivity() {
    private var handedOff = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val run = CountdownService.countdownFrom(intent)
        if (run == null) {
            finish()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Same as MainActivity: wake the TV and end the screensaver (design doc 03.06).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        // Like the countdown itself, Back does nothing here.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        val leaveAt = run.uiStateAt(run.startedAt, AppClock.clock.zone).leaveAt
        setContent {
            var frame by remember { mutableStateOf(Takeover.frameAt(0)) }
            LaunchedEffect(Unit) {
                val start = withFrameMillis { it }
                while (!frame.done) frame = Takeover.frameAt(withFrameMillis { it } - start)
                handOff()
            }
            TakeoverScreen(frame, run.preset.name, leaveAt)
        }
    }

    /** The remote does nothing during the 8 seconds; the parent code works once the countdown is up. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean = true

    /** Left with Home during the takeover: the countdown still comes back, as it would mid-run. */
    override fun onStop() {
        super.onStop()
        if (!handedOff && !isFinishing) startService(CountdownService.bounceIntent(this))
    }

    private fun handOff() {
        handedOff = true
        startActivity(handoff(this, intent))
        // No slide or zoom: night blue straight into night blue.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        finish()
    }

    companion object {
        /** The countdown screen for the same run, asked to fade in. */
        fun handoff(context: Context, intent: Intent): Intent =
            Intent(context, MainActivity::class.java)
                .putCountdown(CountdownService.countdownFrom(intent)!!)
                .putExtra(MainActivity.EXTRA_FADE_IN, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}
