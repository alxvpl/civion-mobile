package nl.civion.mobile.ui.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import nl.civion.mobile.ui.drawer.AccountDragState
import nl.civion.mobile.ui.drawer.CivionDrawerState
import nl.civion.mobile.ui.drawer.accentOnSelected
import nl.civion.mobile.ui.drawer.draggableAccount
import nl.civion.mobile.ui.screen.AppBarAction
import nl.civion.mobile.ui.screen.CivionScreen
import nl.civion.mobile.ui.screen.ListRow

/**
 * The list of accounts (mockup screen 03), opened from the drawer's Accounts action.
 *
 * All Inboxes leads when there is more than one account; under it every account by its address
 * and the count of its unread mail, in the order the user put them in - long-press and drag to
 * change it. The account the drawer is in is marked on the selected surface. Adding an account is
 * on the app bar, not in the list.
 */
@Composable
internal fun CivionAccountsScreen(
    state: CivionDrawerState,
    onBack: () -> Unit,
    onAllInboxesClick: () -> Unit,
    onAccountClick: (accountUuid: String) -> Unit,
    onAccountMove: (accountUuid: String, toPosition: Int) -> Unit,
    onAddAccountClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dragState = remember { AccountDragState(state.accounts.map { it.uuid }) }
    dragState.adoptIfIdle(state.accounts.map { it.uuid })

    CivionScreen(
        title = "Accounts",
        onBack = onBack,
        actions = {
            AppBarAction(icon = Icons.Outlined.Add, label = "Add account", onClick = onAddAccountClick)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.accounts.size > 1) {
                AccountRow(
                    text = "All Inboxes",
                    count = state.unifiedUnreadCount,
                    selected = state.isUnifiedSelected,
                    onClick = onAllInboxesClick,
                )
            }

            dragState.orderedIds
                .mapNotNull { uuid -> state.accounts.firstOrNull { it.uuid == uuid } }
                .forEach { account ->
                    AccountRow(
                        text = account.email,
                        count = account.unreadCount,
                        selected = account.uuid == state.selectedAccountUuid && !state.isUnifiedSelected,
                        onClick = { onAccountClick(account.uuid) },
                        modifier = Modifier
                            .onSizeChanged { dragState.rowHeight = it.height.toFloat() }
                            .draggableAccount(account.uuid, dragState, haptics, onAccountMove),
                    )
                }
        }
    }
}

@Composable
private fun AccountRow(
    text: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        ListRow(
            text = text,
            end = count.takeIf { it > 0 }?.toString(),
            selected = true,
            textColour = accentOnSelected(),
            endColour = accentOnSelected(),
            onClick = onClick,
            modifier = modifier,
        )
    } else {
        ListRow(
            text = text,
            end = count.takeIf { it > 0 }?.toString(),
            onClick = onClick,
            modifier = modifier,
        )
    }
}
