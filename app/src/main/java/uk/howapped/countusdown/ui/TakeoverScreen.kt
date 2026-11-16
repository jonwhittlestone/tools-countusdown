package uk.howapped.countusdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.howapped.countusdown.core.TakeoverFrame

/**
 * Drawn in a see-through window over whatever was on the TV: night blue slowly covers it, and
 * a message says what is happening before the countdown appears.
 */
@Composable
fun TakeoverScreen(frame: TakeoverFrame, presetName: String, leaveAt: String) {
    Box(
        Modifier.fillMaxSize().background(Night.copy(alpha = frame.scrim)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.alpha(frame.message).padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(
                "Time to get ready",
                style = TextStyle(color = Mist, fontSize = 64.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(24.dp))
            BasicText(
                "The $presetName countdown is taking over the screen",
                style = TextStyle(color = Mist.copy(alpha = 0.75f), fontSize = 30.sp, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(40.dp))
            BasicText(
                "leave at $leaveAt",
                style = TextStyle(color = Mist.copy(alpha = 0.55f), fontSize = 28.sp, textAlign = TextAlign.Center),
            )
        }
    }
}
