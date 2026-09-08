package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayAccount

private const val DRAGGED_ITEM_ALPHA = 0.9f

/**
 * The accounts, in the order the user arranged them.
 *
 * A long press picks an account up; dragging it past its neighbours moves it, and lifting the
 * finger reports the new position to [onAccountMove]. Positions count real accounts only: when the
 * unified account is shown it stays at the top and can be neither picked up nor dropped past.
 *
 * While a drag is in progress only the *order* is held here. The accounts themselves are always
 * the ones just handed in, so unread counts and error badges keep updating under the finger.
 */
@Composable
internal fun AccountList(
    accounts: ImmutableList<DisplayAccount>,
    selectedAccount: DisplayAccount?,
    onAccountClick: (DisplayAccount) -> Unit,
    onAccountMove: (accountId: String, toPosition: Int) -> Unit,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val accountIds = accounts.map { it.id }

    val dragState = remember { AccountDragState(accountIds) }
    LaunchedEffect(accountIds) {
        dragState.adoptIfIdle(accountIds)
    }

    val accountsById = accounts.associateBy { it.id }
    val orderedAccounts = dragState.orderedIds
        .mapNotNull { accountsById[it] }
        .takeIf { it.size == accounts.size }
        ?: accounts

    val firstMovableIndex = orderedAccounts.indexOfFirst { it !is UnifiedDisplayAccount }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(vertical = BoltTheme.spacings.default),
    ) {
        items(
            items = orderedAccounts,
            key = { account -> account.id },
        ) { account ->
            AccountListItem(
                account = account,
                onClick = { onAccountClick(account) },
                selected = selectedAccount == account,
                showStarredCount = showStarredCount,
                modifier = Modifier.draggableAccount(
                    account = account,
                    dragState = dragState,
                    listState = listState,
                    firstMovableIndex = firstMovableIndex,
                    haptics = haptics,
                    onAccountMove = onAccountMove,
                ),
            )
        }
    }
}

/**
 * Lifts the account under a long press and lets it be dragged to a new position.
 *
 * The unified account is not draggable and gets no gesture handler at all.
 */
private fun Modifier.draggableAccount(
    account: DisplayAccount,
    dragState: AccountDragState,
    listState: LazyListState,
    firstMovableIndex: Int,
    haptics: HapticFeedback,
    onAccountMove: (accountId: String, toPosition: Int) -> Unit,
): Modifier {
    val isDragged = account.id == dragState.draggedId

    val lifted = this
        .zIndex(if (isDragged) 1f else 0f)
        .graphicsLayer { translationY = if (isDragged) dragState.offset else 0f }
        .alpha(if (isDragged) DRAGGED_ITEM_ALPHA else 1f)

    if (account is UnifiedDisplayAccount) return lifted

    // Keyed on the bound as well: the unified account appearing or disappearing shifts every
    // position, and the handler would otherwise keep using the bound it was installed with.
    return lifted.pointerInput(account.id, firstMovableIndex) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                dragState.start(account.id)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                dragState.drag(
                    distance = dragAmount.y,
                    rowHeight = listState.heightOf(account.id),
                    lowerBound = firstMovableIndex,
                )
            },
            onDragEnd = {
                dragState.finish(firstMovableIndex)?.let { position ->
                    onAccountMove(account.id, position)
                }
            },
            onDragCancel = { dragState.cancel() },
        )
    }
}

/**
 * The order of the accounts while one of them is being dragged.
 *
 * Kept in one object rather than in separate remembered values because the gesture handlers are
 * installed once per account and must keep seeing the current state, not the values captured when
 * they were installed.
 */
@Stable
private class AccountDragState(initialIds: List<String>) {
    var orderedIds by mutableStateOf(initialIds)
        private set
    var draggedId by mutableStateOf<String?>(null)
        private set
    var offset by mutableFloatStateOf(0f)
        private set

    private var idsBeforeDrag = initialIds
    private var startPosition = -1

    /** Takes the account list as it now is, unless the user is in the middle of a drag. */
    fun adoptIfIdle(accountIds: List<String>) {
        if (draggedId == null) {
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
    fun drag(distance: Float, rowHeight: Float, lowerBound: Int) {
        offset += distance

        val from = draggedId?.let { orderedIds.indexOf(it) } ?: -1
        if (rowHeight <= 0f || from == -1) return

        val to = (from + (offset / rowHeight).roundToInt())
            .coerceIn(lowerBound.coerceAtLeast(0), orderedIds.lastIndex)
        if (to != from) {
            orderedIds = orderedIds.reposition(from, to)
            offset -= (to - from) * rowHeight
        }
    }

    /**
     * Ends the drag and returns the account's new position among the real accounts, or `null` if
     * it ended up where it started.
     */
    fun finish(firstMovableIndex: Int): Int? {
        val index = draggedId?.let { orderedIds.indexOf(it) } ?: -1

        draggedId = null
        offset = 0f

        val moved = index >= 0 && index != startPosition && firstMovableIndex >= 0
        return if (moved) index - firstMovableIndex else null
    }

    fun cancel() {
        draggedId = null
        offset = 0f
        orderedIds = idsBeforeDrag
    }
}

private fun List<String>.reposition(from: Int, to: Int): List<String> =
    toMutableList().apply { add(to, removeAt(from)) }

private fun LazyListState.heightOf(key: Any): Float =
    layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.size?.toFloat() ?: 0f
