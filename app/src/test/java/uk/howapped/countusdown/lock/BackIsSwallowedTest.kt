package uk.howapped.countusdown.lock

import android.view.KeyEvent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import org.robolectric.Shadows
import uk.howapped.countusdown.audio.CountdownService
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.howapped.countusdown.FixedClockRule
import uk.howapped.countusdown.MainActivity

// The Compose rule drives the frame clock, so the endless breathing animation can't stall the test.
@RunWith(AndroidJUnit4::class)
class BackIsSwallowedTest {
    @get:Rule(order = 0) val clock = FixedClockRule(FixedClockRule.MID_SCHOOL_RUN)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun backDoesNotLeaveTheCountdown() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        assertThat(compose.activity.isFinishing).isFalse()
    }

    @Test fun theCodeOpensTheParentMenu() {
        compose.runOnUiThread {
            for (key in listOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                               KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN)) {
                compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
                compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
            }
        }
        assertThat(compose.activity.parentMenuOpen).isTrue()
    }
}

@RunWith(AndroidJUnit4::class)
class BounceBackTest {
    @get:Rule(order = 0) val clock = FixedClockRule(FixedClockRule.MID_SCHOOL_RUN)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    private fun startedServiceActions(): List<String?> {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val shadow = Shadows.shadowOf(app)
        return generateSequence { shadow.nextStartedService }.map { it.action }.toList()
    }

    @Test fun leavingMidRunAsksToBounceBack() {
        startedServiceActions() // drain the start-up intents
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED) // onStop, e.g. Home
        assertThat(startedServiceActions()).contains(CountdownService.ACTION_BOUNCE)
    }
}
