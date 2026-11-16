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
        compose.onNodeWithText("Name: ${PresetStore.NAME_CHOICES.first()}", substring = true).assertExists()
        compose.onNodeWithText("Delete this preset").assertExists()
        compose.onNodeWithText("Save").performScrollTo().performClick()
        assertThat(saved!!.map { it.preset.name }).containsExactly("School run", "Football", PresetStore.NAME_CHOICES.first()).inOrder()
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
}
