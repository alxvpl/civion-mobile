package nl.civion.mobile.ui.drawer

import net.thunderbird.feature.mail.folder.api.FolderType

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
    val selectedShortcut: SmartDestination? = null,
    val unifiedUnreadCount: Int = 0,
)

/**
 * An account, as the drawer lists it.
 *
 * Only the address, because that is what the user picks an account by. A display name is
 * something the user typed once during setup and is usually either the same as the address or
 * their own name repeated on every row.
 */
internal data class DrawerAccount(
    val uuid: String,
    val email: String,
    val unreadCount: Int,
    val folders: List<DrawerFolder>,
)

internal data class DrawerFolder(
    val id: Long,
    val name: String,
    val type: FolderType,
    val unreadCount: Int,
)

/**
 * The destinations that are a saved search rather than a folder.
 *
 * Each one is a real query across every account, run by the same search the unified inbox uses.
 * They are here because they are how mail is actually looked for - what is unread, what needs
 * acting on, what carried a file - and none of them requires anything the engine does not
 * already index.
 */
internal enum class SmartDestination {
    UNREAD,
    FLAGGED,
    ATTACHMENTS,
}

/**
 * The per-account folders the drawer offers, in the order it offers them.
 *
 * A mail account can have hundreds of folders; the drawer is not a folder browser, and "Manage
 * folders" is where the full list lives. These are the ones every account has and every user
 * reaches for.
 */
internal val DRAWER_FOLDER_ORDER: List<FolderType> = listOf(
    FolderType.INBOX,
    FolderType.DRAFTS,
    FolderType.SENT,
    FolderType.ARCHIVE,
    FolderType.TRASH,
    FolderType.SPAM,
)
