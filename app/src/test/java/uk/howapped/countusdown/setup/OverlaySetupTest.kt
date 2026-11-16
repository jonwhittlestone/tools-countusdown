package uk.howapped.countusdown.setup

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlaySetupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun showsOnlyWhenThePermissionIsMissingAndNothingIsRunning() {
        assertThat(OverlaySetup.needsSetup(allowed = false, running = false, dismissed = false)).isTrue()
        assertThat(OverlaySetup.needsSetup(allowed = true, running = false, dismissed = false)).isFalse()
        // Never in front of the children mid-run.
        assertThat(OverlaySetup.needsSetup(allowed = false, running = true, dismissed = false)).isFalse()
        // "Not now" hides it until the app is next opened.
        assertThat(OverlaySetup.needsSetup(allowed = false, running = false, dismissed = true)).isFalse()
    }

    @Test fun opensTheDisplayOverOtherAppsScreenForThisApp() {
        val intent = OverlaySetup.settingsIntent(context)
        assertThat(intent.action).isEqualTo(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        assertThat(intent.data.toString()).isEqualTo("package:uk.howapped.countusdown")
    }
}
