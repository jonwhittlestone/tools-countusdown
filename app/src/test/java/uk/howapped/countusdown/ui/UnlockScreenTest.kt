package uk.howapped.countusdown.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.howapped.countusdown.lock.Pad

@RunWith(AndroidJUnit4::class)
class UnlockScreenTest {
    @get:Rule val compose = createComposeRule()

    private val code = listOf(Pad.LEFT, Pad.LEFT, Pad.RIGHT, Pad.UP)

    @Test fun saysWhatToDoWithoutShowingTheCode() {
        compose.setContent { UnlockScreen(code, onUnlock = {}, onCancel = {}) }
        compose.onNodeWithText(UNLOCK_TITLE).assertExists()
        compose.onNodeWithText(UNLOCK_HELP).assertExists()
        assertThat(UNLOCK_HELP).doesNotContain("←")
    }

    @Test fun tappingTheArrowsInOrderUnlocks() {
        var unlocked = false
        compose.setContent { UnlockScreen(code, onUnlock = { unlocked = true }, onCancel = {}) }
        code.forEach { compose.onNodeWithTag(arrowTag(it)).performClick() }
        assertThat(unlocked).isTrue()
    }

    @Test fun aWrongTrySaysSoAndStaysLocked() {
        var unlocked = false
        compose.setContent { UnlockScreen(code, onUnlock = { unlocked = true }, onCancel = {}) }
        listOf(Pad.UP, Pad.UP, Pad.UP, Pad.UP).forEach { compose.onNodeWithTag(arrowTag(it)).performClick() }
        compose.onNodeWithText(UNLOCK_WRONG).assertExists()
        assertThat(unlocked).isFalse()
    }

    @Test fun cancelCloses() {
        var cancelled = false
        compose.setContent { UnlockScreen(code, onUnlock = {}, onCancel = { cancelled = true }) }
        compose.onNodeWithText("Cancel").performClick()
        assertThat(cancelled).isTrue()
    }
}
