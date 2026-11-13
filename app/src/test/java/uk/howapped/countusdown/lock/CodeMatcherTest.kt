package uk.howapped.countusdown.lock

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import uk.howapped.countusdown.lock.Pad.DOWN
import uk.howapped.countusdown.lock.Pad.LEFT
import uk.howapped.countusdown.lock.Pad.RIGHT
import uk.howapped.countusdown.lock.Pad.UP

class CodeMatcherTest {
    private fun CodeMatcher.feedAll(vararg keys: Pad) = keys.map { feed(it) }

    @Test fun theRightSequenceUnlocksOnItsLastKey() {
        val m = CodeMatcher(listOf(LEFT, LEFT, RIGHT, UP))
        assertThat(m.feedAll(LEFT, LEFT, RIGHT, UP)).containsExactly(false, false, false, true).inOrder()
    }

    @Test fun aWrongKeyStartsAgain() {
        val m = CodeMatcher(listOf(LEFT, LEFT, RIGHT, UP))
        assertThat(m.feedAll(LEFT, DOWN, LEFT, RIGHT, UP)).doesNotContain(true)
    }

    @Test fun overlappingAttemptsStillUnlock() {
        // Mashing UP then finishing the code: the extra UP must not spoil it.
        val m = CodeMatcher(listOf(UP, UP, DOWN))
        assertThat(m.feedAll(UP, UP, UP, DOWN).last()).isTrue()
    }

    @Test fun unlockingResetsSoTheCodeMustBeEnteredAgain() {
        val m = CodeMatcher(listOf(UP, DOWN))
        m.feedAll(UP, DOWN)
        assertThat(m.feed(DOWN)).isFalse()
    }

    @Test fun codesRoundTripThroughStorageText() {
        val code = listOf(LEFT, LEFT, RIGHT, UP, DOWN)
        assertThat(Pad.decode(Pad.encode(code))).isEqualTo(code)
    }
}
