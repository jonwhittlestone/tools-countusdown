package uk.howapped.countusdown.lock

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import uk.howapped.countusdown.lock.CodeAttempt.Result.MORE
import uk.howapped.countusdown.lock.CodeAttempt.Result.UNLOCKED
import uk.howapped.countusdown.lock.CodeAttempt.Result.WRONG
import uk.howapped.countusdown.lock.Pad.DOWN
import uk.howapped.countusdown.lock.Pad.LEFT
import uk.howapped.countusdown.lock.Pad.RIGHT
import uk.howapped.countusdown.lock.Pad.UP

/** The unlock screen's attempt: unlike the silent matcher, it says when a try was wrong. */
class CodeAttemptTest {
    private val code = listOf(LEFT, LEFT, RIGHT, UP)

    @Test fun theRightCodeUnlocksOnItsLastPress() {
        val a = CodeAttempt(code)
        assertThat(code.map(a::press)).containsExactly(MORE, MORE, MORE, UNLOCKED).inOrder()
    }

    @Test fun countsThePressesSoFar() {
        val a = CodeAttempt(code)
        a.press(LEFT); a.press(LEFT)
        assertThat(a.entered).isEqualTo(2)
    }

    @Test fun aWrongTryIsReportedOnceItIsFullLengthAndStartsAgain() {
        val a = CodeAttempt(code)
        assertThat(listOf(LEFT, DOWN, RIGHT, UP).map(a::press)).containsExactly(MORE, MORE, MORE, WRONG).inOrder()
        assertThat(a.entered).isEqualTo(0)
        assertThat(code.map(a::press).last()).isEqualTo(UNLOCKED)
    }
}
