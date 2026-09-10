package nl.civion.mobile.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.ThemeColorScheme
import net.thunderbird.components.ui.bolt.theme.ThemeColorSchemeVariants
import net.thunderbird.components.ui.bolt.theme.ThemeConfig
import net.thunderbird.components.ui.bolt.theme.ThemeImageVariants
import net.thunderbird.components.ui.bolt.theme.ThemeImages
import net.thunderbird.components.ui.bolt.theme.default.defaultThemeElevations
import net.thunderbird.components.ui.bolt.theme.default.defaultThemeShapes
import net.thunderbird.components.ui.bolt.theme.default.defaultThemeSizes
import net.thunderbird.components.ui.bolt.theme.default.defaultThemeSpacings
import net.thunderbird.components.ui.bolt.theme.default.defaultTypography
import nl.civion.mobile.brand.resources.Res
import nl.civion.mobile.brand.resources.android_mail_logo

/**
 * CIVION Mobile's own Bolt theme.
 *
 * Composed in `app-civion` on the public [BoltTheme] entry point, so CIVION's visual identity costs
 * nothing at an upstream merge: no Thunderbird file is touched, and shapes, sizes, spacings,
 * elevations and typography stay upstream's.
 *
 * Two deliberate decisions:
 *
 *  - **The accent is used sparingly.** CIVION red carries `primary` only — selection, the compose
 *    action, CIVION's own actions. Secondary and tertiary roles are neutral, so the interface does
 *    not turn red. `error` is a distinct orange-red, so an error never reads as an accent.
 *  - **The dark scheme is the one that had to improve.** Secondary text (`onSurfaceVariant`),
 *    separators (`outlineVariant`) and the steps between surface containers are lifted, because
 *    grey-on-grey was the most common complaint about the inherited dark theme.
 */
@Composable
fun CivionBoltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val images = ThemeImages(
        logo = Res.drawable.android_mail_logo,
    )

    BoltTheme(
        themeConfig = ThemeConfig(
            colors = ThemeColorSchemeVariants(
                dark = civionDarkColorScheme,
                light = civionLightColorScheme,
            ),
            elevations = defaultThemeElevations,
            images = ThemeImageVariants(
                light = images,
                dark = images,
            ),
            sizes = defaultThemeSizes,
            spacings = defaultThemeSpacings,
            shapes = defaultThemeShapes,
            typography = defaultTypography,
        ),
        darkTheme = darkTheme,
        content = content,
    )
}

internal val civionLightColorScheme = ThemeColorScheme(
    // CIVION red, deep enough to carry white text and to stay calm across a long list.
    primary = Color(color = 0xFF9E1119),
    onPrimary = Color(color = 0xFFFFFFFF),
    primaryContainer = Color(color = 0xFFC62828),
    onPrimaryContainer = Color(color = 0xFFFFFFFF),

    // Neutral, so the accent stays rare.
    secondary = Color(color = 0xFF4A4749),
    onSecondary = Color(color = 0xFFFFFFFF),
    secondaryContainer = Color(color = 0xFF6E696B),
    onSecondaryContainer = Color(color = 0xFFFFFFFF),

    tertiary = Color(color = 0xFF2F4858),
    onTertiary = Color(color = 0xFFFFFFFF),
    tertiaryContainer = Color(color = 0xFF456073),
    onTertiaryContainer = Color(color = 0xFFFFFFFF),

    // Orange-red, so an error is never mistaken for the CIVION accent.
    error = Color(color = 0xFF8A3200),
    onError = Color(color = 0xFFFFFFFF),
    errorContainer = Color(color = 0xFFFFF1E8),
    onErrorContainer = Color(color = 0xFF8A3200),

    surfaceDim = Color(color = 0xFFDDD9D8),
    surface = Color(color = 0xFFFCFAF9),
    surfaceBright = Color(color = 0xFFFCFAF9),
    onSurface = Color(color = 0xFF1B1A1A),
    onSurfaceVariant = Color(color = 0xFF3F4144),

    surfaceContainerLowest = Color(color = 0xFFFFFFFF),
    surfaceContainerLow = Color(color = 0xFFF6F4F3),
    surfaceContainer = Color(color = 0xFFF1EEEC),
    surfaceContainerHigh = Color(color = 0xFFEBE8E6),
    surfaceContainerHighest = Color(color = 0xFFE5E2E0),

    inverseSurface = Color(color = 0xFF302F2F),
    inverseOnSurface = Color(color = 0xFFF3F0EF),
    inversePrimary = Color(color = 0xFFFFB3AB),

    outline = Color(color = 0xFF6E7074),
    outlineVariant = Color(color = 0xFFBFC1C4),

    scrim = Color.Black,

    info = Color(color = 0xFF00538A),
    onInfo = Color(color = 0xFFFFFFFF),
    infoContainer = Color(color = 0xFFF0F8FF),
    onInfoContainer = Color(color = 0xFF00538A),

    success = Color(color = 0xFF17542C),
    onSuccess = Color(color = 0xFFFFFFFF),
    successContainer = Color(color = 0xFFF3F9F4),
    onSuccessContainer = Color(color = 0xFF17542C),

    warning = Color(color = 0xFF6E4310),
    onWarning = Color(color = 0xFFFFFFFF),
    warningContainer = Color(color = 0xFFFEF9E7),
    onWarningContainer = Color(color = 0xFF6E4310),
)

internal val civionDarkColorScheme = ThemeColorScheme(
    // A light tint of the same red: legible on dark without glaring.
    primary = Color(color = 0xFFFFB3AB),
    onPrimary = Color(color = 0xFF67000A),
    primaryContainer = Color(color = 0xFF8E1219),
    onPrimaryContainer = Color(color = 0xFFFFDAD6),

    secondary = Color(color = 0xFFCFC8CA),
    onSecondary = Color(color = 0xFF322E30),
    secondaryContainer = Color(color = 0xFF4A4548),
    onSecondaryContainer = Color(color = 0xFFEDE5E7),

    tertiary = Color(color = 0xFFA8CCE3),
    onTertiary = Color(color = 0xFF0C3243),
    tertiaryContainer = Color(color = 0xFF2A4A5C),
    onTertiaryContainer = Color(color = 0xFFCDE7F8),

    error = Color(color = 0xFFFFB59C),
    onError = Color(color = 0xFF551D00),
    errorContainer = Color(color = 0xFF6F2A00),
    onErrorContainer = Color(color = 0xFFFFDBCC),

    surfaceDim = Color(color = 0xFF17181B),
    surface = Color(color = 0xFF1C1E21),
    surfaceBright = Color(color = 0xFF34373C),
    onSurface = Color(color = 0xFFEAE7E8),
    // Lifted from the inherited value: secondary text was the worst offender in dark.
    onSurfaceVariant = Color(color = 0xFFD2D3D8),

    surfaceContainerLowest = Color(color = 0xFF141619),
    surfaceContainerLow = Color(color = 0xFF212429),
    surfaceContainer = Color(color = 0xFF262A2F),
    surfaceContainerHigh = Color(color = 0xFF2E3238),
    surfaceContainerHighest = Color(color = 0xFF373C43),

    inverseSurface = Color(color = 0xFFEAE7E8),
    inverseOnSurface = Color(color = 0xFF303031),
    inversePrimary = Color(color = 0xFF9E1119),

    // Separators had to become visible; dividers were disappearing into the surface.
    outline = Color(color = 0xFFA0A1A7),
    outlineVariant = Color(color = 0xFF56595E),

    scrim = Color.Black,

    info = Color(color = 0xFFBEE6FF),
    onInfo = Color(color = 0xFF002E41),
    infoContainer = Color(color = 0xFF243447),
    onInfoContainer = Color(color = 0xFFBEE6FF),

    success = Color(color = 0xFF8EE7AA),
    onSuccess = Color(color = 0xFF082B16),
    successContainer = Color(color = 0xFF0E3A1F),
    onSuccessContainer = Color(color = 0xFF8EE7AA),

    warning = Color(color = 0xFFFEE78A),
    onWarning = Color(color = 0xFF3E2A05),
    warningContainer = Color(color = 0xFF463809),
    onWarningContainer = Color(color = 0xFFFEE78A),
)
