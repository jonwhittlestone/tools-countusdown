package uk.howapped.countusdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class MenuItem(val label: String, val onSelect: () -> Unit)

/**
 * Shown only after the parent's D-pad code. OK on the remote selects. Closes itself if
 * left alone, so it never lingers in front of the children.
 */
@Composable
fun ParentMenu(items: List<MenuItem>, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        first.requestFocus()
        delay(AUTO_CLOSE_MS)
        onDismiss()
    }
    Box(Modifier.fillMaxSize().background(Color(0xE6000000)), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText("Parent menu", style = TextStyle(color = Mist, fontSize = 28.sp))
            items.forEachIndexed { i, item ->
                TvRow(item.label, if (i == 0) Modifier.focusRequester(first) else Modifier, onSelect = item.onSelect)
            }
        }
    }
}

private const val AUTO_CLOSE_MS = 30_000L
