package nl.civion.mobile.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import androidx.compose.material.icons.Icons as MaterialIcons

/*
 * The drawer's measurements are the mockup's (civion-mail-mockup, screens 01-02), not Bolt's
 * spacing scale: a primary row is 48dp with 18dp to the drawer edge, a 24dp icon and 16dp to
 * the label; the selected row is a pill open on the left, rounded 24dp on the right and stopping
 * 12dp short of the edge; folder rows under Folders are 44dp, 16sp, and start at 58dp - the
 * label keyline of the row above - with a 22dp slot for the chevron and 22dp more per level.
 */
internal const val ROW_HEIGHT_DP = 48
internal const val SUB_ROW_HEIGHT_DP = 44
internal const val DRAWER_ROW_TEXT_SP = 18
internal const val DRAWER_ROW_LINE_SP = 24
internal const val SUB_ROW_TEXT_SP = 16
internal const val SECONDARY_ROW_TEXT_SP = 15
internal const val COUNT_TEXT_SP = 16
internal const val ROW_EDGE_DP = 18
internal const val ROW_ICON_DP = 24
internal const val ROW_GAP_DP = 16
internal const val SELECTED_END_MARGIN_DP = 12
internal const val SELECTED_CORNER_DP = 24
internal const val TRAILING_ICON_DP = 20
internal const val SUB_ROW_START_DP = 58
internal const val SUB_ROW_SLOT_DP = 22
internal const val SUB_ROW_ICON_DP = 18
private const val DRAGGED_ITEM_ALPHA = 0.9f
private const val MAX_SHOWN_COUNT = 999

/**
 * One row of the drawer.
 *
 * Written here rather than taken from the shared components because those draw a Material
 * navigation item: a tall row with a large filled capsule behind the selected one. The selected
 * row here is the accepted selected surface as a pill open on the drawer's left edge, with the
 * accent on its text, icon and count. A row at [indentLevel] 0 is a primary destination; under
 * Folders the rows are the account's tree, lower and in the smaller type, without an icon.
 */
@Suppress("LongParameterList")
@Composable
internal fun DrawerRow(
    label: String,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    indentLevel: Int = 0,
    expandState: Boolean? = null,
    onExpandToggle: () -> Unit = {},
    trailingState: Boolean? = null,
    /** A marker of what the navigation will hold rather than a place in it: in the second text tone. */
    muted: Boolean = false,
    /** Drawn with the accent: the row that manages rather than navigates. */
    accented: Boolean = false,
    /** An action about the account rather than a place in it: lower, smaller, set to the right. */
    secondary: Boolean = false,
) {
    val look = rowLook(
        selected = selected,
        indentLevel = indentLevel,
        muted = muted,
        accented = accented,
        secondary = secondary,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (secondary) Arrangement.End else Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(end = SELECTED_END_MARGIN_DP.dp)
            .clip(RoundedCornerShape(topEnd = SELECTED_CORNER_DP.dp, bottomEnd = SELECTED_CORNER_DP.dp))
            .background(if (selected) BoltTheme.colors.surfaceContainerHighest else Color.Transparent)
            .clickable(onClick = onClick)
            .height(look.height)
            .padding(start = look.startPadding, end = (ROW_EDGE_DP - SELECTED_END_MARGIN_DP).dp),
    ) {
        if (look.sub) {
            SubRowSlot(
                expandState = expandState,
                onExpandToggle = onExpandToggle,
                icon = icon,
                colour = look.contentColour,
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                tint = look.contentColour,
                modifier = Modifier.size(if (secondary) TRAILING_ICON_DP.dp else ROW_ICON_DP.dp),
            )
            Spacer(modifier = Modifier.width(ROW_GAP_DP.dp))
        }

        BasicText(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = look.labelStyle,
            modifier = if (secondary) Modifier else Modifier.weight(1f),
        )

        RowTrailing(
            count = count,
            countColour = if (selected) look.contentColour else text3(),
            trailingState = trailingState,
        )
    }
}

/** What a row looks like, decided once from what it is. */
private class RowLook(
    val sub: Boolean,
    val height: Dp,
    val startPadding: Dp,
    val contentColour: Color,
    val labelStyle: TextStyle,
)

@Composable
private fun rowLook(
    selected: Boolean,
    indentLevel: Int,
    muted: Boolean,
    accented: Boolean,
    secondary: Boolean,
): RowLook {
    val sub = indentLevel > 0
    val small = sub || secondary
    val contentColour = when {
        selected -> accentOnSelected()
        accented -> BoltTheme.colors.primary
        else -> BoltTheme.colors.onSurfaceVariant
    }
    // A plain destination reads in the first text tone; everything else takes the row's colour.
    val plain = !selected && !accented && !muted && !small
    val labelColour = if (plain) BoltTheme.colors.onSurface else contentColour

    return RowLook(
        sub = sub,
        height = (if (small) SUB_ROW_HEIGHT_DP else ROW_HEIGHT_DP).dp,
        startPadding = if (sub) (SUB_ROW_START_DP + (indentLevel - 1) * SUB_ROW_SLOT_DP).dp else ROW_EDGE_DP.dp,
        contentColour = contentColour,
        labelStyle = rowLabelStyle(sub = sub, secondary = secondary, color = labelColour),
    )
}

/** After the label: the unread count, and the chevron of the row that unfolds Folders. */
@Composable
private fun RowTrailing(
    count: Int,
    countColour: Color,
    trailingState: Boolean?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (count > 0) {
            BasicText(
                text = if (count > MAX_SHOWN_COUNT) "$MAX_SHOWN_COUNT+" else count.toString(),
                style = countTextStyle(countColour),
                modifier = Modifier.padding(start = ROW_GAP_DP.dp),
            )
        }

        if (trailingState != null) {
            Icon(
                imageVector = if (trailingState) Icons.Outlined.ExpandMore else MaterialIcons.Outlined.ChevronRight,
                tint = BoltTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = ROW_GAP_DP.dp)
                    .size(TRAILING_ICON_DP.dp),
            )
        }
    }
}

/**
 * The 22dp slot before a folder row's label: the chevron of a folder that has children, the icon
 * of Manage folders, or nothing - so that every label under Folders sits on one keyline.
 *
 * The chevron takes its own tap so that opening a branch and opening the folder are separate
 * actions - a parent folder usually holds mail of its own, and collapsing it would otherwise be
 * the only way to reach it.
 */
@Composable
private fun SubRowSlot(
    expandState: Boolean?,
    onExpandToggle: () -> Unit,
    icon: ImageVector?,
    colour: Color,
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier.width(SUB_ROW_SLOT_DP.dp),
    ) {
        when {
            expandState != null -> Icon(
                imageVector = if (expandState) Icons.Outlined.ExpandMore else MaterialIcons.Outlined.ChevronRight,
                tint = colour,
                modifier = Modifier
                    .size(SUB_ROW_ICON_DP.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onExpandToggle),
            )

            icon != null -> Icon(
                imageVector = icon,
                tint = colour,
                modifier = Modifier.size(SUB_ROW_ICON_DP.dp),
            )
        }
    }
}

/**
 * Long-press and drag an account to reorder it.
 *
 * The order is the account manager's own, which is where it was already stored, so it survives a
 * restart without anything here keeping a second copy of it.
 */
internal fun Modifier.draggableAccount(
    accountUuid: String,
    dragState: AccountDragState,
    haptics: HapticFeedback,
    onAccountMove: (accountUuid: String, toPosition: Int) -> Unit,
): Modifier {
    val isDragged = accountUuid == dragState.draggedId

    return this
        .zIndex(if (isDragged) 1f else 0f)
        .graphicsLayer { translationY = if (isDragged) dragState.offset else 0f }
        .alpha(if (isDragged) DRAGGED_ITEM_ALPHA else 1f)
        .pointerInput(accountUuid) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    dragState.start(accountUuid)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragState.drag(dragAmount.y)
                },
                onDragEnd = { dragState.finish()?.let { onAccountMove(accountUuid, it) } },
                onDragCancel = { dragState.cancel() },
            )
        }
}

/**
 * The order of the accounts while one of them is being dragged.
 *
 * Held in one object rather than in separate remembered values because the gesture handlers are
 * installed once per account and must keep seeing the current state, not the values captured
 * when they were installed.
 */
@Stable
internal class AccountDragState(initialIds: List<String>) {
    var orderedIds by mutableStateOf(initialIds)
        private set
    var draggedId by mutableStateOf<String?>(null)
        private set
    var offset by mutableFloatStateOf(0f)
        private set
    var rowHeight: Float = 0f

    private var idsBeforeDrag = initialIds
    private var startPosition = -1

    /** Takes the account list as it now is, unless the user is in the middle of a drag. */
    fun adoptIfIdle(accountIds: List<String>) {
        if (draggedId == null && accountIds != orderedIds) {
            orderedIds = accountIds
            idsBeforeDrag = accountIds
        }
    }

    fun start(accountId: String) {
        draggedId = accountId
        offset = 0f
        idsBeforeDrag = orderedIds
        startPosition = orderedIds.indexOf(accountId)
    }

    /**
     * Follows the finger, and swaps the dragged account with a neighbour once it has travelled a
     * whole row. The travelled row is then taken off the offset, so the account stays under the
     * finger while the rest of the list closes up behind it.
     */
    fun drag(distance: Float) {
        offset += distance

        val from = draggedId?.let { orderedIds.indexOf(it) } ?: -1
        if (rowHeight <= 0f || from == -1) return

        val to = (from + (offset / rowHeight).roundToInt()).coerceIn(0, orderedIds.lastIndex)
        if (to != from) {
            orderedIds = orderedIds.reposition(from, to)
            offset -= (to - from) * rowHeight
        }
    }

    /** Ends the drag and returns the new position, or `null` if it ended where it started. */
    fun finish(): Int? {
        val index = draggedId?.let { orderedIds.indexOf(it) } ?: -1

        draggedId = null
        offset = 0f

        return index.takeIf { it >= 0 && it != startPosition }
    }

    fun cancel() {
        draggedId = null
        offset = 0f
        orderedIds = idsBeforeDrag
    }
}

private fun List<String>.reposition(from: Int, to: Int): List<String> =
    toMutableList().apply { add(to, removeAt(from)) }
