package nl.civion.mobile.ui.folders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import nl.civion.mobile.ui.drawer.CivionDrawerState
import nl.civion.mobile.ui.drawer.DrawerFolderNode
import nl.civion.mobile.ui.drawer.accentOnSelected
import nl.civion.mobile.ui.drawer.text3
import nl.civion.mobile.ui.screen.CivionScreen
import nl.civion.mobile.ui.screen.ListRow

/** Each level under the top one moves the row in by this much; the rest is the row's own edge. */
private const val INDENT_PER_LEVEL_DP = 22
private const val FOLDER_ICON_DP = 20

/**
 * The account's folders on a screen of their own (mockup screen 08), reached from Manage folders
 * in the drawer. The whole tree but the Inbox, always unfolded, nesting shown by indent; the count
 * only where there is unread mail; the folder the user is in on the selected surface. Manage
 * folders at the foot leads to upstream's editing of the folders themselves.
 */
@Composable
internal fun CivionFoldersScreen(
    state: CivionDrawerState,
    onBack: () -> Unit,
    onFolderClick: (folderId: Long) -> Unit,
    onManageFoldersClick: () -> Unit,
) {
    val account = state.selectedAccount

    CivionScreen(title = "Folders", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            // The Inbox is the drawer's own row under MAIL and is not repeated here; what is
            // under it keeps its place.
            account?.folders.orEmpty()
                .flatMap { node -> if (node.id == account?.inboxFolderId) node.children else listOf(node) }
                .forEach { node ->
                    FolderRows(
                        node = node,
                        depth = 0,
                        selectedFolderId = state.selectedFolderId,
                        onFolderClick = onFolderClick,
                    )
                }

            ListRow(
                text = "Manage folders",
                icon = Icons.Outlined.Settings,
                iconSize = FOLDER_ICON_DP.dp,
                textColour = BoltTheme.colors.primary,
                iconColour = BoltTheme.colors.primary,
                onClick = onManageFoldersClick,
            )
        }
    }
}

@Composable
private fun FolderRows(
    node: DrawerFolderNode,
    depth: Int,
    selectedFolderId: Long?,
    onFolderClick: (folderId: Long) -> Unit,
) {
    val selected = node.id != null && node.id == selectedFolderId

    ListRow(
        text = node.label,
        icon = Icons.Outlined.Folder,
        iconSize = FOLDER_ICON_DP.dp,
        end = node.unreadCount.takeIf { it > 0 }?.toString(),
        selected = selected,
        textColour = if (selected) accentOnSelected() else BoltTheme.colors.onSurface,
        endColour = if (selected) accentOnSelected() else text3(),
        indent = (depth * INDENT_PER_LEVEL_DP).dp,
        onClick = { node.id?.let(onFolderClick) },
    )

    node.children.forEach { child ->
        FolderRows(
            node = child,
            depth = depth + 1,
            selectedFolderId = selectedFolderId,
            onFolderClick = onFolderClick,
        )
    }
}
