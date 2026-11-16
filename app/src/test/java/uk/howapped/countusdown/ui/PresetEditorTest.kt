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
import uk.howapped.countusdown.presets.PresetStore
import uk.howapped.countusdown.presets.StoredPreset

@RunWith(AndroidJUnit4::class)
class PresetEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun addPresetMakesANewOneToEditAndSaveKeepsIt() {
        var saved: List<StoredPreset>? = null
        compose.setContent { PresetEditor(PresetStore.SEEDS, onSave = { saved = it }, onClose = {}) }
        compose.onNodeWithText("Add preset").performScrollTo().performClick()
        compose.onNodeWithText("Preset: Leave the house", substring = true).assertExists()
        // One generic name, so there's nothing to pick: no name row.
        compose.onNodeWithText("Name:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Delete this preset").assertExists()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        assertThat(saved!!.map { it.preset.name })
            .containsExactly("School run", "Leave the house : 30m", "Leave the house : 5s", "Leave the house").inOrder()
    }

    @Test fun deletingAnAddedPresetLeavesTheSeeds() {
        var saved: List<StoredPreset>? = null
        compose.setContent { PresetEditor(PresetStore.SEEDS, onSave = { saved = it }, onClose = {}) }
        compose.onNodeWithText("Add preset").performScrollTo().performClick()
        compose.onNodeWithText("Delete this preset").performScrollTo().performClick()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        assertThat(saved).isEqualTo(PresetStore.SEEDS)
    }

    @Test fun theSeedsCannotBeDeleted() {
        compose.setContent { PresetEditor(PresetStore.SEEDS, onSave = {}, onClose = {}) }
        compose.onNodeWithText("Delete this preset").assertDoesNotExist()
    }

    @Test fun theFiveSecondTestLengthIsLabelledInSeconds() {
        compose.setContent { PresetEditor(PresetStore.SEEDS.filter { it.preset.id == PresetStore.TEST_ID }, onSave = {}, onClose = {}) }
        compose.onNodeWithText("Countdown: 5 s (test)", substring = true).assertExists()
    }
}
