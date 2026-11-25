package uk.howapped.countusdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.howapped.countusdown.lock.CodeAttempt
import uk.howapped.countusdown.lock.Pad

const val UNLOCK_TITLE = "Parent menu"
const val UNLOCK_HELP = "Enter the parent code with the arrows on the remote, or tap the arrows below"
const val UNLOCK_WRONG = "Not quite. Try again"

fun arrowTag(pad: Pad) = "arrow-${pad.name}"

private val GLYPH = mapOf(Pad.UP to "▲", Pad.DOWN to "▼", Pad.LEFT to "◀", Pad.RIGHT to "▶")

/**
 * Asks for the parent code in the open, rather than listening for it silently: the remote's
 * arrows or (on a phone) the on-screen ones. Dots show how far along a try is, never which keys.
 */
@Composable
fun UnlockScreen(code: List<Pad>, onUnlock: () -> Unit, onCancel: () -> Unit) {
    val attempt = remember(code) { CodeAttempt(code) }
    var entered by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun press(pad: Pad) {
        when (attempt.press(pad)) {
            CodeAttempt.Result.UNLOCKED -> onUnlock()
            CodeAttempt.Result.WRONG -> wrong = true
            CodeAttempt.Result.MORE -> wrong = false
        }
        entered = attempt.entered
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xF0000000))
            .focusRequester(focus)
            .focusable()
            // Every remote key is ours: arrows are the code, and OK (still held from opening
            // this screen) must not press an on-screen arrow.
            .onPreviewKeyEvent { e ->
                if (e.key == Key.Back) {
                    if (e.type == KeyEventType.KeyUp) onCancel()
                    return@onPreviewKeyEvent true
                }
                val pad = when (e.key) {
                    Key.DirectionUp -> Pad.UP
                    Key.DirectionDown -> Pad.DOWN
                    Key.DirectionLeft -> Pad.LEFT
                    Key.DirectionRight -> Pad.RIGHT
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> return@onPreviewKeyEvent true
                    else -> return@onPreviewKeyEvent false
                }
                if (e.type == KeyEventType.KeyUp) press(pad)
                true
            },
        contentAlignment = Alignment.Center,
    ) {
        // Scrolls on a small phone screen; a TV shows it all at once.
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText(UNLOCK_TITLE, style = TextStyle(color = Mist, fontSize = 32.sp))
            BasicText(UNLOCK_HELP, style = TextStyle(color = Mist.copy(alpha = 0.7f), fontSize = 20.sp))
            BasicText(
                "●".repeat(entered) + "○".repeat(code.size - entered),
                style = TextStyle(color = Mist, fontSize = 36.sp),
            )
            BasicText(if (wrong) UNLOCK_WRONG else " ", style = TextStyle(color = Color(0xFFE8B04A), fontSize = 20.sp))
            // One row, so it fits a landscape phone as well as a TV.
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(Pad.LEFT, Pad.UP, Pad.DOWN, Pad.RIGHT).forEach { Arrow(it, ::press) }
            }
            BasicText(
                "Cancel",
                style = TextStyle(color = Mist.copy(alpha = 0.7f), fontSize = 20.sp),
                modifier = Modifier.clickable { onCancel() },
            )
        }
    }
}

@Composable
private fun Arrow(pad: Pad, onPress: (Pad) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(72.dp).testTag(arrowTag(pad))
            .border(2.dp, Color(0x55CFD8E3), shape)
            .clickable { onPress(pad) },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(GLYPH.getValue(pad), style = TextStyle(color = Mist, fontSize = 28.sp))
    }
}
