package uk.howapped.countusdown.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import uk.howapped.countusdown.core.Takeover.FADE_MS
import uk.howapped.countusdown.core.Takeover.TOTAL_MS
import uk.howapped.countusdown.core.Takeover.frameAt

/** The slow hand-over when the alarm takes the screen, modelled on the screentime fade. */
class TakeoverTest {
    @Test fun startsInvisibleSoWhateverWasOnScreenIsStillThere() {
        assertThat(frameAt(0).scrim).isEqualTo(0f)
        assertThat(frameAt(0).message).isEqualTo(0f)
    }

    @Test fun theScreenEasesToNightOverThreeSeconds() {
        assertThat(FADE_MS).isEqualTo(3000L)
        assertThat(frameAt(750).scrim).isLessThan(0.25f) // eases in: slow at first
        assertThat(frameAt(1500).scrim).isWithin(0.01f).of(0.5f)
        assertThat(frameAt(3000).scrim).isEqualTo(1f)
    }

    @Test fun theMessageAppearsOnceTheScreenHasStartedToDim() {
        assertThat(frameAt(800).message).isEqualTo(0f)
        assertThat(frameAt(3000).message).isEqualTo(1f)
    }

    @Test fun theMessageHoldsLongEnoughToReadThenFadesBeforeTheCountdown() {
        assertThat(frameAt(6000).message).isEqualTo(1f)
        assertThat(frameAt(TOTAL_MS).message).isEqualTo(0f)
        assertThat(frameAt(TOTAL_MS).scrim).isEqualTo(1f)
        assertThat(frameAt(TOTAL_MS - 1).done).isFalse()
        assertThat(frameAt(TOTAL_MS).done).isTrue()
    }
}
