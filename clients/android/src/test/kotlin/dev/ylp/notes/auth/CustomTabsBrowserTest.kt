package dev.ylp.notes.auth

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CustomTabsBrowserTest {
    private val browser = CustomTabsBrowser()
    private val opened = mutableListOf<String>()

    @Test
    fun `the redirect the activity receives ends the sign-in`() = runTest {
        browser.open = { opened += it }

        val result = async { browser.authorize("https://idp/authorize", "app:/callback") }
        testScheduler.runCurrent()
        browser.onPaused()
        browser.onRedirect("app:/callback?code=c")
        browser.onResumed()

        assertEquals("app:/callback?code=c", result.await())
        assertEquals(listOf("https://idp/authorize"), opened)
    }

    @Test
    fun `coming back without a redirect cancels the sign-in`() = runTest {
        browser.open = { opened += it }

        val result = async { browser.authorize("https://idp/authorize", "app:/callback") }
        testScheduler.runCurrent()
        browser.onPaused()
        browser.onResumed()

        assertNull(result.await())
    }

    @Test
    fun `without an activity to open the page, the sign-in is cancelled`() = runTest {
        assertNull(browser.authorize("https://idp/authorize", "app:/callback"))
    }

    @Test
    fun `a redirect or a resume with no sign-in in progress is ignored`() {
        browser.onRedirect("app:/callback?code=c")
        browser.onPaused()
        browser.onResumed()
    }
}
