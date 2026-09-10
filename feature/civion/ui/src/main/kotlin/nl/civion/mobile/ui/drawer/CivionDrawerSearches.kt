package nl.civion.mobile.ui.drawer

import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute

/**
 * The searches behind the drawer's saved-search destinations.
 *
 * Built the same way the unified inbox is built - a `LocalMessageSearch` handed to the message
 * list - so these are ordinary message lists with everything a message list can do, not a
 * separate kind of screen with its own gaps.
 *
 * Each one is restricted to folders that take part in the unified view, which is what keeps
 * Trash, Spam and Drafts out of them. Unread mail sitting in Trash is not something to be shown
 * a count of.
 */
internal object CivionDrawerSearches {

    private const val UNIFIED = "1"
    private const val NOT_READ = "0"
    private const val IS_FLAGGED = "1"
    private const val NO_ATTACHMENTS = "0"

    fun forDestination(destination: SmartDestination): LocalMessageSearch = when (destination) {
        SmartDestination.UNREAD -> unread()
        SmartDestination.FLAGGED -> flagged()
        SmartDestination.ATTACHMENTS -> withAttachments()
    }

    /** Everything not yet read, wherever it is, as long as it counts towards the unified view. */
    private fun unread(): LocalMessageSearch = unifiedScope("civion_unread").apply {
        and(MessageSearchField.READ, NOT_READ, SearchAttribute.EQUALS)
    }

    /** What the user marked as needing to come back to. */
    private fun flagged(): LocalMessageSearch = unifiedScope("civion_flagged").apply {
        and(MessageSearchField.FLAGGED, IS_FLAGGED, SearchAttribute.EQUALS)
    }

    /**
     * Anything that arrived carrying a file.
     *
     * Expressed as "attachment count is not zero" rather than as a comparison, because the
     * search field is a stored count and not-equals is the one operator that needs no ordering.
     */
    private fun withAttachments(): LocalMessageSearch = unifiedScope("civion_attachments").apply {
        and(MessageSearchField.ATTACHMENT_COUNT, NO_ATTACHMENTS, SearchAttribute.NOT_EQUALS)
    }

    private fun unifiedScope(searchId: String): LocalMessageSearch = LocalMessageSearch().apply {
        id = searchId
        and(MessageSearchField.INTEGRATE, UNIFIED, SearchAttribute.EQUALS)
    }
}
