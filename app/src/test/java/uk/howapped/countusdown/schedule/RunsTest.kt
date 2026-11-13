package uk.howapped.countusdown.schedule

import android.app.AlarmManager
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
        Runs.skip(context, LocalDate.of(2026, 11, 16))
        assertThat(Runs.arm(context, mondaySix)).isEqualTo(Instant.parse("2026-11-17T07:45:00Z"))
    }
}
