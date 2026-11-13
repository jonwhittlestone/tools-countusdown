package uk.howapped.countusdown.lock

import android.view.KeyEvent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.howapped.countusdown.MainActivity

// The Compose rule drives the frame clock, so the endless breathing animation can't stall the test.
@RunWith(AndroidJUnit4::class)
class BackIsSwallowedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

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
