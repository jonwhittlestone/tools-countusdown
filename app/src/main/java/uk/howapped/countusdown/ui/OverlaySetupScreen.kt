package uk.howapped.countusdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val AUTOMATIC_LABEL = "Automatically, at the set time"
const val MANUAL_LABEL = "Only when I start it"

private val STEPS = listOf(
    "1.  Select “Open settings”",
    "2.  Find COUNT-US-DOWN and select it",
    "3.  Switch on “Display over other apps”, then press Back",
)

/** Step 1 of the wizard: one question, two answers. */
@Composable
fun StartChoiceScreen(onAutomatic: () -> Unit, onManual: () -> Unit) {
    WizardPage("How should COUNT-US-DOWN start?") { first ->
        Choice(AUTOMATIC_LABEL, "Takes over the screen, even if something is playing", Modifier.focusRequester(first), onAutomatic)
        Choice(MANUAL_LABEL, "Start a countdown yourself from the parent menu", Modifier, onManual)
    }
}

/** Step 2, for automatic starts only: the one setting that lets it take over the screen. */
@Composable
fun OverlaySetupScreen(onOpenSettings: () -> Unit, onNotNow: () -> Unit) {
    WizardPage("Allow display over other apps") { first ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            STEPS.forEach { BasicText(it, style = TextStyle(color = Mist, fontSize = 22.sp)) }
        }
        Spacer(Modifier.height(8.dp))
        TvRow("Open settings", Modifier.focusRequester(first), onSelect = onOpenSettings)
        TvRow("Not now", onSelect = onNotNow)
    }
}

@Composable
private fun Choice(label: String, detail: String, modifier: Modifier, onSelect: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TvRow(label, modifier, onSelect = onSelect)
        Spacer(Modifier.height(6.dp))
        BasicText(detail, style = TextStyle(color = Mist.copy(alpha = 0.55f), fontSize = 16.sp))
    }
}

/** Night blue, a title, and a column of D-pad rows with the first one focused. */
@Composable
private fun WizardPage(title: String, content: @Composable (FocusRequester) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    Box(Modifier.fillMaxSize().background(Night), contentAlignment = Alignment.Center) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText("One-time setup", style = TextStyle(color = Mist.copy(alpha = 0.5f), fontSize = 18.sp))
            BasicText(title, style = TextStyle(color = Mist, fontSize = 32.sp))
            Spacer(Modifier.height(8.dp))
            content(first)
        }
    }
}
