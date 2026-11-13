package uk.howapped.countusdown.presets

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject
import uk.howapped.countusdown.core.Preset
import uk.howapped.countusdown.core.Schedule

/** A preset plus when (if ever) it starts by itself. */
data class StoredPreset(val preset: Preset, val schedule: Schedule?)

/**
 * The family's presets, kept as JSON in SharedPreferences: a handful of small records that
 * must be readable synchronously from an alarm receiver. Seeded on first use.
 */
class PresetStore(private val context: Context) {
    fun all(): List<StoredPreset> {
        val json = prefs().getString(KEY, null) ?: return SEEDS
        val array = JSONArray(json)
        return List(array.length()) { decode(array.getJSONObject(it)) }
    }

    fun save(presets: List<StoredPreset>) {
        presets.forEach {
            require(it.preset.durationMinutes in Preset.ALLOWED_DURATIONS) {
                "no voice clips for ${it.preset.durationMinutes} minutes (allowed: ${Preset.ALLOWED_DURATIONS})"
            }
            require(it.preset.graceMinutes in 0..MAX_GRACE) { "grace must be 0..$MAX_GRACE minutes" }
        }
        prefs().edit().putString(KEY, JSONArray(presets.map(::encode)).toString()).apply()
    }

    fun byId(id: String): StoredPreset? = all().firstOrNull { it.preset.id == id }

    private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun encode(p: StoredPreset) = JSONObject()
        .put("id", p.preset.id)
        .put("name", p.preset.name)
        .put("duration", p.preset.durationMinutes)
        .put("grace", p.preset.graceMinutes)
        .apply {
            p.schedule?.let { s ->
                put("start", s.start.toString())
                put("days", JSONArray(s.days.sorted().map { it.name }))
            }
        }

    private fun decode(o: JSONObject) = StoredPreset(
        Preset(o.getString("id"), o.getString("name"), o.getInt("duration"), o.getInt("grace")),
        if (o.has("start")) {
            val days = o.getJSONArray("days")
            Schedule(List(days.length()) { DayOfWeek.valueOf(days.getString(it)) }.toSet(), LocalTime.parse(o.getString("start")))
        } else {
            null
        },
    )

    companion object {
        private const val PREFS = "presets"
        private const val KEY = "presets"
        const val MAX_GRACE = 15

        val SEEDS = listOf(
            StoredPreset(Preset("school", "School run", 30, 5), Schedule(Schedule.WEEKDAYS, LocalTime.of(7, 45))),
            StoredPreset(Preset("football", "Football", 5, 0), schedule = null),
        )
    }
}
