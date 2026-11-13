package uk.howapped.countusdown.lock

/** The remote's direction pad: the only keys a code is made of. */
enum class Pad {
    UP, DOWN, LEFT, RIGHT;

    companion object {
        fun encode(code: List<Pad>): String = TODO("commit #19")
        fun decode(text: String): List<Pad> = TODO("commit #19")
    }
}

class CodeMatcher(private val code: List<Pad>) {
    fun feed(key: Pad): Boolean = TODO("commit #19")
}
