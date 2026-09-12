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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ReceiptLong
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextLabelSmall
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import androidx.compose.material.icons.Icons as MaterialIcons

private const val DRAWER_WIDTH_FRACTION = 0.85f
private const val DRAWER_MAX_WIDTH_DP = 400
private const val AVATAR_SIZE_DP = 40
private const val TOP_ICON_TARGET_DP = 48

/**
 * CIVION Mail's navigation drawer.
 *
 * A navigation surface, not a folder tree. At the top is an action panel with the three things
 * that are about the application rather than about mail - Accounts, Add account, Settings - and
 * under it the address of the account the drawer is in, as context. Under MAIL there is the
 * Inbox, which is where mail is read, and a Folders row that unfolds the account's real folder
 * tree only when the user asks for it - the tree is the account's own, nested as the server nests
 * it, and nothing of it is lost; it is just not the first thing on the screen. Under SMART are the
 * four CIVION categories, present now so that the navigation has its shape before the
 * classification behind them exists. At the bottom, set apart and set small, are the account's
 * secondary actions.
 *
 * The surface is graphite in both themes: the drawer is a panel beside the mail, not part of it.
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
    onFoldersToggle: () -> Unit,
    onFolderClick: (accountUuid: String, folderId: Long) -> Unit,
    onManageFoldersClick: () -> Unit,
    onSyncAccountClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val drawerWidth = (screenWidth * DRAWER_WIDTH_FRACTION).dp.coerceAtMost(DRAWER_MAX_WIDTH_DP.dp)

    Surface(
        color = BoltTheme.colors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxHeight()
            .width(drawerWidth),
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = BoltTheme.spacings.half),
        ) {
            ActionPanel(
                isSelectorOpen = state.isAccountSelectorOpen,
                onAccountsClick = onAccountSelectorToggle,
                onAddAccountClick = onAddAccountClick,
                onSettingsClick = onSettingsClick,
            )

            CurrentAccountRow(
                account = state.selectedAccount,
                isUnified = state.isUnifiedSelected,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (state.isAccountSelectorOpen) {
                    AccountSelector(
                        state = state,
                        onAllInboxesClick = onAllInboxesClick,
                        onAccountClick = onAccountClick,
                        onAccountMove = onAccountMove,
                    )
                } else {
                    MailSection(
                        state = state,
                        onAllInboxesClick = onAllInboxesClick,
                        onFoldersToggle = onFoldersToggle,
                        onFolderClick = onFolderClick,
                        onManageFoldersClick = onManageFoldersClick,
                    )

                    SmartSection()
                }
            }

            if (state.selectedAccount != null && !state.isUnifiedSelected && !state.isAccountSelectorOpen) {
                SecondaryActions(onSyncAccountClick = onSyncAccountClick)
            }
        }
    }
}

/**
 * The top of the panel: three actions on a band of their own - Accounts, Add account, Settings.
 *
 * Each is an icon alone, its name being its content description, equal in width and 48dp
 * tall, so none reads as the incidental control of another and the band stays one row high.
 * Accounts opens the list of accounts and is marked while that list is showing.
 */
@Composable
private fun ActionPanel(
    isSelectorOpen: Boolean,
    onAccountsClick: () -> Unit,
    onAddAccountClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = BoltTheme.spacings.half)
            .clip(RoundedCornerShape(ROW_CORNER_DP.dp))
            .background(BoltTheme.colors.surfaceContainer),
    ) {
        PanelAction(
            icon = Icons.Outlined.Group,
            label = "Accounts",
            active = isSelectorOpen,
            onClick = onAccountsClick,
            modifier = Modifier.weight(1f),
        )
        PanelAction(
            icon = Icons.Outlined.Add,
            label = "Add account",
            active = false,
            onClick = onAddAccountClick,
            modifier = Modifier.weight(1f),
        )
        PanelAction(
            icon = Icons.Outlined.Settings,
            label = "Settings",
            active = false,
            onClick = onSettingsClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PanelAction(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colour = if (active) BoltTheme.colors.primary else BoltTheme.colors.onSurfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(TOP_ICON_TARGET_DP.dp)
            .clip(RoundedCornerShape(ROW_CORNER_DP.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Icon(
            imageVector = icon,
            tint = colour,
            modifier = Modifier.size(BoltTheme.sizes.icon),
        )
    }
}

/**
 * The account the drawer is in, named by its address and nothing else: context for the rows
 * below, not a control. A long address is cut with an ellipsis rather than wrapped.
 */
@Composable
private fun CurrentAccountRow(
    account: DrawerAccount?,
    isUnified: Boolean,
) {
    TextTitleMedium(
        text = when {
            isUnified -> "All Inboxes"
            account != null -> account.email
            else -> "No account"
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = BoltTheme.colors.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BoltTheme.spacings.default, vertical = BoltTheme.spacings.default),
    )
}

/**
 * The bottom of the panel: what the account itself can be told to do. Set apart by a line and
 * set in the smaller type, so it is found when looked for and never read as a destination.
 */
@Composable
private fun SecondaryActions(onSyncAccountClick: () -> Unit) {
    Column(modifier = Modifier.padding(bottom = BoltTheme.spacings.half)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BoltTheme.spacings.default, vertical = BoltTheme.spacings.half)
                .height(1.dp)
                .background(BoltTheme.colors.outlineVariant),
        )
        DrawerRow(
            label = "Sync account",
            icon = Icons.Outlined.Sync,
            selected = false,
            count = 0,
            secondary = true,
            onClick = onSyncAccountClick,
        )
    }
}

/**
 * A section label: small capitals, spaced, in the secondary colour. It names the group under
 * it and is not a heading to be read on its own.
 */
@Composable
private fun SectionLabel(text: String) {
    BasicText(
        text = text,
        style = BoltTheme.typography.labelMedium.copy(
            color = BoltTheme.colors.onSurfaceVariant,
            letterSpacing = 1.5.sp,
        ),
        modifier = Modifier.padding(
            start = BoltTheme.spacings.default,
            top = BoltTheme.spacings.oneHalf,
            bottom = BoltTheme.spacings.quarter,
        ),
    )
}

/**
 * Every account, one row each, in the order the user put them in - the address and nothing else.
 * All Inboxes sits above them when there is more than one. Adding an account is on the panel above.
 */
@Composable
private fun AccountSelector(
    state: CivionDrawerState,
    onAllInboxesClick: () -> Unit,
    onAccountClick: (accountUuid: String) -> Unit,
    onAccountMove: (accountUuid: String, toPosition: Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dragState = remember { AccountDragState(state.accounts.map { it.uuid }) }
    dragState.adoptIfIdle(state.accounts.map { it.uuid })

    Column {
        SectionLabel(text = "ACCOUNTS")

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
    }
}

/**
 * MAIL: the Inbox, and the folders behind a row of their own.
 *
 * The Inbox row is the account's Inbox - or every account's, when All Inboxes is what the user
 * is in. The Folders row unfolds the account's tree without the Inbox in it, since the Inbox is
 * already the row above; subfolders of the Inbox keep their place. Manage folders closes the
 * unfolded block, because that is where a user who is looking at the folders will want it.
 */
@Composable
private fun MailSection(
    state: CivionDrawerState,
    onAllInboxesClick: () -> Unit,
    onFoldersToggle: () -> Unit,
    onFolderClick: (accountUuid: String, folderId: Long) -> Unit,
    onManageFoldersClick: () -> Unit,
) {
    val account = state.selectedAccount

    Column {
        SectionLabel(text = "MAIL")

        DrawerRow(
            label = "Inbox",
            icon = Icons.Outlined.Inbox,
            selected = state.isUnifiedSelected ||
                (account?.inboxFolderId != null && state.selectedFolderId == account.inboxFolderId),
            count = if (state.isUnifiedSelected) state.unifiedUnreadCount else account?.unreadCount ?: 0,
            onClick = {
                when {
                    state.isUnifiedSelected -> onAllInboxesClick()
                    account?.inboxFolderId != null -> onFolderClick(account.uuid, account.inboxFolderId)
                }
            },
        )

        // Folders belong to one account. Under All Inboxes there is no one account, so the row
        // is not offered; choosing an account brings it back.
        if (account != null && !state.isUnifiedSelected) {
            DrawerRow(
                label = "Folders",
                icon = Icons.Outlined.Folder,
                selected = false,
                count = 0,
                trailingState = state.isFoldersOpen,
                onClick = onFoldersToggle,
            )

            if (state.isFoldersOpen) {
                account.folders
                    .flatMap { node -> if (node.id == account.inboxFolderId) node.children else listOf(node) }
                    .forEach { node ->
                        FolderNode(
                            node = node,
                            level = 1,
                            selectedFolderId = state.selectedFolderId,
                            onFolderClick = { folderId -> onFolderClick(account.uuid, folderId) },
                        )
                    }

                DrawerRow(
                    label = "Manage folders",
                    icon = Icons.Outlined.FolderManaged,
                    selected = false,
                    count = 0,
                    indentLevel = 1,
                    onClick = onManageFoldersClick,
                )
            }
        }
    }
}

/**
 * SMART: the four CIVION categories, in the accepted order. They are markers of what the
 * navigation will hold, not folders, and nothing happens behind them yet.
 */
@Composable
private fun SmartSection() {
    Column {
        SectionLabel(text = "SMART")

        SmartRow(label = "Invoices", icon = MaterialIcons.Outlined.ReceiptLong)
        SmartRow(label = "Receipts", icon = MaterialIcons.Outlined.Receipt)
        SmartRow(label = "Contracts", icon = MaterialIcons.Outlined.Description)
        SmartRow(label = "Payments", icon = MaterialIcons.Outlined.Payments)

        Spacer(modifier = Modifier.height(BoltTheme.spacings.double))
    }
}

@Composable
private fun SmartRow(label: String, icon: ImageVector) {
    DrawerRow(
        label = label,
        icon = icon,
        selected = false,
        count = 0,
        onClick = {},
    )
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
    var expanded by remember(node.label) { mutableStateOf(level <= 1) }

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
