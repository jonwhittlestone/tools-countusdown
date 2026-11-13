package uk.howapped.countusdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalTime
import uk.howapped.countusdown.core.Preset
import uk.howapped.countusdown.core.Schedule
import uk.howapped.countusdown.presets.PresetStore
import uk.howapped.countusdown.presets.StoredPreset

/** The ways a preset can start, cycled with left/right. Custom day sets survive untouched. */
private val EVERY_DAY = DayOfWeek.entries.toSet()

private fun describe(days: Set<DayOfWeek>?): String = when (days) {
    null -> "Off (start by hand)"
    Schedule.WEEKDAYS -> "Weekdays"
    EVERY_DAY -> "Every day"
    else -> days.sorted().joinToString(" ") { it.name.take(3).lowercase().replaceFirstChar(Char::uppercase) }
}

private fun <T> List<T>.step(current: T, by: Int): T = this[Math.floorMod(indexOf(current).coerceAtLeast(0) + by, size)]

/**
 * Editing presets with only a D-pad: up/down picks a row, left/right changes it, OK on Save.
 * Names are fixed (typing on a TV remote is miserable); durations only offer values the
 * voice library has clips for.
 */
@Composable
fun PresetEditor(presets: List<StoredPreset>, onSave: (List<StoredPreset>) -> Unit, onClose: () -> Unit) {
    var working by remember { mutableStateOf(presets) }
    var index by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }

    val current = working[index]
    fun update(change: (StoredPreset) -> StoredPreset) {
        working = working.toMutableList().also { it[index] = change(it[index]) }
    }
    fun withPreset(change: (Preset) -> Preset) = update { it.copy(preset = change(it.preset)) }

    Box(Modifier.fillMaxSize().background(Color(0xF0000000)), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText("Edit presets", style = TextStyle(color = Mist, fontSize = 28.sp))
            TvRow(
                "Preset: ${current.preset.name}", Modifier.focusRequester(first),
                onLeft = { index = Math.floorMod(index - 1, working.size) },
                onRight = { index = Math.floorMod(index + 1, working.size) },
            )
            TvRow(
                "Countdown: ${current.preset.durationMinutes} min",
                onLeft = { withPreset { it.copy(durationMinutes = Preset.ALLOWED_DURATIONS.step(it.durationMinutes, -1)) } },
                onRight = { withPreset { it.copy(durationMinutes = Preset.ALLOWED_DURATIONS.step(it.durationMinutes, +1)) } },
            )
            TvRow(
                "Grace after: ${current.preset.graceMinutes} min",
                onLeft = { withPreset { it.copy(graceMinutes = (it.graceMinutes - 1).coerceAtLeast(0)) } },
                onRight = { withPreset { it.copy(graceMinutes = (it.graceMinutes + 1).coerceAtMost(PresetStore.MAX_GRACE)) } },
            )
            val modes: List<Set<DayOfWeek>?> = listOfNotNull(null, Schedule.WEEKDAYS, EVERY_DAY, current.schedule?.days)
                .distinct()
            TvRow(
                "Starts by itself: ${describe(current.schedule?.days)}",
                onLeft = { update { it.withDays(modes.step(it.schedule?.days, -1)) } },
                onRight = { update { it.withDays(modes.step(it.schedule?.days, +1)) } },
            )
            current.schedule?.let { s ->
                TvRow(
                    "At: ${s.start}",
                    onLeft = { update { it.copy(schedule = s.copy(start = s.start.minusMinutes(5))) } },
                    onRight = { update { it.copy(schedule = s.copy(start = s.start.plusMinutes(5))) } },
                )
            }
            TvRow("Save", onSelect = { onSave(working) })
            TvRow("Cancel", onSelect = onClose)
        }
    }
}

private fun StoredPreset.withDays(days: Set<DayOfWeek>?): StoredPreset =
    copy(schedule = days?.let { Schedule(it, schedule?.start ?: LocalTime.of(7, 45)) })
