package nl.civion.mobile.provider

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.theme.k9mail.K9MailBoltTheme
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider

/**
 * CIVION Mobile 0.1.0-alpha reuses the existing K-9 derived Bolt theme rather than introducing a
 * CIVION theme module under `core:`. Visual identity is deliberately out of scope for the build gate.
 */
internal class CivionFeatureThemeProvider : FeatureThemeProvider {
    @Composable
    override fun WithTheme(content: @Composable () -> Unit) {
        K9MailBoltTheme {
            content()
        }
    }

    @Composable
    override fun WithTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
        K9MailBoltTheme(darkTheme = darkTheme) {
            content()
        }
    }
}
