package uk.howapped.countusdown.lock

import android.content.Context
import android.content.pm.PackageManager
import android.view.KeyEvent

/** The remote's direction pad: the only keys a code is made of. */
enum class Pad {
    UP, DOWN, LEFT, RIGHT;

    companion object {
        fun from(keyCode: Int): Pad? = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> UP
            KeyEvent.KEYCODE_DPAD_DOWN -> DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> RIGHT
            else -> null
        }

        fun encode(code: List<Pad>): String = code.joinToString(",")
        fun decode(text: String): List<Pad> = text.split(",").filter { it.isNotBlank() }.map(::valueOf)
    }
}

/**
 * Watches D-pad presses for the parent's code. It remembers the last few presses and unlocks
 * when they end with the code, so a stray extra press part-way through never spoils an attempt.
 */
class CodeMatcher(private val code: List<Pad>) {
    private val recent = ArrayDeque<Pad>()

    fun feed(key: Pad): Boolean {
        recent.addLast(key)
        while (recent.size > code.size) recent.removeFirst()
        val unlocked = recent.toList() == code
        if (unlocked) recent.clear()
        return unlocked
    }
}

/** Where the parent's code lives. Not a secret vault: it only has to beat small children. */
object LockStore {
    /** Left, left, right, up, down: easy for a parent to remember, unlikely by accident. */
    val DEFAULT_CODE = listOf(Pad.LEFT, Pad.LEFT, Pad.RIGHT, Pad.UP, Pad.DOWN)
    private const val PREFS = "lock"
    private const val CODE = "code"

    fun code(context: Context): List<Pad> =
        prefs(context).getString(CODE, null)?.let(Pad::decode)?.takeIf { it.size >= 4 } ?: DEFAULT_CODE

    /**
     * Only a TV is locked: it's the shared screen a child could stop the run on. On a phone
     * (a parent's own device) the menu opens without the code.
     */
    fun codeRequired(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    fun setCode(context: Context, code: List<Pad>) {
        require(code.size >= 4) { "a code needs at least 4 presses" }
        prefs(context).edit().putString(CODE, Pad.encode(code)).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/**
 * One try at the code on the unlock screen. Unlike [CodeMatcher], which listens silently, it
 * counts presses (for the dots) and says when a full-length try was wrong, then starts again.
 */
class CodeAttempt(private val code: List<Pad>) {
    enum class Result { MORE, UNLOCKED, WRONG }

    private val keys = mutableListOf<Pad>()

    /** Presses so far in this try. */
    val entered: Int get() = keys.size

    fun press(key: Pad): Result {
        keys += key
        if (keys.size < code.size) return Result.MORE
        val right = keys == code
        keys.clear()
        return if (right) Result.UNLOCKED else Result.WRONG
    }
}
