package uk.howapped.countusdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class MenuItem(val label: String, val onSelect: () -> Unit)

/**
 * Shown only after the parent's D-pad code. Plain text rows that light up when focused;
 * OK on the remote selects. Closes itself if left alone, so it never lingers in front of the kids.
 */
@Composable
fun ParentMenu(items: List<MenuItem>, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        first.requestFocus()
        delay(AUTO_CLOSE_MS)
        onDismiss()
    }
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText("Parent menu", style = TextStyle(color = Color(0xFFCFD8E3), fontSize = 28.sp))
            items.forEachIndexed { i, item ->
                var focused by remember { mutableStateOf(false) }
                val shape = RoundedCornerShape(12.dp)
                BasicText(
                    text = item.label,
                    style = TextStyle(color = if (focused) Color(0xFF0E1A2B) else Color(0xFFCFD8E3), fontSize = 32.sp),
                    modifier = Modifier
                        .width(520.dp)
                        .then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                        .onFocusChanged { focused = it.isFocused }
                        .background(if (focused) Color(0xFFCFD8E3) else Color.Transparent, shape)
                        .border(2.dp, Color(0x55CFD8E3), shape)
                        .clickable { item.onSelect() }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                )
            }
        }
    }
}

private const val AUTO_CLOSE_MS = 20_000L
