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

    @Test fun firstStepIsTwoChoices() {
        var picked = ""
        compose.setContent { StartChoiceScreen(onAutomatic = { picked = "auto" }, onManual = { picked = "manual" }) }
        compose.onNodeWithText("How should COUNT-US-DOWN start?").assertExists()
        compose.onNodeWithText(AUTOMATIC_LABEL).performScrollTo().performClick()
        assertThat(picked).isEqualTo("auto")
        compose.onNodeWithText(MANUAL_LABEL).performScrollTo().performClick()
        assertThat(picked).isEqualTo("manual")
    }

    @Test fun automaticThenWalksThroughTheSetting() {
        var opened = false
        var later = false
        compose.setContent { OverlaySetupScreen(onOpenSettings = { opened = true }, onNotNow = { later = true }) }
        compose.onNodeWithText("Allow display over other apps").assertExists()
        compose.onNodeWithText("2.  Find COUNT-US-DOWN and select it").assertExists()
        compose.onNodeWithText("3.  Switch on “Display over other apps”, then press Back").assertExists()
        compose.onNodeWithText("Open settings").performScrollTo().performClick()
        assertThat(opened).isTrue()
        compose.onNodeWithText("Not now").performScrollTo().performClick()
        assertThat(later).isTrue()
    }
}
