package uk.howapped.countusdown.presets

import android.content.Context
import uk.howapped.countusdown.core.Preset
import uk.howapped.countusdown.core.Schedule

/** A preset plus when (if ever) it starts by itself. */
data class StoredPreset(val preset: Preset, val schedule: Schedule?)

/** The family's presets, kept as JSON in SharedPreferences. Seeded on first use. */
class PresetStore(private val context: Context) {
    fun all(): List<StoredPreset> = TODO("commit #22")
    fun save(presets: List<StoredPreset>): Unit = TODO("commit #22")
    fun byId(id: String): StoredPreset? = TODO("commit #22")
}
