package nl.civion.mobile.provider

import androidx.compose.runtime.Composable
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import nl.civion.mobile.theme.CivionBoltTheme

/**
 * CIVION Mobile composes its own Bolt theme: upstream's shapes, sizes, spacings and typography with
 * CIVION's colours. See [CivionBoltTheme] for what the palette decides and why.
 */
internal class CivionFeatureThemeProvider : FeatureThemeProvider {
    @Composable
    override fun WithTheme(content: @Composable () -> Unit) {
        CivionBoltTheme {
            content()
        }
    }

    @Composable
    override fun WithTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
        CivionBoltTheme(darkTheme = darkTheme) {
            content()
        }
    }
}
