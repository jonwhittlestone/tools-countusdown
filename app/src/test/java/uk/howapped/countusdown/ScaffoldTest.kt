package uk.howapped.countusdown

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke tests for the scaffold: the app launches, and Android TV can list it. */
@RunWith(AndroidJUnit4::class)
class ScaffoldTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensStraightIntoTheCountdown() {
        compose.onNodeWithText("leave at", substring = true).assertExists()
    }

    @Test
    fun isListedOnTheTvHomeScreenWithABanner() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val tvLauncher = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .setPackage(context.packageName)
        val activities = context.packageManager.queryIntentActivities(tvLauncher, 0)
        assertThat(activities.map { it.activityInfo.name }).containsExactly(MainActivity::class.java.name)

        val appInfo: ApplicationInfo = context.applicationInfo
        assertThat(appInfo.banner).isNotEqualTo(0)
    }
}
