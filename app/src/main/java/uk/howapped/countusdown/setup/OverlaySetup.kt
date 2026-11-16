package uk.howapped.countusdown.setup

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** How runs begin: by the clock (taking over the screen) or only when someone starts one. */
enum class StartMode { AUTO, MANUAL }

/**
 * The first-run wizard. It asks how countdowns should start, and for automatic starts walks the
 * user to "Display over other apps" (SYSTEM_ALERT_WINDOW), which lets Android 10+ bring the
 * countdown to the front by itself. A store install can't run `make grant`, so this is how a
 * household turns it on, the way Projectivy's install wizard does.
 *
 * Google TV has the screen (Settings → Apps → Special app access → Display over other apps);
 * the standard intent opens it straight at this app (design doc 03.15).
 */
object OverlaySetup {
    enum class Step { NONE, CHOICE, PERMISSION }

    fun stepFor(mode: StartMode?, allowed: Boolean, running: Boolean, dismissed: Boolean): Step = when {
        running || dismissed || mode == StartMode.MANUAL -> Step.NONE
        // Already allowed (e.g. `make grant` before the wizard existed): automatic, no questions.
        allowed -> Step.NONE
        mode == null -> Step.CHOICE
        else -> Step.PERMISSION
    }

    fun mode(context: Context): StartMode? =
        prefs(context).getString(MODE, null)?.let { runCatching { StartMode.valueOf(it) }.getOrNull() }

    fun setMode(context: Context, mode: StartMode) {
        prefs(context).edit().putString(MODE, mode.name).apply()
    }

    fun allowed(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun settingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    /** Opens the screen, or the main settings if this device somehow has no such screen. */
    fun open(context: Context) {
        try {
            context.startActivity(settingsIntent(context))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private const val MODE = "start_mode"
    private fun prefs(context: Context) = context.getSharedPreferences("setup", Context.MODE_PRIVATE)
}
