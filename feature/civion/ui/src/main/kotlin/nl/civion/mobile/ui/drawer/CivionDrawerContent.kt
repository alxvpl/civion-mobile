package nl.civion.mobile.ui.drawer

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextLabelSmall
import net.thunderbird.components.ui.bolt.organism.drawer.NavigationDrawerItem
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.mail.folder.api.FolderType

private const val DRAWER_WIDTH_DP = 320
private const val DRAGGED_ITEM_ALPHA = 0.85f

/**
 * Android Mail's navigation drawer.
 *
 * One list, read top to bottom: everything, then the accounts, then the ways of looking across
 * them, then the folders of whichever account is open, then settings. The upstream drawer this
 * replaces had two modes behind a toggle on the account header - accounts *or* folders - so
 * reaching a folder in another account meant switching mode, choosing the account, switching
 * back. Flat is fewer taps and, more to the point, the whole drawer is visible at once.
 *
 * What is deliberately absent: pinned, snoozed, scheduled, recently seen, appointments, travel,
 * packages, subscriptions, coupons, entertainment, contacts, help and marketing rows. A drawer
 * is for going somewhere, and every row that is not a destination makes the ones that are harder
 * to find.
 */
@Suppress("LongParameterList")
@Composable
internal fun CivionDrawerContent(
    state: CivionDrawerState,
    onAllInboxesClick: () -> Unit,
    onAccountClick: (accountUuid: String) -> Unit,
    onAccountMove: (accountUuid: String, toPosition: Int) -> Unit,
    onAddAccountClick: () -> Unit,
    onSmartDestinationClick: (SmartDestination) -> Unit,
    onFolderClick: (accountUuid: String, folderId: Long) -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BoltTheme.spacings.default),
        ) {
            NavigationDrawerItem(
                label = "All Inboxes",
                selected = state.isUnifiedSelected,
                onClick = onAllInboxesClick,
                icon = { Icon(imageVector = Icons.Outlined.AllInbox) },
                badge = { UnreadBadge(state.unifiedUnreadCount) },
            )

            AccountSection(
                state = state,
                onAccountClick = onAccountClick,
                onAccountMove = onAccountMove,
            )

            NavigationDrawerItem(
                label = "Add account",
                selected = false,
                onClick = onAddAccountClick,
                icon = { Icon(imageVector = Icons.Outlined.Add) },
            )

            DividerHorizontal(modifier = Modifier.padding(vertical = BoltTheme.spacings.default))

            SmartDestination.entries.forEach { destination ->
                NavigationDrawerItem(
                    label = destination.label(),
                    selected = state.selectedShortcut == destination,
                    onClick = { onSmartDestinationClick(destination) },
                    icon = { Icon(imageVector = destination.icon()) },
                )
            }

            FolderSection(
                state = state,
                onFolderClick = onFolderClick,
            )

            DividerHorizontal(modifier = Modifier.padding(vertical = BoltTheme.spacings.default))

            NavigationDrawerItem(
                label = "Settings",
                selected = false,
                onClick = onSettingsClick,
                icon = { Icon(imageVector = Icons.Outlined.Settings) },
            )
        }
    }
}

/**
 * The accounts, in the order the user put them in.
 *
 * A row is the address and nothing else. Account setup asks for a display name, and it is almost
 * always either the address again or the user's own name repeated down the list; the address is
 * the thing that tells two accounts apart.
 */
@Composable
private fun AccountSection(
    state: CivionDrawerState,
    onAccountClick: (accountUuid: String) -> Unit,
    onAccountMove: (accountUuid: String, toPosition: Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dragState = remember { AccountDragState(state.accounts.map { it.uuid }) }
    dragState.adoptIfIdle(state.accounts.map { it.uuid })

    val ordered = dragState.orderedIds.mapNotNull { uuid -> state.accounts.firstOrNull { it.uuid == uuid } }

    ordered.forEach { account ->
        NavigationDrawerItem(
            label = account.email,
            selected = account.uuid == state.selectedAccountUuid && !state.isUnifiedSelected,
            onClick = { onAccountClick(account.uuid) },
            icon = { Icon(imageVector = Icons.Outlined.AccountCircle) },
            badge = { UnreadBadge(account.unreadCount) },
            modifier = Modifier
                .onSizeChanged { dragState.rowHeight = it.height.toFloat() }
                .draggableAccount(
                    accountUuid = account.uuid,
                    dragState = dragState,
                    haptics = haptics,
                    onAccountMove = onAccountMove,
                ),
        )
    }
}

/**
 * The folders of the account that is open.
 *
 * Inbox is not repeated here: tapping the account itself opens it, which is what the account row
 * is for. Only the folders every account has are offered - the full list, including whatever the
 * user made themselves, lives behind Manage folders, because a drawer that lists two hundred
 * folders is a folder browser and stops being navigation.
 */
@Composable
private fun FolderSection(
    state: CivionDrawerState,
    onFolderClick: (accountUuid: String, folderId: Long) -> Unit,
) {
    val account = state.accounts.firstOrNull { it.uuid == state.selectedAccountUuid } ?: return
    val byType = account.folders.associateBy { it.type }

    val folders = DRAWER_FOLDER_ORDER
        .filter { it != FolderType.INBOX }
        .mapNotNull { byType[it] }

    if (folders.isEmpty()) return

    DividerHorizontal(modifier = Modifier.padding(vertical = BoltTheme.spacings.default))

    folders.forEach { folder ->
        NavigationDrawerItem(
            label = folder.name,
            selected = folder.id == state.selectedFolderId && account.uuid == state.selectedAccountUuid,
            onClick = { onFolderClick(account.uuid, folder.id) },
            icon = { Icon(imageVector = folder.type.icon()) },
            badge = { UnreadBadge(folder.unreadCount) },
        )
    }
}

/**
 * A count, or nothing.
 *
 * Zero is not shown. A row of zeroes is noise, and the absence of a number already says the
 * folder has nothing waiting.
 */
@Composable
private fun UnreadBadge(count: Int) {
    if (count <= 0) return

    TextLabelSmall(
        text = if (count > MAX_SHOWN_COUNT) "$MAX_SHOWN_COUNT+" else count.toString(),
        color = BoltTheme.colors.onSurfaceVariant,
    )
}

private const val MAX_SHOWN_COUNT = 999

private fun SmartDestination.label(): String = when (this) {
    SmartDestination.UNREAD -> "Unread"
    SmartDestination.FLAGGED -> "Flagged"
    SmartDestination.ATTACHMENTS -> "Attachments"
}

private fun SmartDestination.icon() = when (this) {
    SmartDestination.UNREAD -> Icons.Outlined.MarkEmailUnread
    SmartDestination.FLAGGED -> Icons.Outlined.Star
    SmartDestination.ATTACHMENTS -> Icons.Outlined.Attachment
}

private fun FolderType.icon() = when (this) {
    FolderType.INBOX -> Icons.Outlined.Inbox
    FolderType.DRAFTS -> Icons.Outlined.Drafts
    FolderType.SENT -> Icons.Outlined.Send
    FolderType.ARCHIVE -> Icons.Outlined.Archive
    FolderType.TRASH -> Icons.Outlined.Delete
    FolderType.SPAM -> Icons.Outlined.Report
    else -> Icons.Outlined.Folder
}

/**
 * Long-press and drag an account to reorder it.
 *
 * Carried over from the drawer this replaces, where it lived as a patch in an upstream file. The
 * behaviour is the one that was already accepted; what changed is that it is now in a file
 * Android Mail owns.
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
