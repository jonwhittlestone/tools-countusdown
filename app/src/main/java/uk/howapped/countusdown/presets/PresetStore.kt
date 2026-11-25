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
        // Devices that saved presets before the current seeds existed: drop an untouched Football
        // and add any ready-made preset they're missing.
        val stored = List(array.length()) { decode(array.getJSONObject(it)) }.filter { it != OLD_FOOTBALL }
        val ids = stored.map { it.preset.id }.toSet()
        return stored + SEEDS.filter { it.preset.id !in ids }
    }

    fun save(presets: List<StoredPreset>) {
        presets.forEach {
            require(it.preset.durationMinutes in Preset.ALLOWED_DURATIONS) {
                "no voice clips for ${it.preset.durationMinutes} minutes (allowed: ${Preset.ALLOWED_DURATIONS})"
            }
            require(it.preset.graceMinutes in 0..MAX_GRACE) { "grace must be 0..$MAX_GRACE minutes" }
            it.schedule?.let { s ->
                require(s.warnMinutes in Schedule.WARN_CHOICES) { "warn must be one of ${Schedule.WARN_CHOICES}" }
            }
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
                put("warn", s.warnMinutes)
            }
        }

    private fun decode(o: JSONObject) = StoredPreset(
        Preset(o.getString("id"), o.getString("name"), o.getInt("duration"), o.getInt("grace")),
        if (o.has("start")) {
            val days = o.getJSONArray("days")
            Schedule(
                List(days.length()) { DayOfWeek.valueOf(days.getString(it)) }.toSet(),
                LocalTime.parse(o.getString("start")),
                // Presets saved by v0.x have no "warn": they get the default.
                o.optInt("warn", Schedule.DEFAULT_WARN),
            )
        } else {
            null
        },
    )

    companion object {
        private const val PREFS = "presets"
        private const val KEY = "presets"
        const val MAX_GRACE = 15

        /** A generic name that suits any trip out; extra copies are numbered. */
        const val NEW_NAME = "Leave the house"

        /** A new 10-minute preset that starts by hand: "Leave the house", then "Leave the house 2"… */
        fun newPreset(existing: List<StoredPreset>): StoredPreset {
            val ids = existing.map { it.preset.id }.toSet()
            val n = generateSequence(1) { it + 1 }.first { "custom-$it" !in ids }
            val used = existing.map { it.preset.name }.toSet()
            val name = generateSequence(1) { it + 1 }
                .map { if (it == 1) NEW_NAME else "$NEW_NAME $it" }
                .first { it !in used }
            return StoredPreset(Preset("custom-$n", name, 10, 0), schedule = null)
        }

        /** The ready-made presets always stay; anything added can go. */
        fun canDelete(p: StoredPreset): Boolean = SEEDS.none { it.preset.id == p.preset.id }

        const val TEST_ID = "leave-5s"

        val SEEDS = listOf(
            StoredPreset(Preset("school", "School run", 30, 5), Schedule(Schedule.WEEKDAYS, LocalTime.of(7, 45))),
            // Multipurpose, replacing the old Football seed (design doc 03.19).
            StoredPreset(Preset("leave-30m", "Leave the house : 30m", 30, 5), schedule = null),
            // For smoke testing from the remote: the whole run in 5 seconds.
            StoredPreset(Preset(TEST_ID, "Leave the house : 5s", Preset.FIVE_SECONDS, 0), schedule = null),
        )

        /** The first seed list (before 2026-11-16): an unchanged Football is replaced on upgrade. */
        private val OLD_FOOTBALL = StoredPreset(Preset("football", "Football", 5, 0), schedule = null)
    }
}
