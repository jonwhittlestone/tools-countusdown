package uk.howapped.countusdown.setup

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import uk.howapped.countusdown.setup.OverlaySetup.Step
import uk.howapped.countusdown.setup.OverlaySetup.stepFor

@RunWith(AndroidJUnit4::class)
class OverlaySetupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun firstAsksHowItShouldStart() {
        assertThat(stepFor(mode = null, allowed = false, running = false, dismissed = false)).isEqualTo(Step.CHOICE)
    }

    @Test fun automaticNeedsTheSettingStepsUntilTheSettingIsOn() {
        assertThat(stepFor(StartMode.AUTO, allowed = false, running = false, dismissed = false)).isEqualTo(Step.PERMISSION)
        assertThat(stepFor(StartMode.AUTO, allowed = true, running = false, dismissed = false)).isEqualTo(Step.NONE)
    }

    @Test fun manualNeedsNoSetting() {
        assertThat(stepFor(StartMode.MANUAL, allowed = false, running = false, dismissed = false)).isEqualTo(Step.NONE)
    }

    @Test fun alreadyAllowedMeansAutomaticWithoutAsking() {
        // The TVs were set up with `make grant` before the wizard existed: leave them alone.
        assertThat(stepFor(mode = null, allowed = true, running = false, dismissed = false)).isEqualTo(Step.NONE)
    }

    @Test fun neverDuringARunAndNotAgainAfterNotNow() {
        assertThat(stepFor(mode = null, allowed = false, running = true, dismissed = false)).isEqualTo(Step.NONE)
        assertThat(stepFor(mode = null, allowed = false, running = false, dismissed = true)).isEqualTo(Step.NONE)
    }

    @Test fun theChoiceIsRemembered() {
        assertThat(OverlaySetup.mode(context)).isNull()
        OverlaySetup.setMode(context, StartMode.MANUAL)
        assertThat(OverlaySetup.mode(context)).isEqualTo(StartMode.MANUAL)
    }

    @Test fun opensTheDisplayOverOtherAppsScreenForThisApp() {
        val intent = OverlaySetup.settingsIntent(context)
        assertThat(intent.action).isEqualTo(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        assertThat(intent.data.toString()).isEqualTo("package:uk.howapped.countusdown")
    }
}
