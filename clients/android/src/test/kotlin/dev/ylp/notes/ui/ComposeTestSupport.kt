package dev.ylp.notes.ui

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText

/** Waits until a node shows [text]: the screens load from the network, off the main thread. */
fun ComposeTestRule.waitUntilText(
    text: String,
    timeoutMillis: Long = 5_000,
) = waitUntil(timeoutMillis) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
