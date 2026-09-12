package nl.civion.mobile.reader

import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.GlobalCssStyleProvider

/**
 * Makes an HTML message readable without pinching and zooming.
 *
 * This is an *additional* stylesheet, not a replacement: the message reader is handed a list of
 * style providers, and CIVION appends this one to the upstream three rather than substituting
 * for any of them. Upstream's styling keeps evolving and keeps applying; this only adds rules on
 * top, and it costs no change to any upstream file.
 *
 * All of it is scoped to the message body's own classes, so nothing here can reach another
 * WebView.
 *
 * The reader zooms out — making text tiny — when the message lays out wider than the screen, so
 * the rules below are aimed at the two things that actually cause that, rather than at making
 * text bigger and leaving the cause in place.
 */
internal class CivionReadableCssStyleProvider private constructor(
    cssClassNameProvider: CssClassNameProvider,
    private val useDarkMode: Boolean,
) : GlobalCssStyleProvider {

    private val content =
        ".${cssClassNameProvider.rootClassName}.${cssClassNameProvider.mainContentClassName}"

    override val style: String = """
        |<style>
        |  /* Images are the most common reason a message is wider than the screen: senders set
        |     width attributes and inline widths for a desktop layout. Capping them changes
        |     nothing for an image that already fits, and removes the zoom-out for one that does
        |     not. The widths are usually inline, which is why this has to be !important. */
        |  $content img,
        |  $content video {
        |    max-width: 100% !important;
        |    height: auto !important;
        |  }
        |
        |  /* The other common cause is the fixed-width wrapper table a newsletter is built on.
        |     Only the outermost table is constrained. Forcing every nested table to the screen
        |     width squashes multi-column layouts into unreadable slivers, which is worse than
        |     the scrolling it would save. */
        |  $content > table,
        |  $content > div > table {
        |    max-width: 100% !important;
        |  }
        |
        |  /* A single long URL or unbroken token otherwise sets the page width by itself. */
        |  $content a,
        |  $content code,
        |  $content kbd,
        |  $content samp {
        |    overflow-wrap: anywhere;
        |  }
        |
        |  /* Upstream puts the body at 0.9rem — a tenth below the platform default — on top of
        |     the reader's own text-size setting and the system font scale. Reading is what this
        |     screen is for, so the body is set as the mockup of 2026-09-12 (screen 04) has it:
        |     15sp on a 1.62 line, still scaled by the reader's text-size setting and the system
        |     font scale. A class selector outranks upstream's element selector, so this does not
        |     depend on the order the stylesheets happen to be injected in. */
        |  $content {
        |    font-size: 0.9375rem;
        |    line-height: 1.62;
        |  }
        |
        |  /* In the dark theme upstream paints every element #121212; the mockup of 2026-09-12
        |     (screen 04) has the body on the window, #212121, like the header above it. This
        |     sheet comes after upstream's, so the same rule with the window's value wins. */
        |${if (useDarkMode) darkWindowRule() else ""}
        |</style>
    """.trimMargin()

    private fun darkWindowRule(): String = """
        |  html, body, $content, $content * {
        |    background-color: #212121 !important;
        |  }
    """.trimMargin()

    internal class Factory(
        private val cssClassNameProvider: CssClassNameProvider,
    ) : GlobalCssStyleProvider.Factory {
        override fun create(htmlSettings: HtmlSettings): CssStyleProvider = CivionReadableCssStyleProvider(
            cssClassNameProvider = cssClassNameProvider,
            useDarkMode = htmlSettings.useDarkMode,
        )
    }
}
