package nl.civion.mobile.ui.drawer

import app.k9mail.legacy.ui.folder.DisplayFolder
import app.k9mail.legacy.ui.folder.FolderIconProvider
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import net.thunderbird.feature.mail.folder.api.FolderType

/**
 * Rebuilds an account's folder tree from the flat list the engine returns.
 *
 * The engine gives folders with their full server path in the name - `Work`, `Work/Clients`,
 * `Work/Clients/2026` - and the delimiter that separates them. The nesting is therefore not
 * invented here; it is read back out of names the server chose, which is why what the drawer
 * shows is the account's own structure rather than an arrangement of ours.
 *
 * A path segment that has no folder of its own is kept as a node that can be opened but not
 * entered: a server may hold `Work/Clients` without holding `Work`, and dropping the branch would
 * hide the folder underneath it.
 */
internal class CivionFolderTree(
    private val nameFormatter: FolderNameFormatter,
    private val iconProvider: FolderIconProvider,
) {

    fun build(folders: List<DisplayFolder>): List<DrawerFolderNode> {
        val root = MutableNode(label = "")

        folders.sortedWith(FOLDER_ORDER).forEach { displayFolder ->
            val delimiter = displayFolder.pathDelimiter.takeIf { it.isNotEmpty() } ?: DEFAULT_DELIMITER
            val displayName = nameFormatter.displayName(displayFolder.folder)

            // Special folders keep their given name whole. Their display name is a translated
            // label rather than a server path, and splitting "Sent" on a delimiter that happens
            // to occur in it would file it under a branch that does not exist.
            val segments = if (displayFolder.folder.type == FolderType.REGULAR) {
                displayName.split(delimiter).filter { it.isNotBlank() }
            } else {
                listOf(displayName)
            }

            if (segments.isEmpty()) return@forEach

            var node = root
            segments.forEachIndexed { index, segment ->
                node = node.children.getOrPut(segment) { MutableNode(label = segment) }

                if (index == segments.lastIndex) {
                    node.id = displayFolder.folder.id
                    node.unreadCount = displayFolder.unreadMessageCount
                    node.iconRes = iconProvider.getFolderIcon(displayFolder.folder.type)
                }
            }
        }

        return root.children.values.map { it.toNode() }
    }

    private class MutableNode(val label: String) {
        var id: Long? = null
        var unreadCount: Int = 0
        var iconRes: Int? = null
        val children = linkedMapOf<String, MutableNode>()

        fun toNode(): DrawerFolderNode = DrawerFolderNode(
            id = id,
            label = label,
            iconRes = iconRes ?: FALLBACK_ICON,
            unreadCount = unreadCount,
            children = children.values.map { it.toNode() },
        )
    }

    private companion object {
        const val DEFAULT_DELIMITER = "/"

        /**
         * A path segment with no folder behind it still needs something in front of it, and a
         * plain folder is what it is.
         */
        val FALLBACK_ICON = FolderIconProvider().getFolderIcon(FolderType.REGULAR)

        /**
         * Inbox first, then the other folders the account was created with, then everything the
         * user made, alphabetically. `isInTopGroup` is the engine's own idea of what belongs at
         * the top and is honoured before any of it.
         */
        val TYPE_ORDER = listOf(
            FolderType.INBOX,
            FolderType.OUTBOX,
            FolderType.DRAFTS,
            FolderType.SENT,
            FolderType.ARCHIVE,
            FolderType.SPAM,
            FolderType.TRASH,
        )

        val FOLDER_ORDER = compareByDescending<DisplayFolder> { it.isInTopGroup }
            .thenBy { TYPE_ORDER.indexOf(it.folder.type).takeIf { index -> index >= 0 } ?: TYPE_ORDER.size }
            .thenBy { it.folder.name.lowercase() }
    }
}
