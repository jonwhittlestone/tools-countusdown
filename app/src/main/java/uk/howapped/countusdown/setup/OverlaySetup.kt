package uk.howapped.countusdown.setup

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * "Display over other apps" (SYSTEM_ALERT_WINDOW) is what lets Android 10+ bring the countdown
 * to the front by itself at 07:45. A user who installs from a store can't run `make grant`, so a
 * setup screen asks them to switch it on, the way Projectivy's install wizard does.
 *
 * Google TV has the screen (Settings → Apps → Special app access → Display over other apps);
 * the standard intent opens it straight at this app (design doc 03.15).
 */
object OverlaySetup {
    fun needsSetup(allowed: Boolean, running: Boolean, dismissed: Boolean): Boolean =
        !allowed && !running && !dismissed

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
}
