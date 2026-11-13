package uk.howapped.countusdown.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.Clock
import java.time.Instant
import uk.howapped.countusdown.audio.CountdownService
import uk.howapped.countusdown.core.Countdown

/** Fires at the scheduled start, and after a reboot or an app update to re-arm the alarm. */
class StartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val clock = Clock.systemDefaultZone()
        when (intent.action) {
            Runs.ACTION_START -> {
                val at = intent.getLongExtra(CountdownService.EXTRA_STARTED_AT, 0L)
                val startedAt = if (at > 0) Instant.ofEpochMilli(at) else Instant.now(clock)
                Runs.launch(context, Countdown(Runs.SCHOOL_RUN, startedAt))
            }
            // Rebooted (or reinstalled) in the middle of a run: pick it back up.
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                Runs.active(context, clock)?.let { Runs.launch(context, it) }
        }
        // Alarms are one-shot: always arm the next one.
        Runs.arm(context, clock)
    }
}
