package nl.civion.mobile.reader

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import net.thunderbird.core.common.inject.defaultListQualifier
import net.thunderbird.core.common.mail.html.HtmlSettings
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.feature.mail.message.reader.api.css.CssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.GlobalCssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.PlainTextMessagePreElementCssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.SignatureCssStyleProvider
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * The CIVION stylesheet is added to the reader's list of stylesheets rather than replacing one,
 * so what has to hold is that the list still carries the upstream providers and carries CIVION's
 * as well.
 *
 * This exercises the override against a stand-in for the upstream registration. It cannot catch
 * upstream abandoning the list mechanism altogether — that shows up as CIVION's styling silently
 * not applying, and only the running application can reveal it.
 */
class ReaderModuleTest {

    @Test
    fun `should keep the upstream stylesheets and add the CIVION one last`() {
        val koin = koinApplication {
            modules(upstreamStandIn, civionMessageReaderCssOverrideModule)
        }.koin

        val factories = koin.get<List<CssStyleProvider.Factory>>(
            defaultListQualifier<CssStyleProvider.Factory>(),
        )

        assertThat(factories).hasSize(4)
        assertThat(factories[0]).isInstanceOf<GlobalCssStyleProvider.Factory>()
        assertThat(factories[1]).isInstanceOf<SignatureCssStyleProvider.Factory>()
        assertThat(factories[2]).isInstanceOf<PlainTextMessagePreElementCssStyleProvider.Factory>()
        assertThat(factories[3]).isInstanceOf<CivionReadableCssStyleProvider.Factory>()
    }

    @Test
    fun `should build a CIVION stylesheet from the class names in the graph`() {
        val koin = koinApplication {
            modules(upstreamStandIn, civionMessageReaderCssOverrideModule)
        }.koin

        val factories = koin.get<List<CssStyleProvider.Factory>>(
            defaultListQualifier<CssStyleProvider.Factory>(),
        )
        val style = factories.last().create(HtmlSettings(useDarkMode = false, useFixedWidthFont = false)).style

        assertThat(style).isInstanceOf<String>()
        check(style.contains(".root.main")) { "CIVION rules were not scoped with the injected class names: $style" }
    }

    /**
     * Stands in for the reader's own module: the three stylesheet providers it registers, the
     * list they are registered as, and the class names CIVION's provider needs.
     */
    private val upstreamStandIn = module {
        single<CssClassNameProvider> { FakeCssClassNameProvider }
        factory<GlobalCssStyleProvider.Factory> { GlobalCssStyleProvider.Factory { FakeGlobalProvider } }
        factory<SignatureCssStyleProvider.Factory> { SignatureCssStyleProvider.Factory { FakeSignatureProvider } }
        factory<PlainTextMessagePreElementCssStyleProvider.Factory> {
            PlainTextMessagePreElementCssStyleProvider.Factory { FakePlainTextProvider }
        }
        factory(defaultListQualifier<CssStyleProvider.Factory>()) {
            listOf<CssStyleProvider.Factory>(
                get<GlobalCssStyleProvider.Factory>(),
                get<SignatureCssStyleProvider.Factory>(),
                get<PlainTextMessagePreElementCssStyleProvider.Factory>(),
            )
        }
    }

    private object FakeGlobalProvider : GlobalCssStyleProvider {
        override val style: String = "<style>/* upstream global */</style>"
    }

    private object FakeSignatureProvider : SignatureCssStyleProvider {
        override val style: String = "<style>/* upstream signature */</style>"
    }

    private object FakePlainTextProvider : PlainTextMessagePreElementCssStyleProvider {
        override val style: String = "<style>/* upstream plain text */</style>"
    }

    private object FakeCssClassNameProvider : CssClassNameProvider {
        override val defaultNamespaceClassName: String = "ns"
        override val rootClassName: String = "root"
        override val mainContentClassName: String = "main"
        override val plainTextMessagePreClassName: String = "pre"
        override val signatureClassName: String = "signature"
    }
}
