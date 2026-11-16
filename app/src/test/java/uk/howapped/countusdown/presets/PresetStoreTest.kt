package uk.howapped.countusdown.presets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import uk.howapped.countusdown.core.Preset
import uk.howapped.countusdown.core.Schedule

@RunWith(AndroidJUnit4::class)
class PresetStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun firstUseSeedsSchoolRunAndFootball() {
        val presets = PresetStore(context).all()
        assertThat(presets.map { it.preset.name }).containsExactly("School run", "Football").inOrder()
        val school = presets.first()
        assertThat(school.preset.durationMinutes).isEqualTo(30)
        assertThat(school.preset.graceMinutes).isEqualTo(5)
        assertThat(school.schedule).isEqualTo(Schedule(Schedule.WEEKDAYS, LocalTime.of(7, 45)))
        val football = presets.last()
        assertThat(football.preset.durationMinutes).isEqualTo(5)
        assertThat(football.schedule).isNull()
    }

    @Test fun editsSurviveANewStore() {
        val edited = PresetStore(context).all().map {
            if (it.preset.id == "school") it.copy(
                preset = it.preset.copy(graceMinutes = 3),
                schedule = Schedule(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(7, 40)),
            ) else it
        }
        PresetStore(context).save(edited)
        assertThat(PresetStore(context).all()).isEqualTo(edited)
        assertThat(PresetStore(context).byId("school")!!.schedule!!.start).isEqualTo(LocalTime.of(7, 40))
    }

    @Test fun onlyDurationsWithRecordedVoiceClipsAreAccepted() {
        val odd = StoredPreset(Preset("x", "Odd", durationMinutes = 7, graceMinutes = 0), null)
        assertThrows(IllegalArgumentException::class.java) { PresetStore(context).save(listOf(odd)) }
    }

    @Test fun aNewPresetGetsAFreshIdAndAnUnusedName() {
        val added = PresetStore.newPreset(PresetStore.SEEDS)
        assertThat(added.preset.id).isNotIn(PresetStore.SEEDS.map { it.preset.id })
        assertThat(added.preset.name).isEqualTo(PresetStore.NAME_CHOICES.first())
        assertThat(added.preset.durationMinutes).isEqualTo(10)
        assertThat(added.schedule).isNull()
        val second = PresetStore.newPreset(PresetStore.SEEDS + added)
        assertThat(second.preset.id).isNotEqualTo(added.preset.id)
        assertThat(second.preset.name).isNotEqualTo(added.preset.name)
    }

    @Test fun addedPresetsAreSavedAndOnlyTheyCanBeDeleted() {
        val store = PresetStore(context)
        val added = PresetStore.newPreset(store.all())
        store.save(store.all() + added)
        assertThat(PresetStore(context).byId(added.preset.id)).isEqualTo(added)
        assertThat(PresetStore.canDelete(added)).isTrue()
        assertThat(PresetStore.canDelete(PresetStore.SEEDS.first())).isFalse()
    }
}
