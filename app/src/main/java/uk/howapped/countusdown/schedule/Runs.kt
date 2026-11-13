package uk.howapped.countusdown.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import uk.howapped.countusdown.MainActivity
import uk.howapped.countusdown.audio.CountdownService
import uk.howapped.countusdown.audio.CountdownService.Companion.putCountdown
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.core.Preset
import uk.howapped.countusdown.core.Schedule
import uk.howapped.countusdown.core.activeStart
import uk.howapped.countusdown.core.nextStart

/**
 * Starting countdowns by the clock: arms an exact alarm for the next scheduled start, and
 * launches a run (audio service + full-screen activity) when it fires.
 * Until the preset store lands (F7) there is one built-in scheduled preset: the school run.
 */
object Runs {
    val SCHOOL_RUN = Preset(id = "school", name = "School run", durationMinutes = 30, graceMinutes = 5)
    val SCHOOL_SCHEDULE = Schedule(Schedule.WEEKDAYS, LocalTime.of(7, 45))

    const val ACTION_START = "uk.howapped.countusdown.action.START_SCHEDULED"
    private const val PREFS = "runs"
    private const val SKIP = "skip_dates"

    /** Dates the schedule should not fire on ("skip today"), stored as ISO dates. */
    fun skipDates(context: Context): Set<LocalDate> =
        prefs(context).getStringSet(SKIP, emptySet())!!.map(LocalDate::parse).toSet()

    fun skip(context: Context, date: LocalDate) {
        prefs(context).edit().putStringSet(SKIP, (skipDates(context) + date).map { it.toString() }.toSet()).apply()
        arm(context, Clock.systemDefaultZone())
    }

    /** Arms (or re-arms) the alarm for the next scheduled start. Returns when it will fire. */
    fun arm(context: Context, clock: Clock): Instant? {
        val at = SCHOOL_SCHEDULE.nextStart(Instant.now(clock), clock.zone, skipDates(context))
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = startIntent(context, at)
        if (at == null) {
            alarms.cancel(pending)
        } else {
            // Exact and allowed while idle: the TV box is usually dozing at 07:44.
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pending)
        }
        return at
    }

    /** The scheduled run going on right now, if any (e.g. after a reboot at 07:50). */
    fun active(context: Context, clock: Clock): Countdown? =
        SCHOOL_SCHEDULE.activeStart(SCHOOL_RUN, Instant.now(clock), clock.zone, skipDates(context))
            ?.let { Countdown(SCHOOL_RUN, it) }

    /** Starts the audio and brings the countdown screen to the front. */
    fun launch(context: Context, countdown: Countdown) {
        context.startForegroundService(CountdownService.intentFor(context, countdown))
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .putCountdown(countdown)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }

    private fun startIntent(context: Context, at: Instant?): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, StartReceiver::class.java).setAction(ACTION_START)
            .putExtra(CountdownService.EXTRA_STARTED_AT, at?.toEpochMilli() ?: 0L),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
