package uk.howapped.countusdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.howapped.countusdown.lock.Pad

/** Setting a new unlock code: press 4–8 arrows and OK, then the same again to confirm. */
@Composable
fun CodeEntry(onDone: (List<Pad>) -> Unit, onCancel: () -> Unit) {
    var first by remember { mutableStateOf<List<Pad>?>(null) }
    var keys by remember { mutableStateOf(listOf<Pad>()) }
    var message by remember { mutableStateOf("Press the new code: 4 to 8 arrows, then OK") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        Modifier.fillMaxSize().background(Color(0xF0000000))
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyUp) return@onPreviewKeyEvent true
                when (e.key) {
                    Key.DirectionUp -> keys = (keys + Pad.UP).take(8)
                    Key.DirectionDown -> keys = (keys + Pad.DOWN).take(8)
                    Key.DirectionLeft -> keys = (keys + Pad.LEFT).take(8)
                    Key.DirectionRight -> keys = (keys + Pad.RIGHT).take(8)
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> when {
                        keys.size < 4 -> message = "At least 4 arrows, then OK"
                        first == null -> { first = keys; keys = emptyList(); message = "Again, to confirm, then OK" }
                        first == keys -> onDone(keys)
                        else -> { first = null; keys = emptyList(); message = "They didn't match. Start again: 4 to 8 arrows, then OK" }
                    }
                    Key.Back -> onCancel()
                    else -> return@onPreviewKeyEvent false
                }
                true
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            BasicText(message, style = TextStyle(color = Mist, fontSize = 32.sp))
            // Dots, not arrows: anyone watching sees how many, not which.
            BasicText("●".repeat(keys.size).ifEmpty { "–" }, style = TextStyle(color = Mist, fontSize = 48.sp))
        }
    }
}
