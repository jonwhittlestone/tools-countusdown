package uk.howapped.countusdown.warning

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.time.Duration
import java.time.Instant
import kotlin.math.min
import kotlin.math.roundToInt
import uk.howapped.countusdown.AppClock
import uk.howapped.countusdown.audio.CountdownService
import uk.howapped.countusdown.core.Schedule
import uk.howapped.countusdown.core.Warning
import uk.howapped.countusdown.core.WarningColour
import uk.howapped.countusdown.schedule.Runs

/**
 * The warning before a scheduled start: the screentime extension's box at the top right, over
 * whatever is on the TV. It never takes focus or touches, so the remote still drives the app
 * below. It only draws what [Warning.at] returns, and goes away at the start (or when the run's
 * takeover stops it), so the takeover fades in over the programme as in v0.
 */
class WarningOverlayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private var box: LinearLayout? = null
    private var time: TextView? = null
    private var pulse: ObjectAnimator? = null
    private var chime: ExoPlayer? = null
    // The alarm decides when the box appears (start − Warn before, or now if armed late), so the
    // service only needs a window as wide as the longest choice.
    private val warning = Warning(Duration.ofMinutes(Schedule.WARN_CHOICES.max().toLong()))
    private var start: Instant = Instant.EPOCH

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground()
        val at = intent?.getLongExtra(CountdownService.EXTRA_STARTED_AT, 0L) ?: 0L
        if (at <= 0 || !Settings.canDrawOverlays(this)) {
            finish()
            return START_NOT_STICKY
        }
        start = Instant.ofEpochMilli(at)
        handler.removeCallbacks(tick)
        tick.run()
        return START_NOT_STICKY
    }

    private val tick = object : Runnable {
        override fun run() {
            val now = Instant.now(AppClock.clock)
            val shown = warning.at(now, start)
            if (shown == null) {
                finish()
                return
            }
            if (box == null) {
                addBox()
                playChime() // once, as the box appears
            }
            time?.text = shown.label
            time?.setTextColor(argb(shown.colour))
            setPulsing(shown.pulses)
            // Tick on the second boundary of the countdown, like the extension.
            val msToNextSecond = Duration.between(now, start).toMillis() % 1000
            handler.postDelayed(this, if (msToNextSecond == 0L) 1000 else msToNextSecond)
        }
    }

    /** The GNOME box, scaled so it takes the same share of the screen as on a 1080p monitor. */
    private fun addBox() {
        val metrics = resources.displayMetrics
        val scale = min(metrics.widthPixels, metrics.heightPixels) / 1080f
        fun px(v: Int) = (v * scale).roundToInt()

        time = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 62 * scale)
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }
        val caption = TextView(this).apply {
            text = CAPTION
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 14 * scale)
            setTextColor(CAPTION_COLOUR)
        }
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(26), px(14), px(26), px(14))
            background = GradientDrawable().apply {
                setColor(BOX_COLOUR)
                cornerRadius = 12 * scale
            }
            addView(time)
            addView(caption)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = px(40) // a little inside the edge: TVs overscan
            y = px(40)
            title = "CountUsDownWarning"
        }
        windowManager.addView(box, params)
    }

    /**
     * One soft chime (pipeline/countusdown_pipeline/chime.py). It doesn't take audio focus, so the
     * programme underneath carries on playing rather than pausing.
     */
    private fun playChime() {
        chime = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_NOTIFICATION_EVENT)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION).build(),
                /* handleAudioFocus = */ false,
            )
            setMediaItem(MediaItem.fromUri(CHIME))
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) releaseChime()
                }
            })
            prepare()
            play()
        }
    }

    private fun releaseChime() {
        chime?.release()
        chime = null
    }

    /** Opacity 255 → 60 → 255 over 900 ms each way, EASE_IN_OUT_SINE, as the extension does. */
    private fun setPulsing(on: Boolean) {
        val view = box ?: return
        if (on && pulse == null) {
            pulse = ObjectAnimator.ofFloat(view, "alpha", 1f, 60f / 255f).apply {
                duration = 900
                interpolator = PathInterpolator(0.37f, 0f, 0.63f, 1f)
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        } else if (!on && pulse != null) {
            pulse?.cancel()
            pulse = null
            view.alpha = 1f
        }
    }

    private fun goForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Warning", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("Countdown starting soon")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun finish() {
        handler.removeCallbacksAndMessages(null)
        pulse?.cancel()
        pulse = null
        releaseChime()
        box?.let { windowManager.removeView(it) }
        box = null
        time = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (box != null) finish()
        super.onDestroy()
    }

    companion object {
        const val CAPTION = "until the countdown"
        const val CHIME = "asset:///audio/chime.ogg"
        private const val CHANNEL = "warning"
        private const val NOTIFICATION_ID = 2 // the countdown's own service uses 1

        // The screentime extension's colours (tools-tv-screentime TimerStyle).
        private const val BOX_COLOUR = 0xE00A0A0A.toInt()
        private const val CAPTION_COLOUR = 0xFF999999.toInt()
        private fun argb(colour: WarningColour): Int = when (colour) {
            WarningColour.GREY -> 0xFFD0D0D0.toInt()
            WarningColour.YELLOW -> 0xFFC8D400.toInt()
            WarningColour.AMBER -> 0xFFE8A000.toInt()
        }

        fun intentFor(context: Context, presetId: String?, start: Instant): Intent =
            Intent(context, WarningOverlayService::class.java)
                .putExtra(Runs.EXTRA_PRESET_ID, presetId)
                .putExtra(CountdownService.EXTRA_STARTED_AT, start.toEpochMilli())

        fun stop(context: Context) {
            context.stopService(Intent(context, WarningOverlayService::class.java))
        }
    }
}
