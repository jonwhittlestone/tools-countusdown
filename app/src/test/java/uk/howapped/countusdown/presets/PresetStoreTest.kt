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
}
