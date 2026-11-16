package uk.howapped.countusdown

import android.content.Intent
import android.view.KeyEvent
import androidx.compose.ui.test.performClick
import uk.howapped.countusdown.ui.IDLE_MENU_LABEL
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.robolectric.shadows.ShadowSettings
import org.junit.Test
import org.junit.runner.RunWith

/** Opened from the launcher at 07:50 on a school day: straight into the running countdown. */
@RunWith(AndroidJUnit4::class)
class ScaffoldTest {
    @get:Rule(order = 0) val clock = FixedClockRule(FixedClockRule.MID_SCHOOL_RUN)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensIntoTheScheduledRunInProgress() {
        compose.onNodeWithText("25").assertExists()
        compose.onNodeWithText("leave at 08:15").assertExists()
    }

    @Test
    fun isListedOnTheTvHomeScreenWithABanner() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val tvLauncher = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .setPackage(context.packageName)
        val activities = context.packageManager.queryIntentActivities(tvLauncher, 0)
        assertThat(activities.map { it.activityInfo.name }).containsExactly(MainActivity::class.java.name)
        assertThat(context.applicationInfo.banner).isNotEqualTo(0)
    }
}

/** Opened on a Saturday: nothing starts, the screen just says what's next. */
@RunWith(AndroidJUnit4::class)
class IdleTest {
    @get:Rule(order = 0) val clock = FixedClockRule(FixedClockRule.SATURDAY)
    // Already set up, so the one-time wizard stays out of the way.
    @get:Rule(order = 1) val allowed = object : ExternalResource() {
        override fun before() = ShadowSettings.setCanDrawOverlays(true)
        override fun after() = ShadowSettings.setCanDrawOverlays(false)
    }
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsTheNextScheduledRunAndNoCountdown() {
        compose.onNodeWithText("Next: School run · Mon 07:45").assertExists()
        compose.onNodeWithText("leave at", substring = true).assertDoesNotExist()
    }

    @Test
    fun backLeavesTheIdleScreen() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        assertThat(compose.activity.isFinishing).isTrue()
    }

    @Test
    fun theMenuButtonOpensTheMenuWithoutTheCode() {
        compose.onNodeWithText(IDLE_MENU_LABEL).performClick()
        compose.onNodeWithText("Change how it starts").assertExists()
        // Anything that could undo the lock or the school run still needs the code.
        compose.onNodeWithText("Change unlock code").assertDoesNotExist()
        compose.onNodeWithText("Skip today's countdown").assertDoesNotExist()
    }

    @Test
    fun okOnTheRemoteOpensTheMenuToo() {
        compose.runOnUiThread {
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        }
        assertThat(compose.activity.parentMenuOpen).isTrue()
    }

    @Test
    fun theCodeOnTheIdleScreenStillGivesTheFullMenu() {
        compose.runOnUiThread {
            for (key in listOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                               KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN)) {
                compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
                compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
            }
        }
        compose.onNodeWithText("Change unlock code").assertExists()
    }
}

@RunWith(AndroidJUnit4::class)
class SetupWizardTest {
    @get:Rule(order = 0) val clock = FixedClockRule(FixedClockRule.SATURDAY)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensByAskingHowItShouldStart() {
        compose.onNodeWithText("How should COUNT-US-DOWN start?").assertExists()
    }

    @Test
    fun backMeansNotNowAndShowsTheIdleScreen() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("How should COUNT-US-DOWN start?").assertDoesNotExist()
        compose.onNodeWithText("Next: School run · Mon 07:45").assertExists()
        assertThat(compose.activity.isFinishing).isFalse()
    }
}
