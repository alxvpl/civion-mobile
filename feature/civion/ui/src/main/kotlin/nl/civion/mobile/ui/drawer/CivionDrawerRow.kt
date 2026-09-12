package nl.civion.mobile.ui.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextLabelSmall
import net.thunderbird.components.ui.bolt.theme.BoltTheme

internal const val ROW_HEIGHT_DP = 44
internal const val ROW_CORNER_DP = 8
internal const val INDENT_PER_LEVEL_DP = 16
private const val DRAGGED_ITEM_ALPHA = 0.9f
private const val MAX_SHOWN_COUNT = 999

/**
 * One row of the drawer.
 *
 * Written here rather than taken from the shared components because those draw a Material
 * navigation item: a tall row with a large filled capsule behind the selected one. The selected
 * row here is a slightly lighter graphite with a soft corner - present when looked for, quiet
 * when not - and every row is the same compact height whether it is a folder, an account or an
 * action.
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
    iconRes: Int? = null,
    indentLevel: Int = 0,
    expandState: Boolean? = null,
    onExpandToggle: () -> Unit = {},
    trailingState: Boolean? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(ROW_CORNER_DP.dp))
            .background(if (selected) BoltTheme.colors.surfaceContainerHigh else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(
                PaddingValues(
                    start = BoltTheme.spacings.default + (indentLevel * INDENT_PER_LEVEL_DP).dp,
                    end = BoltTheme.spacings.default,
                ),
            ),
    ) {
        RowLeading(
            expandState = expandState,
            onExpandToggle = onExpandToggle,
            icon = icon,
            iconRes = iconRes,
            contentColour = rowContentColour(selected),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .size(height = ROW_HEIGHT_DP.dp, width = 0.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            TextBodyMedium(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = rowContentColour(selected),
            )
        }

        if (count > 0) {
            TextLabelSmall(
                text = if (count > MAX_SHOWN_COUNT) "$MAX_SHOWN_COUNT+" else count.toString(),
                color = BoltTheme.colors.onSurfaceVariant,
            )
        }

        if (trailingState != null) {
            Icon(
                imageVector = if (trailingState) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                tint = BoltTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(BoltTheme.sizes.iconSmall),
            )
        }
    }
}

/**
 * What sits before the label: the expand chevron of a folder that has children, and the icon.
 *
 * The chevron takes its own tap so that opening a branch and opening the folder are separate
 * actions - a parent folder usually holds mail of its own, and collapsing it would otherwise be
 * the only way to reach it.
 */
@Composable
private fun RowLeading(
    expandState: Boolean?,
    onExpandToggle: () -> Unit,
    icon: ImageVector?,
    iconRes: Int?,
    contentColour: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
    ) {
        if (expandState != null) {
            Icon(
                imageVector = if (expandState) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                tint = BoltTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .size(BoltTheme.sizes.iconSmall)
                    .clip(RoundedCornerShape(ROW_CORNER_DP.dp))
                    .clickable(onClick = onExpandToggle),
            )
        }

        when {
            iconRes != null -> Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColour),
                modifier = Modifier.size(BoltTheme.sizes.iconSmall),
            )

            icon != null -> Icon(
                imageVector = icon,
                tint = contentColour,
                modifier = Modifier.size(BoltTheme.sizes.iconSmall),
            )
        }
    }
}

/**
 * The selected row is marked by its background. Tinting the text as well would make the accent a
 * second, competing signal on a surface meant to stay calm.
 */
@Composable
private fun rowContentColour(selected: Boolean): Color =
    if (selected) BoltTheme.colors.onSurface else BoltTheme.colors.onSurfaceVariant

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
