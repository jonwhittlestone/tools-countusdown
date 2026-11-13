package uk.howapped.countusdown.debug

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import uk.howapped.countusdown.audio.CountdownService
import uk.howapped.countusdown.schedule.Runs
import uk.howapped.countusdown.schedule.StartReceiver

/**
 * Debug builds only. Arms the same exact alarm the 07:45 schedule uses, but N seconds from now:
 *   adb shell am broadcast -a uk.howapped.countusdown.DEBUG_ARM -p uk.howapped.countusdown --ei in_seconds 60
 */
class DebugArmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val at = System.currentTimeMillis() + intent.getIntExtra("in_seconds", 60) * 1000L
        val pending = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, StartReceiver::class.java).setAction(Runs.ACTION_START)
                .putExtra(CountdownService.EXTRA_STARTED_AT, at),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        context.getSystemService(AlarmManager::class.java)
            .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        Log.i("CountUsDown", "debug alarm armed for $at")
    }
}
