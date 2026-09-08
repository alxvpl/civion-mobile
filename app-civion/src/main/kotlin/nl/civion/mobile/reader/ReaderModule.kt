package nl.civion.mobile.reader

import net.thunderbird.core.common.inject.defaultListQualifier
import net.thunderbird.feature.mail.message.reader.api.css.CssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.GlobalCssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.PlainTextMessagePreElementCssStyleProvider
import net.thunderbird.feature.mail.message.reader.api.css.SignatureCssStyleProvider
import org.koin.dsl.module
import org.koin.dsl.override

/**
 * Appends CIVION's readability stylesheet to the message reader's own.
 *
 * The reader resolves its stylesheets as one list. Rebinding that list — rather than replacing
 * any single provider — keeps all three upstream providers in place and resolved through Koin,
 * so upstream's styling continues to apply and to evolve, and no upstream file is touched.
 *
 * Include this module *after* the upstream one, so the list being overridden already exists.
 */
internal val civionMessageReaderCssOverrideModule = module {
    factory(defaultListQualifier<CssStyleProvider.Factory>()) {
        listOf<CssStyleProvider.Factory>(
            get<GlobalCssStyleProvider.Factory>(),
            get<SignatureCssStyleProvider.Factory>(),
            get<PlainTextMessagePreElementCssStyleProvider.Factory>(),
            CivionReadableCssStyleProvider.Factory(cssClassNameProvider = get()),
        )
    }.override()
}
