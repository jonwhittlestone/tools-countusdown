package uk.howapped.countusdown.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.howapped.countusdown.core.Phase

// Calm palette: night blue drifting towards dusk, amber in grace, near-black when late.
private val NightBlue = Color(0xFF0E1A2B)
private val Dusk = Color(0xFF241F35)
private val GraceAmber = Color(0xFF3A2814)
private val LateDim = Color(0xFF0D0B09)
private val Amber = Color(0xFFE8B04A)
private val Track = Color(0x22CFD8E3)

/** One breath of the pulse: slow enough to be calming, never a tick (screentime extension, EASE_IN_OUT_SINE). */
private const val BREATH_MS = 4000

@Composable
fun CountdownScreen(state: CountdownUiState) {
    val background by animateColorAsState(
        when (state.phase) {
            Phase.CALM -> lerp(Dusk, NightBlue, state.fractionLeft)
            Phase.GRACE -> GraceAmber
            Phase.LATE, Phase.DONE -> LateDim
            Phase.PENDING -> NightBlue
        },
        animationSpec = tween(durationMillis = 3000),
        label = "background",
    )
    val accent = if (state.phase == Phase.CALM) Mist else Amber

    // The numeral breathes: full opacity down to ~45% and back, forever.
    val breath by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(BREATH_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )

    Box(Modifier.fillMaxSize().background(background), contentAlignment = Alignment.Center) {
        when (state.phase) {
            Phase.CALM, Phase.PENDING -> {
                Ring(fraction = state.fractionLeft, colour = accent)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    BasicText(
                        text = state.minutesLeft.toString(),
                        modifier = Modifier.alpha(breath),
                        style = TextStyle(color = accent, fontSize = 160.sp, fontWeight = FontWeight.Light),
                    )
                    BasicText(
                        text = if (state.minutesLeft == 1) "minute" else "minutes",
                        style = TextStyle(color = accent.copy(alpha = 0.7f), fontSize = 32.sp),
                    )
                    Spacer(Modifier.height(48.dp))
                    BasicText(
                        text = "leave at ${state.leaveAt}",
                        style = TextStyle(color = accent.copy(alpha = 0.55f), fontSize = 28.sp),
                    )
                }
            }
            Phase.GRACE, Phase.LATE, Phase.DONE -> {
                val dim = if (state.phase == Phase.GRACE) breath else 0.35f
                BasicText(
                    text = "Time to go",
                    modifier = Modifier.alpha(dim),
                    style = TextStyle(color = accent, fontSize = 96.sp, fontWeight = FontWeight.Light),
                )
            }
        }
    }
}

/** A thin ring that drains clockwise from 12 o'clock as time runs out. */
@Composable
private fun Ring(fraction: Float, colour: Color) {
    // 80% of the screen height, whatever the TV's density.
    Canvas(Modifier.fillMaxHeight(0.8f).aspectRatio(1f)) {
        val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
        drawArc(Track, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
        drawArc(colour, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false, style = stroke)
    }
}
