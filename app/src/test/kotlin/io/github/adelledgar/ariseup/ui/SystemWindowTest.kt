package io.github.adelledgar.ariseup.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.ui.components.SystemWindow
import io.github.adelledgar.ariseup.ui.components.TypewriterText
import io.github.adelledgar.ariseup.ui.theme.AriseUpTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemWindowTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun showsBracketedTitleAndContent() {
        compose.setContent {
            AriseUpTheme(penalty = false) {
                SystemWindow(title = "Notificación") { Text("Contenido") }
            }
        }
        compose.onNodeWithText("[Notificación]").assertIsDisplayed()
        compose.onNodeWithText("Contenido").assertIsDisplayed()
    }

    @Test
    fun typewriterEventuallyShowsFullText() {
        compose.mainClock.autoAdvance = true
        compose.setContent { AriseUpTheme(penalty = false) { TypewriterText("[Ha subido de nivel]", charDelayMs = 1) } }
        compose.waitUntil(3_000) { compose.onAllNodes(androidx.compose.ui.test.hasText("[Ha subido de nivel]")).fetchSemanticsNodes().isNotEmpty() }
    }
}
