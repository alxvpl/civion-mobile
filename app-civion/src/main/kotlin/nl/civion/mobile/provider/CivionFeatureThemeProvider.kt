package nl.civion.mobile.provider

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider

/**
 * CIVION Mobile composes the Thunderbird Bolt theme, so the application's navigation and
 * component behaviour match Thunderbird for Android. CIVION identity is carried by the
 * application id, name and launcher icon, not by a divergent theme.
 */
internal class CivionFeatureThemeProvider : FeatureThemeProvider {
    @Composable
    override fun WithTheme(content: @Composable () -> Unit) {
        ThunderbirdBoltTheme {
            content()
        }
    }

    @Composable
    override fun WithTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
        ThunderbirdBoltTheme(darkTheme = darkTheme) {
            content()
        }
    }
}
