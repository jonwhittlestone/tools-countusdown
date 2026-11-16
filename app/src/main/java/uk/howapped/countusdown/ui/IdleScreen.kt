package uk.howapped.countusdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val IDLE_MENU_LABEL = "≡  Menu"

/**
 * Opened from the launcher with nothing running: says what's next, with a Menu button. With no
 * countdown to protect, the menu needs no code here (OK on the remote, or a tap).
 */
@Composable
fun IdleScreen(next: String?, onMenu: () -> Unit = {}) {
    Box(Modifier.fillMaxSize().background(Night), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            BasicText("COUNT-US-DOWN", style = TextStyle(color = Mist.copy(alpha = 0.5f), fontSize = 28.sp))
            BasicText(next ?: "Nothing scheduled", style = TextStyle(color = Mist, fontSize = 44.sp))
            TvRow(IDLE_MENU_LABEL, Modifier.width(220.dp), onSelect = onMenu)
            BasicText("Press OK on the remote", style = TextStyle(color = Mist.copy(alpha = 0.4f), fontSize = 16.sp))
        }
    }
}
