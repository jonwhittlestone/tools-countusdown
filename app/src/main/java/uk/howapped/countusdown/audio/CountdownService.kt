package uk.howapped.countusdown.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.core.Phase
import uk.howapped.countusdown.core.Preset

/**
 * Plays a countdown's audio from start to finish. A foreground service, so the voice and
 * soundscape carry on even if the screen is covered (e.g. Home is pressed) or the activity
 * is recreated. Stops itself when the countdown is done.
 */
class CountdownService : Service() {
    private val clock: Clock = Clock.systemDefaultZone()
    private val handler = Handler(Looper.getMainLooper())
    private var player: ExoClipPlayer? = null
    private var scheduler: CueScheduler? = null
    private var countdown: Countdown? = null

    private val ticker = object : Runnable {
        override fun run() {
            val now = Instant.now(clock)
            scheduler?.tick(now)
            if (countdown?.phaseAt(now) == Phase.DONE) stopSelf() else handler.postDelayed(this, 1000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        val next = intent?.let(::countdownFrom) ?: return START_NOT_STICKY
        if (next != countdown) {
            // A new run replaces whatever was playing.
            handler.removeCallbacks(ticker)
            player?.release()
            val manifest = AudioManifest.parse(assets.open("audio/manifest.json").bufferedReader().readText())
            val exo = ExoClipPlayer(this)
            player = exo
            countdown = next
            scheduler = CueScheduler(next, VoicePicker(manifest, seed = LocalDate.now(clock).dayOfYear), exo)
            handler.post(ticker)
        }
        return START_NOT_STICKY
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Countdown", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("COUNT-US-DOWN")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "countdown"
        private const val NOTIFICATION_ID = 1
        const val EXTRA_PRESET_ID = "preset_id"
        const val EXTRA_PRESET_NAME = "preset_name"
        const val EXTRA_DURATION = "duration_minutes"
        const val EXTRA_GRACE = "grace_minutes"
        const val EXTRA_STARTED_AT = "started_at_ms"

        fun intentFor(context: Context, countdown: Countdown): Intent =
            Intent(context, CountdownService::class.java).putCountdown(countdown)

        fun Intent.putCountdown(c: Countdown): Intent = putExtra(EXTRA_PRESET_ID, c.preset.id)
            .putExtra(EXTRA_PRESET_NAME, c.preset.name)
            .putExtra(EXTRA_DURATION, c.preset.durationMinutes)
            .putExtra(EXTRA_GRACE, c.preset.graceMinutes)
            .putExtra(EXTRA_STARTED_AT, c.startedAt.toEpochMilli())

        fun countdownFrom(intent: Intent): Countdown? {
            if (!intent.hasExtra(EXTRA_STARTED_AT)) return null
            return Countdown(
                Preset(
                    id = intent.getStringExtra(EXTRA_PRESET_ID) ?: "school",
                    name = intent.getStringExtra(EXTRA_PRESET_NAME) ?: "School run",
                    durationMinutes = intent.getIntExtra(EXTRA_DURATION, 30),
                    graceMinutes = intent.getIntExtra(EXTRA_GRACE, 5),
                ),
                Instant.ofEpochMilli(intent.getLongExtra(EXTRA_STARTED_AT, 0)),
            )
        }
    }
}
