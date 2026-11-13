package uk.howapped.countusdown.audio

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import uk.howapped.countusdown.core.CueKind
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.core.Preset
import uk.howapped.countusdown.core.cues

@RunWith(AndroidJUnit4::class)
class VoicePickerTest {
    // Unit tests run from the app module directory, so the real bundled manifest is readable.
    private val manifest = AudioManifest.parse(File("src/main/assets/audio/manifest.json").readText())
    private val start = Instant.parse("2026-11-16T07:45:00Z")
    private fun cuesFor(duration: Int, grace: Int = 5) =
        Countdown(Preset("p", "P", duration, grace), start).cues()

    @Test fun neverTheSameVoiceTwiceInARow() {
        val picker = VoicePicker(manifest, seed = 0)
        val voices = cuesFor(30).map { picker.pick(it).voice }
        voices.zipWithNext().forEach { (a, b) -> assertThat(b).isNotEqualTo(a) }
    }

    @Test fun neverTheSameWordingTwiceInARowWithinATier() {
        val picker = VoicePicker(manifest, seed = 0)
        val firm = cuesFor(30).filter { it.kind == CueKind.MINUTES && it.minutesLeft <= 5 }.map { picker.pick(it).variant }
        firm.zipWithNext().forEach { (a, b) -> assertThat(b).isNotEqualTo(a) }
    }

    @Test fun differentSeedsStartWithDifferentVoices() {
        val first = cuesFor(30).first()
        assertThat(VoicePicker(manifest, seed = 0).pick(first).voice)
            .isNotEqualTo(VoicePicker(manifest, seed = 1).pick(first).voice)
    }

    @Test fun everyCueOfEveryAllowedDurationHasABundledClip() {
        for (duration in Preset.ALLOWED_DURATIONS) for (grace in listOf(0, 5)) {
            val picker = VoicePicker(manifest, seed = 3)
            for (cue in cuesFor(duration, grace)) {
                val key = picker.pick(cue).key
                assertWithMessage("$duration+$grace: $key").that(manifest.hasClip(key)).isTrue()
                assertWithMessage("$key.ogg").that(File("src/main/assets/audio/voice/$key.ogg").exists()).isTrue()
            }
        }
    }
}
