package nl.civion.mobile.ui.drawer

import androidx.annotation.DrawableRes

/**
 * What the drawer shows.
 *
 * Assembled from the engine's accounts and folders and then handed to the UI, so the Compose
 * layer never asks the engine anything itself.
 */
internal data class CivionDrawerState(
    val accounts: List<DrawerAccount> = emptyList(),
    val selectedAccountUuid: String? = null,
    val selectedFolderId: Long? = null,
    val isUnifiedSelected: Boolean = false,
    /** The folder tree is shown only after the user asks for it; the drawer opens with Inbox alone. */
    val isFoldersOpen: Boolean = false,
    val unifiedUnreadCount: Int = 0,
) {
    val selectedAccount: DrawerAccount?
        get() = accounts.firstOrNull { it.uuid == selectedAccountUuid }
}

/**
 * An account, as the drawer lists it.
 *
 * Only the address, because that is what the user picks an account by. A display name is
 * something typed once during setup and is usually either the address again or the user's own
 * name repeated on every row.
 */
internal data class DrawerAccount(
    val uuid: String,
    val email: String,
    val unreadCount: Int,
    /** The account's Inbox, which the drawer offers on its own; `null` until the folder list has loaded. */
    val inboxFolderId: Long?,
    val folders: List<DrawerFolderNode>,
) {
    /** What the avatar shows: the first letter of the address, which is how the user tells accounts apart. */
    val initial: String get() = email.trim().take(1).uppercase()
}

/**
 * A folder, and whatever is under it.
 *
 * The engine hands back a flat list whose names carry the server's own path, so the nesting is
 * rebuilt here rather than invented: what the drawer shows is the structure the account actually
 * has, including folders the user made themselves.
 *
 * [id] is null for a path segment that is not itself a folder - a server can have `Work/Clients`
 * without having `Work`. Such a node can be expanded but not opened.
 */
internal data class DrawerFolderNode(
    val id: Long?,
    val label: String,
    @param:DrawableRes val iconRes: Int,
    val unreadCount: Int,
    val children: List<DrawerFolderNode> = emptyList(),
) {
    val hasChildren: Boolean get() = children.isNotEmpty()

    /** Unread in this folder and everything under it, which is what a collapsed node must say. */
    val totalUnreadCount: Int get() = unreadCount + children.sumOf { it.totalUnreadCount }
}
