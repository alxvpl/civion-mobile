package nl.civion.mobile.ui.search

import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import net.thunderbird.feature.search.legacy.api.SearchCondition

/** What the user asked for on the Search screen (mockup screen 07): the words, and the filters. */
internal data class CivionSearchQuery(
    val text: String = "",
    /** Every folder of the account, or only the one the user is in. */
    val allFolders: Boolean = true,
    val withAttachment: Boolean = false,
    val unread: Boolean = false,
    val starred: Boolean = false,
) {
    val hasFilter: Boolean get() = withAttachment || unread || starred

    /**
     * The search as the engine runs it. The words match where upstream's own search matches
     * them - sender, recipients, subject, body - and the filters narrow that down; the scope is
     * the account, and its current folder unless all folders were asked for.
     */
    fun toLocalSearch(accountUuid: String?, folderId: Long?): LocalMessageSearch =
        LocalMessageSearch().apply {
            isManualSearch = true

            val words = text.trim()
            if (words.isNotEmpty()) {
                TEXT_FIELDS.forEach { field ->
                    or(SearchCondition(field, SearchAttribute.CONTAINS, words))
                }
            }
            if (withAttachment) and(MessageSearchField.ATTACHMENT_COUNT, "0", SearchAttribute.NOT_EQUALS)
            if (unread) and(MessageSearchField.READ, "0", SearchAttribute.EQUALS)
            if (starred) and(MessageSearchField.FLAGGED, "1", SearchAttribute.EQUALS)

            if (accountUuid != null) {
                addAccountUuid(accountUuid)
                if (!allFolders && folderId != null) addAllowedFolder(folderId)
            }
        }

    private companion object {
        val TEXT_FIELDS = listOf(
            MessageSearchField.SENDER,
            MessageSearchField.TO,
            MessageSearchField.CC,
            MessageSearchField.BCC,
            MessageSearchField.SUBJECT,
            MessageSearchField.MESSAGE_CONTENTS,
        )
    }
}
