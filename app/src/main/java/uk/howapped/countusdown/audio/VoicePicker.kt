package uk.howapped.countusdown.audio

import org.json.JSONObject
import uk.howapped.countusdown.core.Cue
import uk.howapped.countusdown.core.CueKind

/** What the pipeline bundled (assets/audio/manifest.json): voices, variants per tier, clip keys. */
class AudioManifest(val voices: List<String>, val variants: Map<String, Int>, private val clips: Set<String>) {
    fun hasClip(key: String) = key in clips

    companion object {
        fun parse(json: String): AudioManifest {
            val o = JSONObject(json)
            val voices = o.getJSONArray("voices").let { a -> List(a.length()) { a.getString(it) } }
            val v = o.getJSONObject("variants")
            val variants = v.keys().asSequence().associateWith { v.getInt(it) }
            val clips = o.getJSONObject("clips").keys().asSequence().toSet()
            return AudioManifest(voices, variants, clips)
        }
    }
}

data class ClipRef(val key: String, val voice: String, val variant: Int)

/**
 * Chooses who says each cue and which wording they use. A rotation, not a dice roll: variety
 * without ever repeating the same voice (or the same wording in a tier) back-to-back.
 * [seed] shifts the starting point, e.g. the day of the year, so mornings differ.
 */
class VoicePicker(private val manifest: AudioManifest, seed: Int) {
    private var voiceTurn = seed
    private val variantTurn = mutableMapOf<String, Int>()
    private val offset = seed

    fun pick(cue: Cue): ClipRef {
        val voices = manifest.voices
        // Plain rotation through the voices: with two or more, neighbours always differ.
        val voice = voices[Math.floorMod(voiceTurn++, voices.size)]
        val group = when (cue.kind) {
            CueKind.MINUTES -> cue.tier.name.lowercase()
            CueKind.TARGET -> "target"
            CueKind.LATE -> "late"
        }
        val count = manifest.variants.getValue(group)
        val turn = variantTurn.getOrPut(group) { offset }
        variantTurn[group] = turn + 1
        val variant = Math.floorMod(turn, count)
        val key = when (cue.kind) {
            CueKind.MINUTES -> "minutes_${group}_${variant}_${"%02d".format(cue.minutesLeft)}_$voice"
            else -> "${group}_${variant}_$voice"
        }
        return ClipRef(key, voice, variant)
    }
}
