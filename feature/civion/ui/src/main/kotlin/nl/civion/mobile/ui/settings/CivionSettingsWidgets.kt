package nl.civion.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import nl.civion.mobile.ui.drawer.accentOnSelected
import nl.civion.mobile.ui.drawer.text3
import nl.civion.mobile.ui.screen.Hairline
import nl.civion.mobile.ui.screen.LIST_ROW_EDGE_DP
import nl.civion.mobile.ui.screen.LIST_ROW_GAP_DP
import nl.civion.mobile.ui.screen.LIST_ROW_LINE_SP
import nl.civion.mobile.ui.screen.LIST_ROW_MIN_HEIGHT_DP
import nl.civion.mobile.ui.screen.LIST_ROW_TEXT_SP
import nl.civion.mobile.ui.screen.LIST_ROW_VERTICAL_DP

/*
 * The Settings screen's pieces, measured from the mockup: a group title in 12sp medium, tracked
 * 0.09em, in the accent - the one place outside a selected state where the accent is text; a
 * row of 16sp with a 13sp second line; a 38x20 switch that is the accent when on.
 */
private const val GROUP_TEXT_SP = 12
private const val GROUP_LINE_SP = 16
private const val GROUP_TRACKING_EM = 0.09f
private const val GROUP_TOP_DP = 18
private const val GROUP_BOTTOM_DP = 6
private const val SUBTITLE_TEXT_SP = 13
private const val SUBTITLE_LINE_SP = 18
private const val SWITCH_WIDTH_DP = 38
private const val SWITCH_HEIGHT_DP = 20
private const val SWITCH_THUMB_DP = 16
private const val SWITCH_INSET_DP = 2
private const val SWATCH_DP = 20
private const val DIALOG_CORNER_DP = 12
private const val DIALOG_TITLE_SP = 18
private const val DIALOG_TITLE_LINE_SP = 24
private const val DIALOG_EDGE_DP = 8

@Composable
internal fun SettingsGroup(title: String) {
    BasicText(
        text = title,
        style = BoltTheme.typography.bodyLarge.copy(
            color = BoltTheme.colors.primary,
            fontSize = GROUP_TEXT_SP.sp,
            lineHeight = GROUP_LINE_SP.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = GROUP_TRACKING_EM.em,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(BoltTheme.colors.surface)
            .padding(
                start = LIST_ROW_EDGE_DP.dp,
                end = LIST_ROW_EDGE_DP.dp,
                top = GROUP_TOP_DP.dp,
                bottom = GROUP_BOTTOM_DP.dp,
            ),
    )
}

/** A setting: its name, its value or explanation under it, and whatever acts on it at the end. */
@Composable
internal fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    end: @Composable () -> Unit = {},
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(BoltTheme.colors.surface)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = LIST_ROW_MIN_HEIGHT_DP.dp)
                .padding(horizontal = LIST_ROW_EDGE_DP.dp, vertical = LIST_ROW_VERTICAL_DP.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = BoltTheme.typography.bodyLarge.copy(
                        color = BoltTheme.colors.onSurface,
                        fontSize = LIST_ROW_TEXT_SP.sp,
                        lineHeight = LIST_ROW_LINE_SP.sp,
                    ),
                )
                if (subtitle != null) {
                    BasicText(
                        text = subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = BoltTheme.typography.bodyLarge.copy(
                            color = text3(),
                            fontSize = SUBTITLE_TEXT_SP.sp,
                            lineHeight = SUBTITLE_LINE_SP.sp,
                        ),
                    )
                }
            }
            Box(modifier = Modifier.padding(start = LIST_ROW_GAP_DP.dp)) {
                end()
            }
        }
        Hairline()
    }
}

/** The mockup's switch: a 38x20 track in the hairline colour, the accent when on, a white thumb. */
@Composable
internal fun CivionSwitch(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = Modifier
            .size(width = SWITCH_WIDTH_DP.dp, height = SWITCH_HEIGHT_DP.dp)
            .clip(CircleShape)
            .background(if (checked) BoltTheme.colors.primary else BoltTheme.colors.outlineVariant)
            .clickable { onCheckedChange(!checked) }
            .semantics { contentDescription = label }
            .padding(SWITCH_INSET_DP.dp),
    ) {
        Box(
            modifier = Modifier
                .size(SWITCH_THUMB_DP.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** A round sample of a colour, for the Accent row. */
@Composable
internal fun ColourSwatch(colour: Color) {
    Box(
        modifier = Modifier
            .size(SWATCH_DP.dp)
            .clip(CircleShape)
            .background(colour),
    )
}

/** The values a setting may take, each with the name it is shown by. */
@Immutable
internal class Choices<T>(val items: List<Pair<T, String>>)

/**
 * A choice among a few values, put up over the screen: the title, then one row per option, the
 * current one on the selected surface. Choosing closes it.
 */
@Composable
internal fun <T> ChoiceDialog(
    title: String,
    options: Choices<T>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = BoltTheme.colors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DIALOG_CORNER_DP.dp)),
        ) {
            Column(modifier = Modifier.padding(vertical = DIALOG_EDGE_DP.dp)) {
                BasicText(
                    text = title,
                    style = BoltTheme.typography.bodyLarge.copy(
                        color = BoltTheme.colors.onSurface,
                        fontSize = DIALOG_TITLE_SP.sp,
                        lineHeight = DIALOG_TITLE_LINE_SP.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.padding(
                        start = LIST_ROW_EDGE_DP.dp,
                        end = LIST_ROW_EDGE_DP.dp,
                        top = LIST_ROW_VERTICAL_DP.dp,
                        bottom = LIST_ROW_VERTICAL_DP.dp,
                    ),
                )
                options.items.forEach { (value, label) ->
                    val isSelected = value == selected
                    BasicText(
                        text = label,
                        style = BoltTheme.typography.bodyLarge.copy(
                            color = if (isSelected) accentOnSelected() else BoltTheme.colors.onSurface,
                            fontSize = LIST_ROW_TEXT_SP.sp,
                            lineHeight = LIST_ROW_LINE_SP.sp,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) BoltTheme.colors.surfaceContainerHighest else Color.Transparent)
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(horizontal = LIST_ROW_EDGE_DP.dp, vertical = LIST_ROW_VERTICAL_DP.dp),
                    )
                }
            }
        }
    }
}
