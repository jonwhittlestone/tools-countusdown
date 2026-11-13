package uk.howapped.countusdown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** Scaffold only: a calm dark screen with the app name. The countdown arrives in F2. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PlaceholderScreen() }
    }
}

@Composable
fun PlaceholderScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0E1A2B)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = stringResource(R.string.app_name),
            style = TextStyle(color = Color(0xFFCFD8E3), fontSize = 48.sp),
        )
    }
}
