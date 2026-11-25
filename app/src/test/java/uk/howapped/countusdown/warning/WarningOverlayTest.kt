package uk.howapped.countusdown.warning

import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowWindowManagerImpl
import uk.howapped.countusdown.FixedClockRule
import uk.howapped.countusdown.audio.CountdownService
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.presets.PresetStore
import uk.howapped.countusdown.schedule.Runs
import uk.howapped.countusdown.schedule.StartReceiver

/** The v1.0 warning: a small box over whatever is on the TV, a few minutes before a scheduled start. */
@RunWith(AndroidJUnit4::class)
class WarningOverlayTest {
    @get:Rule val clock = FixedClockRule(MONDAY_SEVEN_FORTY_TWO)
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val start = Instant.parse("2026-11-16T07:45:00Z")

    @After fun notAllowed() = ShadowSettings.setCanDrawOverlays(false)

    private fun warnFires() = StartReceiver().onReceive(
        context,
        Intent(context, StartReceiver::class.java).setAction(Runs.ACTION_WARN)
            .putExtra(Runs.EXTRA_PRESET_ID, "school")
            .putExtra(CountdownService.EXTRA_STARTED_AT, start.toEpochMilli()),
    )

    @Test fun theWarningAlarmStartsTheOverlayWithTheRunsStartTime() {
        ShadowSettings.setCanDrawOverlays(true)
        warnFires()
        val started = shadowOf(context as Application).nextStartedService!!
        assertThat(started.component!!.className).isEqualTo(WarningOverlayService::class.java.name)
        assertThat(started.getLongExtra(CountdownService.EXTRA_STARTED_AT, 0L)).isEqualTo(start.toEpochMilli())
    }

    @Test fun withoutDisplayOverOtherAppsThereIsNoOverlayAndTheTakeoverStillComes() {
        warnFires()
        assertThat(shadowOf(context as Application).nextStartedService).isNull()
    }

    @Test fun theStartPutsTheOverlayAway() {
        Runs.launch(context, Countdown(PresetStore.SEEDS.first().preset, start))
        val stopped = shadowOf(context as Application).nextStoppedService!!
        assertThat(stopped.component!!.className).isEqualTo(WarningOverlayService::class.java.name)
    }

    @Test fun itDrawsTheTimeLeftUntilTheCountdownInABoxThatNeverTakesTheRemote() {
        ShadowSettings.setCanDrawOverlays(true)
        startService(start)
        val box = overlayViews().single()
        assertThat(texts(box)).containsExactly("03:00", "until the countdown").inOrder()
        val params = box.layoutParams as WindowManager.LayoutParams
        assertThat(params.type).isEqualTo(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        assertThat(params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE).isNotEqualTo(0)
        assertThat(params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE).isNotEqualTo(0)
    }

    @Test fun aWarningForAStartAlreadyPastDrawsNothing() {
        ShadowSettings.setCanDrawOverlays(true)
        startService(start.minusSeconds(600))
        assertThat(overlayViews()).isEmpty()
    }

    private fun startService(at: Instant) {
        Robolectric.buildService(
            WarningOverlayService::class.java,
            WarningOverlayService.intentFor(context, "school", at),
        ).create().startCommand(0, 1)
    }

    private fun overlayViews(): List<View> =
        Shadow.extract<ShadowWindowManagerImpl>(context.getSystemService(WindowManager::class.java)).views

    private fun texts(view: View): List<String> = when (view) {
        is TextView -> listOf(view.text.toString())
        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        else -> emptyList()
    }

    companion object {
        const val MONDAY_SEVEN_FORTY_TWO = "2026-11-16T07:42:00Z"
    }
}
