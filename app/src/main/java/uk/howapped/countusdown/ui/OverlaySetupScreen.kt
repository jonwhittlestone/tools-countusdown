package uk.howapped.countusdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

private val STEPS = listOf(
    "1.  Select “Open settings” below",
    "2.  Find COUNT-US-DOWN in the list and select it",
    "3.  Switch on “Display over other apps”",
    "4.  Press Back to come back here",
)

/**
 * A one-time wizard, like Projectivy's: explains why the setting matters and takes the grown-up
 * straight to it. Disappears by itself once the setting is on.
 */
@Composable
fun OverlaySetupScreen(onOpenSettings: () -> Unit, onNotNow: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    Box(Modifier.fillMaxSize().background(Night), contentAlignment = Alignment.Center) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText("One-time setup", style = TextStyle(color = Mist.copy(alpha = 0.5f), fontSize = 18.sp))
            Spacer(Modifier.height(8.dp))
            BasicText("Allow COUNT-US-DOWN to display over other apps", style = TextStyle(color = Mist, fontSize = 32.sp))
            Spacer(Modifier.height(12.dp))
            BasicText(
                "So the countdown can come to the front at its start time, even while something else is on screen.",
                style = TextStyle(color = Mist.copy(alpha = 0.7f), fontSize = 18.sp),
            )
            Spacer(Modifier.height(24.dp))
            Column(Modifier.width(640.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                STEPS.forEach { BasicText(it, style = TextStyle(color = Mist, fontSize = 22.sp)) }
            }
            Spacer(Modifier.height(12.dp))
            BasicText(
                "Or find it yourself: Settings → Apps → Special app access → Display over other apps",
                style = TextStyle(color = Mist.copy(alpha = 0.5f), fontSize = 16.sp),
            )
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TvRow("Open settings", Modifier.focusRequester(first), onSelect = onOpenSettings)
                TvRow("Not now", onSelect = onNotNow)
            }
        }
    }
}
