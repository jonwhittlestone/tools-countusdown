package uk.howapped.countusdown.schedule

import android.app.AlarmManager
import uk.howapped.countusdown.setup.OverlaySetup
import uk.howapped.countusdown.setup.StartMode
import uk.howapped.countusdown.presets.PresetStore
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.audio.CountdownService.Companion.putCountdown
import uk.howapped.countusdown.audio.CountdownService
import uk.howapped.countusdown.TakeoverActivity
import uk.howapped.countusdown.MainActivity
import android.content.Intent
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class RunsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val london = ZoneId.of("Europe/London")
    private val mondaySix = Clock.fixed(Instant.parse("2026-11-16T06:00:00Z"), london)

    @Test fun armsAnExactAlarmForTheNextStart() {
        val at = Runs.arm(context, mondaySix)
        assertThat(at).isEqualTo(Instant.parse("2026-11-16T07:45:00Z"))
        val alarm = shadowOf(context.getSystemService(AlarmManager::class.java)).peekNextScheduledAlarm()!!
        assertThat(alarm.triggerAtMs).isEqualTo(at!!.toEpochMilli())
        assertThat(alarm.getType()).isEqualTo(AlarmManager.RTC_WAKEUP)
    }

    @Test fun skippingTodayMovesTheAlarmToTomorrow() {
        Runs.skip(context, LocalDate.of(2026, 11, 16), mondaySix) // the real clock would prune a past date
        assertThat(Runs.arm(context, mondaySix)).isEqualTo(Instant.parse("2026-11-17T07:45:00Z"))
    }

    @Test fun aScheduledStartArrivesThroughTheTakeoverScreen() {
        val run = Countdown(PresetStore.SEEDS.first().preset, Instant.parse("2026-11-16T07:45:00Z"))
        Runs.launch(context, run)
        val started = shadowOf(context as Application).nextStartedActivity
        assertThat(started.component!!.className).isEqualTo(TakeoverActivity::class.java.name)
        assertThat(CountdownService.countdownFrom(started)).isEqualTo(run)
    }

    @Test fun theTakeoverHandsTheSameRunToTheCountdownAndAsksItToFadeIn() {
        val run = Countdown(PresetStore.SEEDS.first().preset, Instant.parse("2026-11-16T07:45:00Z"))
        val handoff = TakeoverActivity.handoff(context, Intent().putCountdown(run))
        assertThat(handoff.component!!.className).isEqualTo(MainActivity::class.java.name)
        assertThat(CountdownService.countdownFrom(handoff)).isEqualTo(run)
        assertThat(handoff.getBooleanExtra(MainActivity.EXTRA_FADE_IN, false)).isTrue()
    }

    @Test fun startingManuallyTurnsOffTheTimedStarts() {
        OverlaySetup.setMode(context, StartMode.MANUAL)
        Runs.arm(context, Clock.fixed(Instant.parse("2026-11-14T09:00:00Z"), ZoneId.of("Europe/London")))
        assertThat(shadowOf(context.getSystemService(AlarmManager::class.java)).peekNextScheduledAlarm()).isNull()
        assertThat(Runs.next(context, Clock.systemUTC())).isNull()
        OverlaySetup.setMode(context, StartMode.AUTO)
    }

    // ── v1.0 warning alarm (F19) ──────────────────────────────────────────────────────────────

    private fun alarms() = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms
    private fun warningAlarm() = alarms().singleOrNull { shadowOf(it.operation).savedIntent.action == Runs.ACTION_WARN }
    private fun startAlarm() = alarms().single { shadowOf(it.operation).savedIntent.action == Runs.ACTION_START }
    private fun saveSchoolRunWarning(minutes: Int) {
        val store = PresetStore(context)
        store.save(store.all().map { p ->
            if (p.preset.id == "school") p.copy(schedule = p.schedule!!.copy(warnMinutes = minutes)) else p
        })
    }

    @Test fun armingAlsoSetsAWarningAlarmTheWarnTimeBeforeTheStart() {
        Runs.arm(context, mondaySix)
        assertThat(startAlarm().triggerAtMs).isEqualTo(Instant.parse("2026-11-16T07:45:00Z").toEpochMilli())
        val warning = warningAlarm()!!
        assertThat(warning.triggerAtMs).isEqualTo(Instant.parse("2026-11-16T07:40:00Z").toEpochMilli())
        assertThat(warning.getType()).isEqualTo(AlarmManager.RTC_WAKEUP)
    }

    @Test fun theWarningKnowsWhichRunAndWhenItStarts() {
        Runs.arm(context, mondaySix)
        val intent = shadowOf(warningAlarm()!!.operation).savedIntent
        assertThat(intent.getStringExtra(Runs.EXTRA_PRESET_ID)).isEqualTo("school")
        assertThat(intent.getLongExtra(CountdownService.EXTRA_STARTED_AT, 0L))
            .isEqualTo(Instant.parse("2026-11-16T07:45:00Z").toEpochMilli())
    }

    @Test fun theWarningFollowsThePresetsWarnTime() {
        saveSchoolRunWarning(10)
        Runs.arm(context, mondaySix)
        assertThat(warningAlarm()!!.triggerAtMs).isEqualTo(Instant.parse("2026-11-16T07:35:00Z").toEpochMilli())
    }

    @Test fun noWarningAlarmWhenWarnBeforeIsOff() {
        saveSchoolRunWarning(0)
        Runs.arm(context, mondaySix)
        assertThat(warningAlarm()).isNull()
        assertThat(startAlarm().triggerAtMs).isEqualTo(Instant.parse("2026-11-16T07:45:00Z").toEpochMilli())
    }

    @Test fun anEarlierWarningIsCancelledWhenWarnBeforeIsTurnedOff() {
        Runs.arm(context, mondaySix)
        saveSchoolRunWarning(0)
        Runs.arm(context, mondaySix)
        assertThat(warningAlarm()).isNull()
    }

    @Test fun skippingTodayMovesTheWarningToTomorrow() {
        Runs.skip(context, LocalDate.of(2026, 11, 16), mondaySix)
        assertThat(warningAlarm()!!.triggerAtMs).isEqualTo(Instant.parse("2026-11-17T07:40:00Z").toEpochMilli())
    }

    @Test fun noWarningWhenStartingManually() {
        OverlaySetup.setMode(context, StartMode.MANUAL)
        Runs.arm(context, mondaySix)
        assertThat(warningAlarm()).isNull()
        OverlaySetup.setMode(context, StartMode.AUTO)
    }

    @Test fun armingInsideTheWarningWindowWarnsStraightAway() {
        val mondaySevenFortyTwo = Clock.fixed(Instant.parse("2026-11-16T07:42:00Z"), london)
        Runs.arm(context, mondaySevenFortyTwo)
        assertThat(warningAlarm()!!.triggerAtMs).isEqualTo(Instant.parse("2026-11-16T07:42:00Z").toEpochMilli())
    }
}
