package nl.civion.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import nl.civion.mobile.ui.drawer.text3

/*
 * The frame every CIVION screen shares, measured from the mockup: a 56dp app bar on the window
 * surface with 48dp icon targets and a 20sp medium title, over a list surface.
 */
internal const val APP_BAR_HEIGHT_DP = 56
internal const val APP_BAR_EDGE_DP = 6
internal const val APP_BAR_ICON_TARGET_DP = 48
internal const val APP_BAR_TITLE_SP = 20
internal const val APP_BAR_TITLE_LINE_SP = 28
internal const val APP_BAR_TITLE_GAP_DP = 6
internal const val LIST_ROW_MIN_HEIGHT_DP = 56
internal const val LIST_ROW_EDGE_DP = 16
internal const val LIST_ROW_VERTICAL_DP = 10
internal const val LIST_ROW_GAP_DP = 16
internal const val LIST_ROW_TEXT_SP = 16
internal const val LIST_ROW_END_SP = 14
internal const val LIST_ROW_LINE_SP = 24

/** A whole screen: the window behind the system bars, the app bar, and the content under it. */
@Composable
internal fun CivionScreen(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    Surface(
        color = BoltTheme.colors.surface,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            AppBar(title = title, onBack = onBack, actions = actions)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BoltTheme.colors.surfaceContainer),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun AppBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(APP_BAR_HEIGHT_DP.dp)
            .padding(horizontal = APP_BAR_EDGE_DP.dp),
    ) {
        AppBarAction(icon = Icons.Outlined.ArrowBack, label = "Back", onClick = onBack)
        Spacer(modifier = Modifier.width(APP_BAR_TITLE_GAP_DP.dp))
        BasicText(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = BoltTheme.typography.bodyLarge.copy(
                color = BoltTheme.colors.onSurface,
                fontSize = APP_BAR_TITLE_SP.sp,
                lineHeight = APP_BAR_TITLE_LINE_SP.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

/** One icon on the app bar, its name being its content description. */
@Composable
internal fun AppBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = BoltTheme.colors.onSurfaceVariant,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(APP_BAR_ICON_TARGET_DP.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Icon(
            imageVector = icon,
            tint = tint,
            modifier = Modifier.size(BoltTheme.sizes.icon),
        )
    }
}

/**
 * One row of a plain list: a line of text, an optional icon before it and an optional value
 * after it, 56dp or taller, on the window surface with a hairline under it.
 */
@Composable
internal fun ListRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    end: String? = null,
    selected: Boolean = false,
    textColour: Color = BoltTheme.colors.onSurface,
    endColour: Color = text3(),
    iconColour: Color = BoltTheme.colors.onSurfaceVariant,
    iconSize: Dp = BoltTheme.sizes.icon,
    /** Extra space before the content, for a row that sits under another. */
    indent: Dp = 0.dp,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LIST_ROW_GAP_DP.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) BoltTheme.colors.surfaceContainerHighest else BoltTheme.colors.surface)
                .clickable(onClick = onClick)
                .heightIn(min = LIST_ROW_MIN_HEIGHT_DP.dp)
                .padding(
                    start = LIST_ROW_EDGE_DP.dp + indent,
                    end = LIST_ROW_EDGE_DP.dp,
                    top = LIST_ROW_VERTICAL_DP.dp,
                    bottom = LIST_ROW_VERTICAL_DP.dp,
                ),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    tint = iconColour,
                    modifier = Modifier.size(iconSize),
                )
            }
            BasicText(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = BoltTheme.typography.bodyLarge.copy(
                    color = textColour,
                    fontSize = LIST_ROW_TEXT_SP.sp,
                    lineHeight = LIST_ROW_LINE_SP.sp,
                ),
                modifier = Modifier.weight(1f),
            )
            if (end != null) {
                BasicText(
                    text = end,
                    style = BoltTheme.typography.bodyLarge.copy(
                        color = endColour,
                        fontSize = LIST_ROW_END_SP.sp,
                        lineHeight = LIST_ROW_LINE_SP.sp,
                        fontFeatureSettings = "tnum",
                    ),
                )
            }
        }
        Hairline()
    }
}

/** The 1dp rule between rows. */
@Composable
internal fun Hairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BoltTheme.colors.outlineVariant),
    )
}
