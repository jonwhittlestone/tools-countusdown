package uk.howapped.countusdown

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource
import org.robolectric.Shadows

/** Makes Robolectric's device a TV (FEATURE_LEANBACK). Order it before the Compose rule. */
class TvRule : ExternalResource() {
    override fun before() {
        val pm = ApplicationProvider.getApplicationContext<android.app.Application>().packageManager
        Shadows.shadowOf(pm).setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
    }
}
