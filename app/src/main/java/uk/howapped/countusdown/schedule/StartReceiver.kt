package uk.howapped.countusdown.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.Instant
import uk.howapped.countusdown.AppClock
import uk.howapped.countusdown.audio.CountdownService
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.presets.PresetStore

/** Fires at the scheduled start, and after a reboot or an app update to re-arm the alarm. */
class StartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val clock = AppClock.clock
        when (intent.action) {
            Runs.ACTION_START -> {
                val preset = intent.getStringExtra(Runs.EXTRA_PRESET_ID)?.let { PresetStore(context).byId(it) }
                val at = intent.getLongExtra(CountdownService.EXTRA_STARTED_AT, 0L)
                val startedAt = if (at > 0) Instant.ofEpochMilli(at) else Instant.now(clock)
                // A preset deleted or edited since the alarm was armed: just re-arm below.
                if (preset != null) Runs.launch(context, Countdown(preset.preset, startedAt))
            }
            // Rebooted (or reinstalled) in the middle of a run: pick it back up.
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                Runs.active(context, clock)?.let { Runs.launch(context, it) }
        }
        // Alarms are one-shot: always arm the next one.
        Runs.arm(context, clock)
    }
}
