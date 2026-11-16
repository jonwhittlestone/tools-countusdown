package uk.howapped.countusdown.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlaySetupScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun walksAGrownUpThroughAllowingDisplayOverOtherApps() {
        var opened = false
        var later = false
        compose.setContent { OverlaySetupScreen(onOpenSettings = { opened = true }, onNotNow = { later = true }) }
        compose.onNodeWithText("Allow COUNT-US-DOWN to display over other apps").assertExists()
        compose.onNodeWithText("2.  Find COUNT-US-DOWN in the list and select it").assertExists()
        compose.onNodeWithText("3.  Switch on “Display over other apps”").assertExists()
        compose.onNodeWithText("Open settings").performScrollTo().performClick()
        assertThat(opened).isTrue()
        compose.onNodeWithText("Not now").performScrollTo().performClick()
        assertThat(later).isTrue()
    }
}
