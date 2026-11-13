package uk.howapped.countusdown.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import uk.howapped.countusdown.AppClock
import uk.howapped.countusdown.MainActivity
import uk.howapped.countusdown.audio.CountdownService
import uk.howapped.countusdown.audio.CountdownService.Companion.putCountdown
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.core.activeStart
import uk.howapped.countusdown.core.nextStart
import uk.howapped.countusdown.presets.PresetStore
import uk.howapped.countusdown.presets.StoredPreset

/**
 * Starting countdowns by the clock: arms an exact alarm for the earliest next start of any
 * scheduled preset, and launches a run (audio service + full-screen activity) when it fires.
 */
object Runs {
    const val ACTION_START = "uk.howapped.countusdown.action.START_SCHEDULED"
    const val EXTRA_PRESET_ID = "scheduled_preset_id"
    private const val PREFS = "runs"
    private const val SKIP = "skip_dates"

    /** Dates the schedule should not fire on ("skip today"), stored as ISO dates. */
    fun skipDates(context: Context): Set<LocalDate> =
        prefs(context).getStringSet(SKIP, emptySet())!!.map(LocalDate::parse).toSet()

    fun skip(context: Context, date: LocalDate, clock: Clock = AppClock.clock) {
        // Keep only today and later: old dates are no use to anyone.
        val kept = (skipDates(context) + date).filter { !it.isBefore(LocalDate.now(clock)) }
        prefs(context).edit().putStringSet(SKIP, kept.map { it.toString() }.toSet()).apply()
        arm(context, clock)
    }

    /** The next scheduled start across all presets: what the alarm is armed for. */
    fun next(context: Context, clock: Clock): Pair<StoredPreset, Instant>? {
        val now = Instant.now(clock)
        val skip = skipDates(context)
        return PresetStore(context).all()
            .mapNotNull { p -> p.schedule?.nextStart(now, clock.zone, skip)?.let { p to it } }
            .minByOrNull { it.second }
    }

    /** Arms (or re-arms) the alarm for the next scheduled start. Returns when it will fire. */
    fun arm(context: Context, clock: Clock): Instant? {
        val next = next(context, clock)
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = startIntent(context, next)
        if (next == null) {
            alarms.cancel(pending)
        } else {
            // Exact and allowed while idle: the TV box is usually dozing at 07:44.
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.second.toEpochMilli(), pending)
        }
        return next?.second
    }

    /** The scheduled run going on right now, if any (e.g. after a reboot at 07:50). */
    fun active(context: Context, clock: Clock): Countdown? {
        val now = Instant.now(clock)
        val skip = skipDates(context)
        return PresetStore(context).all().firstNotNullOfOrNull { p ->
            p.schedule?.activeStart(p.preset, now, clock.zone, skip)?.let { Countdown(p.preset, it) }
        }
    }

    /** True if any preset is scheduled for today (so "skip today" means something). */
    fun scheduledToday(context: Context, clock: Clock): Boolean {
        val today = LocalDate.now(clock).dayOfWeek
        return PresetStore(context).all().any { it.schedule?.days?.contains(today) == true }
    }

    /** Starts the audio and brings the countdown screen to the front. */
    fun launch(context: Context, countdown: Countdown) {
        context.startForegroundService(CountdownService.intentFor(context, countdown))
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .putCountdown(countdown)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }

    private fun startIntent(context: Context, next: Pair<StoredPreset, Instant>?): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, StartReceiver::class.java).setAction(ACTION_START)
            .putExtra(EXTRA_PRESET_ID, next?.first?.preset?.id)
            .putExtra(CountdownService.EXTRA_STARTED_AT, next?.second?.toEpochMilli() ?: 0L),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
