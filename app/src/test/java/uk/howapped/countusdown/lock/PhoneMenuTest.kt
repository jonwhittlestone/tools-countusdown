package uk.howapped.countusdown.lock

import android.view.KeyEvent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.howapped.countusdown.FixedClockRule
import uk.howapped.countusdown.MainActivity
import uk.howapped.countusdown.ui.PHONE_MENU_HINT

/** Robolectric's default device has no FEATURE_LEANBACK: a phone, where the code isn't asked for. */
@RunWith(AndroidJUnit4::class)
class PhoneMenuTest {
    @get:Rule(order = 0) val clock = FixedClockRule(FixedClockRule.MID_SCHOOL_RUN)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun onAPhoneTappingTheHintOpensTheFullMenuWithoutTheCode() {
        compose.onNodeWithText(PHONE_MENU_HINT).performClick()
        assertThat(compose.activity.unlockOpen).isFalse()
        assertThat(compose.activity.parentMenuOpen).isTrue()
        compose.onNodeWithText("Exit countdown").assertExists()
    }

    @Test fun onAPhoneHoldingOkOpensTheMenuToo() {
        compose.runOnUiThread {
            val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER)
            compose.activity.dispatchKeyEvent(down)
            compose.activity.dispatchKeyEvent(KeyEvent.changeTimeRepeat(down, down.eventTime + 600, 1))
        }
        assertThat(compose.activity.parentMenuOpen).isTrue()
    }
}
