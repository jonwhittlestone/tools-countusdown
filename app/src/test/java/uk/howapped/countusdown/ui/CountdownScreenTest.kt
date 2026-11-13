package uk.howapped.countusdown.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.howapped.countusdown.core.Countdown
import uk.howapped.countusdown.core.Phase
import uk.howapped.countusdown.core.Preset

@RunWith(AndroidJUnit4::class)
class CountdownScreenTest {
    @get:Rule val compose = createComposeRule()

    private val countdown = Countdown(Preset("school", "School run", 30, 5), Instant.parse("2026-11-16T07:45:00Z"))

    @Test fun stateRoundsMinutesUpAndDrainsTheRing() {
        val state = countdown.uiStateAt(Instant.parse("2026-11-16T07:52:30Z"), ZoneOffset.UTC)
        assertThat(state.phase).isEqualTo(Phase.CALM)
        assertThat(state.minutesLeft).isEqualTo(23)
        assertThat(state.fractionLeft).isWithin(0.001f).of(0.75f)
        assertThat(state.leaveAt).isEqualTo("08:15")
    }

    @Test fun ringIsEmptyAfterTheTarget() {
        val state = countdown.uiStateAt(Instant.parse("2026-11-16T08:17:00Z"), ZoneOffset.UTC)
        assertThat(state.minutesLeft).isEqualTo(0)
        assertThat(state.fractionLeft).isEqualTo(0f)
    }

    @Test fun calmShowsWholeMinutesAndLeaveAtTime() {
        compose.setContent { CountdownScreen(CountdownUiState(Phase.CALM, 23, 0.75f, "08:15")) }
        compose.onNodeWithText("23").assertExists()
        compose.onNodeWithText("minutes").assertExists()
        compose.onNodeWithText("leave at 08:15").assertExists()
    }

    @Test fun oneMinuteIsSingular() {
        compose.setContent { CountdownScreen(CountdownUiState(Phase.CALM, 1, 0.03f, "08:15")) }
        compose.onNodeWithText("minute").assertExists()
    }

    @Test fun graceAndLateSayTimeToGo() {
        compose.setContent { CountdownScreen(CountdownUiState(Phase.GRACE, 0, 0f, "08:15")) }
        compose.onNodeWithText("Time to go").assertExists()
    }
}
