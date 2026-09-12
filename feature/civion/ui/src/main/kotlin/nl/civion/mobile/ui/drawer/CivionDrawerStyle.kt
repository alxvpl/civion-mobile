package nl.civion.mobile.ui.drawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.thunderbird.components.ui.bolt.theme.BoltTheme

/*
 * The drawer's colours and text sizes beyond what the scheme names.
 *
 * Bolt hands the composition a colour scheme rather than a light/dark flag, so wherever the canon
 * gives a value per theme the active theme is read off the scheme's own window surface.
 */

/** Above this the scheme's window is a light surface; below it, a dark one. */
private const val LIGHT_SURFACE_LUMINANCE = 0.5f

/** The accent as it reads on the selected surface: #5B45B0 on #DCD5EF, #C9BFF2 on #403757. */
private val AccentOnSelectedLight = Color(color = 0xFF5B45B0)
private val AccentOnSelectedDark = Color(color = 0xFFC9BFF2)

/** The third text tone of the canon (counts, section labels): #616161 light, #BDBDBD dark. */
private val Text3Light = Color(color = 0xFF616161)
private val Text3Dark = Color(color = 0xFFBDBDBD)

@Composable
private fun isDarkWindow(): Boolean = BoltTheme.colors.surface.luminance() < LIGHT_SURFACE_LUMINANCE

/** The canon's third text tone. */
@Composable
internal fun text3(): Color = if (isDarkWindow()) Text3Dark else Text3Light

/**
 * The accent as it reads on the selected surface.
 *
 * `primary` is the accent on the drawer's own surface, not on a selected row. Over the accepted
 * selected surfaces it measures 3.80:1 (light) and 3.77:1 (dark), below the 4.5:1 that a drawer
 * row - 18sp regular, normal text - needs. UI canon r002 §2 therefore gives the accent a separate
 * foreground value there: #5B45B0 on #DCD5EF is 5.07:1, #C9BFF2 on #403757 is 6.42:1.
 */
@Composable
internal fun accentOnSelected(): Color = if (isDarkWindow()) AccentOnSelectedDark else AccentOnSelectedLight

/**
 * The drawer's primary row text: 18sp on a 24sp line, in the body face.
 *
 * Bolt's scale steps from bodyLarge (16sp) straight to titleLarge (22sp); the accepted drawer
 * correction of 2026-09-12 needs the row labels visibly larger than 16 without reaching a title
 * size, so this is the one size the drawer adds, derived from bodyLarge and scaling with the
 * user's font size like every other sp value.
 */
@Composable
internal fun drawerRowTextStyle(color: Color): TextStyle =
    BoltTheme.typography.bodyLarge.copy(
        color = color,
        fontSize = DRAWER_ROW_TEXT_SP.sp,
        lineHeight = DRAWER_ROW_LINE_SP.sp,
    )

/** A folder under Folders: 16sp on the same 24sp line. */
@Composable
internal fun subRowTextStyle(color: Color): TextStyle =
    BoltTheme.typography.bodyLarge.copy(
        color = color,
        fontSize = SUB_ROW_TEXT_SP.sp,
        lineHeight = DRAWER_ROW_LINE_SP.sp,
    )

/** The account's secondary action at the foot: 15sp, between Bolt's 14 and 16. */
@Composable
internal fun secondaryRowTextStyle(color: Color): TextStyle =
    BoltTheme.typography.bodyLarge.copy(
        color = color,
        fontSize = SECONDARY_ROW_TEXT_SP.sp,
        lineHeight = DRAWER_ROW_LINE_SP.sp,
    )

/** The unread count: 16sp medium, tabular so that counts line up from row to row. */
@Composable
internal fun countTextStyle(color: Color): TextStyle =
    BoltTheme.typography.bodyLarge.copy(
        color = color,
        fontSize = COUNT_TEXT_SP.sp,
        lineHeight = DRAWER_ROW_LINE_SP.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum",
    )

/** The label of a row, sized for what the row is. */
@Composable
internal fun rowLabelStyle(sub: Boolean, secondary: Boolean, color: Color): TextStyle =
    when {
        secondary -> secondaryRowTextStyle(color)
        sub -> subRowTextStyle(color)
        else -> drawerRowTextStyle(color)
    }
