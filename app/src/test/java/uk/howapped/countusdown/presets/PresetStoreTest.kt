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

    @Test fun firstUseSeedsSchoolRunAndTheTwoLeaveTheHousePresets() {
        val presets = PresetStore(context).all()
        assertThat(presets.map { it.preset.name })
            .containsExactly("School run", "Leave the house : 30m", "Leave the house : 5s").inOrder()
        val school = presets.first()
        assertThat(school.preset.durationMinutes).isEqualTo(30)
        assertThat(school.preset.graceMinutes).isEqualTo(5)
        assertThat(school.schedule).isEqualTo(Schedule(Schedule.WEEKDAYS, LocalTime.of(7, 45)))
        val thirty = presets[1]
        assertThat(thirty.preset.durationMinutes).isEqualTo(30)
        assertThat(thirty.schedule).isNull()
        val test = presets[2]
        assertThat(test.preset.durationMinutes).isEqualTo(Preset.FIVE_SECONDS)
        assertThat(test.preset.graceMinutes).isEqualTo(0)
        assertThat(test.schedule).isNull()
        assertThat(presets.map { PresetStore.canDelete(it) }).containsExactly(false, false, false)
    }

    @Test fun existingInstallsSwapAnUntouchedFootballForTheNewPresets() {
        // What the TVs saved before: School run and the original Football seed.
        val football = StoredPreset(Preset("football", "Football", 5, 0), null)
        PresetStore(context).save(listOf(PresetStore.SEEDS.first(), football))
        assertThat(PresetStore(context).all()).isEqualTo(PresetStore.SEEDS)
    }

    @Test fun anEditedFootballIsKept() {
        val football = StoredPreset(Preset("football", "Football", 10, 2), null)
        PresetStore(context).save(listOf(PresetStore.SEEDS.first(), football))
        assertThat(PresetStore(context).all()).contains(football)
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

    @Test fun aScheduleWarnsFiveMinutesBeforeByDefault() {
        assertThat(Schedule(Schedule.WEEKDAYS, LocalTime.of(7, 45)).warnMinutes).isEqualTo(5)
        assertThat(Schedule.WARN_CHOICES).containsExactly(0, 2, 5, 10).inOrder()
        assertThat(PresetStore(context).byId("school")!!.schedule!!.warnMinutes).isEqualTo(5)
    }

    @Test fun theWarnTimeIsKeptWhenSaved() {
        val edited = PresetStore(context).all().map {
            if (it.preset.id == "school") it.copy(schedule = it.schedule!!.copy(warnMinutes = 2)) else it
        }
        PresetStore(context).save(edited)
        assertThat(PresetStore(context).byId("school")!!.schedule!!.warnMinutes).isEqualTo(2)
    }

    @Test fun presetsSavedBeforeV1WarnFiveMinutesBefore() {
        // v0 stored schedules without a "warn" field.
        context.getSharedPreferences("presets", Context.MODE_PRIVATE).edit().putString(
            "presets",
            """[{"id":"school","name":"School run","duration":30,"grace":5,"start":"07:40","days":["MONDAY"]}]""",
        ).commit()
        val school = PresetStore(context).byId("school")!!.schedule!!
        assertThat(school.start).isEqualTo(LocalTime.of(7, 40))
        assertThat(school.warnMinutes).isEqualTo(5)
    }

    @Test fun onlyTheOfferedWarnTimesAreAccepted() {
        val odd = PresetStore.SEEDS.first().let { it.copy(schedule = it.schedule!!.copy(warnMinutes = 7)) }
        assertThrows(IllegalArgumentException::class.java) { PresetStore(context).save(listOf(odd)) }
    }

    @Test fun onlyDurationsWithRecordedVoiceClipsAreAccepted() {
        val odd = StoredPreset(Preset("x", "Odd", durationMinutes = 7, graceMinutes = 0), null)
        assertThrows(IllegalArgumentException::class.java) { PresetStore(context).save(listOf(odd)) }
    }

    @Test fun aNewPresetGetsAFreshIdAndAnUnusedName() {
        val added = PresetStore.newPreset(PresetStore.SEEDS)
        assertThat(added.preset.id).isNotIn(PresetStore.SEEDS.map { it.preset.id })
        assertThat(added.preset.name).isEqualTo("Leave the house")
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

    @Test fun newPresetsAreCalledLeaveTheHouseNumberedFromTheSecond() {
        val first = PresetStore.newPreset(PresetStore.SEEDS)
        val second = PresetStore.newPreset(PresetStore.SEEDS + first)
        val third = PresetStore.newPreset(PresetStore.SEEDS + first + second)
        assertThat(listOf(first, second, third).map { it.preset.name })
            .containsExactly("Leave the house", "Leave the house 2", "Leave the house 3").inOrder()
    }
}
