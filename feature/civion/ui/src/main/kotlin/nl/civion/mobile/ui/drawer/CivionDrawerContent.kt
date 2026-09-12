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
 * A navigation surface, not a folder tree. The top area holds the two things that are about the
 * application rather than about mail: the account (its avatar opens the list of accounts, and
 * that list is where a new one is added) and Options. Under MAIL there is the Inbox, which is
 * where mail is read, and a Folders row that unfolds the account's real folder tree only when the
 * user asks for it - the tree is the account's own, nested as the server nests it, and nothing of
 * it is lost; it is just not the first thing on the screen. Under SMART are the four CIVION
 * categories, present now so that the navigation has its shape before the classification behind
 * them exists.
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
            TopArea(
                account = state.selectedAccount,
                isUnified = state.isUnifiedSelected,
                isSelectorOpen = state.isAccountSelectorOpen,
                onAvatarClick = onAccountSelectorToggle,
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
        }
    }
}

/**
 * The top of the panel: the account on the left, Options on the right.
 *
 * The avatar is the account control - tapping it opens the list of accounts - and the address
 * beside it names the one the drawer is in. Options is its own target at the end of the row,
 * the same size as the avatar, so neither is a small secondary action hidden in the other.
 */
@Composable
private fun TopArea(
    account: DrawerAccount?,
    isUnified: Boolean,
    isSelectorOpen: Boolean,
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BoltTheme.spacings.half, vertical = BoltTheme.spacings.default),
    ) {
        AccountAvatar(
            initial = if (isUnified) "∗" else account?.initial ?: "?",
            selected = isSelectorOpen,
            onClick = onAvatarClick,
        )

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

        TopIconButton(
            icon = Icons.Outlined.Settings,
            contentDescription = "Options",
            onClick = onSettingsClick,
        )
    }
}

/**
 * A circle with the first letter of the address. It is the account switch: the whole circle is
 * the target, and it takes the accent while the account list it opens is showing.
 */
@Composable
private fun AccountAvatar(
    initial: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TOP_ICON_TARGET_DP.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Switch account" },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AVATAR_SIZE_DP.dp)
                .clip(CircleShape)
                .background(
                    if (selected) BoltTheme.colors.primary else BoltTheme.colors.primaryContainer,
                ),
        ) {
            TextTitleMedium(
                text = initial,
                color = if (selected) BoltTheme.colors.onPrimary else BoltTheme.colors.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun TopIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TOP_ICON_TARGET_DP.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = BoltTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(BoltTheme.sizes.icon),
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
        style = BoltTheme.typography.labelSmall.copy(
            color = BoltTheme.colors.onSurfaceVariant,
            letterSpacing = 1.5.sp,
        ),
        modifier = Modifier.padding(
            start = BoltTheme.spacings.default,
            top = BoltTheme.spacings.double,
            bottom = BoltTheme.spacings.half,
        ),
    )
}

/**
 * Every account, one row each, in the order the user put them in - the address and nothing else.
 * All Inboxes sits above them when there is more than one, and adding an account is the last row.
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

        DrawerRow(
            label = "Add account",
            icon = Icons.Outlined.Add,
            selected = false,
            count = 0,
            onClick = onAddAccountClick,
        )
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
