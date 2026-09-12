package nl.civion.mobile.reader

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import kotlin.test.Test
import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider

class CivionReadableCssStyleProviderTest {

    private val style = CivionReadableCssStyleProvider.Factory(FakeCssClassNameProvider)
        .create(HtmlSettings(useDarkMode = false, useFixedWidthFont = false))
        .style

    @Test
    fun `should cap images so a wide one cannot force the page wider than the screen`() {
        assertThat(style).contains(".root.main img")
        assertThat(style).contains("max-width: 100% !important")
        assertThat(style).contains("height: auto !important")
    }

    @Test
    fun `should constrain the outermost table only`() {
        assertThat(style).contains(".root.main > table")

        // A rule for every table would squash nested layout tables into unreadable columns,
        // which is worse than the horizontal scrolling it would save.
        assertThat(style).doesNotContain(".root.main table {")
    }

    @Test
    fun `should let a long link break rather than set the page width`() {
        assertThat(style).contains(".root.main a")
        assertThat(style).contains("overflow-wrap: anywhere")
    }

    @Test
    fun `should set the body to 15sp on the line the mockup has`() {
        assertThat(style).contains("font-size: 0.9375rem")
        assertThat(style).contains("line-height: 1.62")
    }

    @Test
    fun `should paint the dark body on the window and leave the light one alone`() {
        val darkStyle = CivionReadableCssStyleProvider.Factory(FakeCssClassNameProvider)
            .create(HtmlSettings(useDarkMode = true, useFixedWidthFont = false))
            .style

        assertThat(darkStyle).contains("background-color: #212121 !important")
        assertThat(style).doesNotContain("background-color")
    }

    @Test
    fun `should scope every rule to the message body`() {
        // This stylesheet shares a document with mail written by other people, and the same
        // WebView is used elsewhere; nothing here may apply outside the message content.
        val unscopedSelectors = style.lines()
            .map { it.trim() }
            .filter { it.endsWith("{") || it.endsWith(",") }
            .filterNot { it == "<style>" || it.startsWith(".root.main") }

        assertThat(unscopedSelectors).isEmpty()
    }

    private object FakeCssClassNameProvider : CssClassNameProvider {
        override val defaultNamespaceClassName: String = "ns"
        override val rootClassName: String = "root"
        override val mainContentClassName: String = "main"
        override val plainTextMessagePreClassName: String = "pre"
        override val signatureClassName: String = "signature"
    }
}
