package uk.howapped.countusdown

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
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
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

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
}
