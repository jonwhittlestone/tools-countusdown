package uk.howapped.countusdown.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.howapped.countusdown.core.Takeover

@RunWith(AndroidJUnit4::class)
class TakeoverScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun saysWhatIsHappeningAndWhenWeLeave() {
        compose.setContent { TakeoverScreen(Takeover.frameAt(4000), "School run", "08:15") }
        compose.onNodeWithText("Time to get ready").assertExists()
        compose.onNodeWithText("The School run countdown is taking over the screen").assertExists()
        compose.onNodeWithText("leave at 08:15").assertExists()
    }
}
