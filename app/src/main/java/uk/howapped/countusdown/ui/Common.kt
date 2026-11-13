package uk.howapped.countusdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Mist = Color(0xFFCFD8E3)
val Night = Color(0xFF0E1A2B)

/**
 * A D-pad friendly row: lights up when focused, OK selects, and (if given) left/right change
 * its value in place. Used by the parent menu and the preset editor.
 */
@Composable
fun TvRow(
    text: String,
    modifier: Modifier = Modifier,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onSelect: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val arrows = onLeft != null || onRight != null
    BasicText(
        text = if (arrows && focused) "‹  $text  ›" else text,
        style = TextStyle(color = if (focused) Night else Mist, fontSize = 26.sp),
        modifier = modifier
            .width(640.dp)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent {
                if (it.type != KeyEventType.KeyUp) return@onKeyEvent false
                when (it.key) {
                    Key.DirectionLeft -> onLeft?.let { f -> f(); true } ?: false
                    Key.DirectionRight -> onRight?.let { f -> f(); true } ?: false
                    else -> false
                }
            }
            .background(if (focused) Mist else Color.Transparent, shape)
            .border(2.dp, Color(0x55CFD8E3), shape)
            .clickable { onSelect() }
            .padding(horizontal = 24.dp, vertical = 9.dp),
    )
}
