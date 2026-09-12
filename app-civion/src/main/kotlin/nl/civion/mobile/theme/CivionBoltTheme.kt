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
 *  - **The accent is used sparingly.** Deep Violet carries `primary` only — selection, the compose
 *    action, CIVION's own actions. Secondary and tertiary roles are neutral, so the interface does
 *    not turn violet. `error` is a distinct orange-red, so an error never reads as an accent.
 *  - **The neutrals are the accepted CIVION surface system** (colour-system pass of 2026-09-12):
 *    a white light hierarchy and a layered dark ladder, with #DCD5EF / #403757 as the selected
 *    surface in the highest container role (violet-tinted, so a selected row is a visible mark;
 *    the cyan #E6FBFF / #193034 taken from the reference sat 1.08:1 from a read row). The classic
 *    theme in values/themes.xml names the same values, so both UI layers sit on one grey system.
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
    // Deep Violet (#6E56CF), the accepted CIVION Mail accent: carries white text and stays calm across a long list.
    primary = Color(color = 0xFF6E56CF),
    onPrimary = Color(color = 0xFFFFFFFF),
    primaryContainer = Color(color = 0xFF7A62DA),
    onPrimaryContainer = Color(color = 0xFFFFFFFF),

    // Neutral, so the accent stays rare: mid grey and the very light grey of the accepted scale.
    secondary = Color(color = 0xFF616161),
    onSecondary = Color(color = 0xFFFFFFFF),
    secondaryContainer = Color(color = 0xFFE0E0E0),
    onSecondaryContainer = Color(color = 0xFF171717),

    tertiary = Color(color = 0xFF2F4858),
    onTertiary = Color(color = 0xFFFFFFFF),
    tertiaryContainer = Color(color = 0xFF456073),
    onTertiaryContainer = Color(color = 0xFFFFFFFF),

    // Orange-red, so an error is never mistaken for the CIVION accent.
    error = Color(color = 0xFF8A3200),
    onError = Color(color = 0xFFFFFFFF),
    errorContainer = Color(color = 0xFFFFF1E8),
    onErrorContainer = Color(color = 0xFF8A3200),

    // The accepted light surface system: window white, list #F1F1F1, group header #F0F0F0, read
    // row #EEEEEE, selected #DCD5EF (the highest container is the selected surface, as in the
    // classic theme; Bolt derives surfaceVariant from it), text #171717 over #464646.
    surfaceDim = Color(color = 0xFFE0E0E0),
    surface = Color(color = 0xFFFFFFFF),
    surfaceBright = Color(color = 0xFFFFFFFF),
    onSurface = Color(color = 0xFF171717),
    onSurfaceVariant = Color(color = 0xFF464646),

    surfaceContainerLowest = Color(color = 0xFFFFFFFF),
    surfaceContainerLow = Color(color = 0xFFF1F1F1),
    surfaceContainer = Color(color = 0xFFF0F0F0),
    surfaceContainerHigh = Color(color = 0xFFEEEEEE),
    surfaceContainerHighest = Color(color = 0xFFDCD5EF),

    inverseSurface = Color(color = 0xFF212121),
    inverseOnSurface = Color(color = 0xFFE0E0E0),
    inversePrimary = Color(color = 0xFFC8BAFF),

    outline = Color(color = 0xFF464646),
    outlineVariant = Color(color = 0xFFE0E0E0),

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
    // Deep Violet has one identity and two rendered values (UI canon r002): #6E56CF measures
    // 2.99:1 on the #212121 window and cannot carry text or an icon there, so dark renders the
    // accent as #9B87F5 (5.50:1). onPrimary follows it down — white on #9B87F5 is 2.93:1, the
    // window graphite is 5.50:1. #6E56CF stays the inverse role and the product's identity colour.
    primary = Color(color = 0xFF9B87F5),
    onPrimary = Color(color = 0xFF212121),
    primaryContainer = Color(color = 0xFF5A45B2),
    onPrimaryContainer = Color(color = 0xFFE7DFFF),

    secondary = Color(color = 0xFFA6B7BF),
    onSecondary = Color(color = 0xFF212121),
    secondaryContainer = Color(color = 0xFF424242),
    onSecondaryContainer = Color(color = 0xFFFFFFFF),

    tertiary = Color(color = 0xFFA8CCE3),
    onTertiary = Color(color = 0xFF0C3243),
    tertiaryContainer = Color(color = 0xFF2A4A5C),
    onTertiaryContainer = Color(color = 0xFFCDE7F8),

    error = Color(color = 0xFFFFB59C),
    onError = Color(color = 0xFF551D00),
    errorContainer = Color(color = 0xFF6F2A00),
    onErrorContainer = Color(color = 0xFFFFDBCC),

    // The accepted dark ladder: #080808 / #101010 / #181818 under the #212121 window, message
    // row #323232, selected #403757; text #FFFFFF over #E0E0E0 over #BDBDBD.
    surfaceDim = Color(color = 0xFF151515),
    surface = Color(color = 0xFF212121),
    surfaceBright = Color(color = 0xFF323232),
    onSurface = Color(color = 0xFFFFFFFF),
    onSurfaceVariant = Color(color = 0xFFE0E0E0),

    surfaceContainerLowest = Color(color = 0xFF080808),
    surfaceContainerLow = Color(color = 0xFF101010),
    surfaceContainer = Color(color = 0xFF181818),
    surfaceContainerHigh = Color(color = 0xFF323232),
    surfaceContainerHighest = Color(color = 0xFF403757),

    inverseSurface = Color(color = 0xFFE0E0E0),
    inverseOnSurface = Color(color = 0xFF212121),
    inversePrimary = Color(color = 0xFFC8BAFF),

    outline = Color(color = 0xFFBDBDBD),
    outlineVariant = Color(color = 0xFF424242),

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
