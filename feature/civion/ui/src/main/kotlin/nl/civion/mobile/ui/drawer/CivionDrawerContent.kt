package nl.civion.mobile.ui.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextLabelSmall
import net.thunderbird.components.ui.bolt.theme.BoltTheme

private const val DRAWER_WIDTH_DP = 300
private const val ROW_HEIGHT_DP = 44
private const val ROW_CORNER_DP = 8
private const val INDENT_PER_LEVEL_DP = 16
private const val DRAGGED_ITEM_ALPHA = 0.9f
private const val MAX_SHOWN_COUNT = 999

/**
 * Android Mail's navigation drawer.
 *
 * The shape is Thunderbird's, because it is the right one for mail: the account you are in at the
 * top, its real folders below, and every other account one tap away behind the header. What is
 * different is the finish - one line per row, no avatar in front of an address, no capsule
 * highlight, and a graphite surface rather than near-black.
 *
 * The folder list is the account's own, rebuilt with its nesting intact. It is deliberately not a
 * curated set of global categories: a folder tree is what a mail account *is*, and replacing it
 * with a shorter list of clever destinations takes away the folders the user made and hides the
 * ones the server has.
 */
@Suppress("LongParameterList")
@Composable
internal fun CivionDrawerContent(
    state: CivionDrawerState,
    onAccountSelectorToggle: () -> Unit,
    onAllInboxesClick: () -> Unit,
    onAccountClick: (accountUuid: String) -> Unit,
    onAccountMove: (accountUuid: String, toPosition: Int) -> Unit,
    onAddAccountClick: () -> Unit,
    onFolderClick: (accountUuid: String, folderId: Long) -> Unit,
    onSyncAccountClick: () -> Unit,
    onManageFoldersClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Surface(
        color = BoltTheme.colors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxHeight()
            .width(DRAWER_WIDTH_DP.dp),
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = BoltTheme.spacings.half),
        ) {
            CurrentAccountHeader(
                account = state.selectedAccount,
                isUnified = state.isUnifiedSelected,
                isOpen = state.isAccountSelectorOpen,
                onClick = onAccountSelectorToggle,
                onSettingsClick = onSettingsClick,
            )

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (state.isAccountSelectorOpen) {
                    AccountSelector(
                        state = state,
                        onAllInboxesClick = onAllInboxesClick,
                        onAccountClick = onAccountClick,
                        onAccountMove = onAccountMove,
                        onAddAccountClick = onAddAccountClick,
                    )
                } else {
                    FolderTree(
                        state = state,
                        onFolderClick = onFolderClick,
                    )

                    if (state.selectedAccount != null) {
                        AccountActions(
                            onSyncAccountClick = onSyncAccountClick,
                            onManageFoldersClick = onManageFoldersClick,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The account you are in, on one line, with Settings beside it.
 *
 * No avatar in front of it: an address is already the thing that identifies an account, and a
 * generic circle before every one of them says nothing while taking the room the address needs.
 * A long address is cut with an ellipsis rather than wrapped, so the header keeps its height and
 * the folders below never move.
 *
 * Settings sits at the right end of the header, in the top area of the panel together with the
 * accounts, as accepted for CIVION Mail. It is its own tap; the rest of the row still opens the
 * account list.
 */
@Composable
private fun CurrentAccountHeader(
    account: DrawerAccount?,
    isUnified: Boolean,
    isOpen: Boolean,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(ROW_CORNER_DP.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = BoltTheme.spacings.default, vertical = BoltTheme.spacings.default),
        ) {
            TextBodyMedium(
                text = when {
                    isUnified -> "All Inboxes"
                    account != null -> account.email
                    else -> "No account"
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = BoltTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )

            Icon(
                imageVector = if (isOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                tint = BoltTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(BoltTheme.sizes.iconSmall),
            )
        }

        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Settings",
            tint = BoltTheme.colors.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(ROW_CORNER_DP.dp))
                .clickable(onClick = onSettingsClick)
                .padding(BoltTheme.spacings.default)
                .size(BoltTheme.sizes.iconSmall),
        )
    }
}

/**
 * Every account, one row each, in the order the user put them in.
 */
@Composable
private fun AccountSelector(
    state: CivionDrawerState,
    onAllInboxesClick: () -> Unit,
    onAccountClick: (accountUuid: String) -> Unit,
    onAccountMove: (accountUuid: String, toPosition: Int) -> Unit,
    onAddAccountClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dragState = remember { AccountDragState(state.accounts.map { it.uuid }) }
    dragState.adoptIfIdle(state.accounts.map { it.uuid })

    if (state.accounts.size > 1) {
        DrawerRow(
            label = "All Inboxes",
            icon = Icons.Outlined.AllInbox,
            selected = state.isUnifiedSelected,
            count = state.unifiedUnreadCount,
            onClick = onAllInboxesClick,
        )
    }

    dragState.orderedIds
        .mapNotNull { uuid -> state.accounts.firstOrNull { it.uuid == uuid } }
        .forEach { account ->
            DrawerRow(
                label = account.email,
                icon = null,
                selected = account.uuid == state.selectedAccountUuid && !state.isUnifiedSelected,
                count = account.unreadCount,
                onClick = { onAccountClick(account.uuid) },
                modifier = Modifier
                    .onSizeChanged { dragState.rowHeight = it.height.toFloat() }
                    .draggableAccount(account.uuid, dragState, haptics, onAccountMove),
            )
        }

    DrawerRow(
        label = "Add account",
        icon = Icons.Outlined.Add,
        selected = false,
        count = 0,
        onClick = onAddAccountClick,
    )
}

/**
 * The account's folders, as the server has them.
 */
@Composable
private fun FolderTree(
    state: CivionDrawerState,
    onFolderClick: (accountUuid: String, folderId: Long) -> Unit,
) {
    val account = state.selectedAccount ?: return

    account.folders.forEach { node ->
        FolderNode(
            node = node,
            level = 0,
            selectedFolderId = state.selectedFolderId,
            onFolderClick = { folderId -> onFolderClick(account.uuid, folderId) },
        )
    }
}

/**
 * One folder, and its children when it is open.
 *
 * A folder with children carries a chevron on the left; tapping that opens the branch, tapping
 * the row opens the folder. The two are separate because a parent folder usually holds mail of
 * its own, and collapsing it would otherwise be the only way to reach it.
 */
@Composable
private fun FolderNode(
    node: DrawerFolderNode,
    level: Int,
    selectedFolderId: Long?,
    onFolderClick: (folderId: Long) -> Unit,
) {
    var expanded by remember(node.label) { mutableStateOf(level == 0) }

    DrawerRow(
        label = node.label,
        iconRes = node.iconRes,
        selected = node.id != null && node.id == selectedFolderId,
        count = if (expanded) node.unreadCount else node.totalUnreadCount,
        indentLevel = level,
        expandState = if (node.hasChildren) expanded else null,
        onExpandToggle = { expanded = !expanded },
        onClick = { node.id?.let(onFolderClick) },
    )

    if (expanded) {
        node.children.forEach { child ->
            FolderNode(
                node = child,
                level = level + 1,
                selectedFolderId = selectedFolderId,
                onFolderClick = onFolderClick,
            )
        }
    }
}

/**
 * What the account itself can be told to do. Kept out of the folder list, and kept present:
 * these are the mail functions the drawer has always offered and there is no reason to lose them.
 * Settings is not among them: it lives in the header, with the accounts.
 */
@Composable
private fun AccountActions(
    onSyncAccountClick: () -> Unit,
    onManageFoldersClick: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = BoltTheme.spacings.double)) {
        DrawerRow(
            label = "Sync account",
            icon = Icons.Outlined.Sync,
            selected = false,
            count = 0,
            onClick = onSyncAccountClick,
        )
        DrawerRow(
            label = "Manage folders",
            icon = Icons.Outlined.FolderManaged,
            selected = false,
            count = 0,
            onClick = onManageFoldersClick,
        )
    }
}

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
private fun DrawerRow(
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
private fun Modifier.draggableAccount(
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
private class AccountDragState(initialIds: List<String>) {
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
