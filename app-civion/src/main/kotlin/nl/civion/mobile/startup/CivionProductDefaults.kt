package nl.civion.mobile.startup

import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

/**
 * The settings Android Mail ships with, rather than the ones the engine ships with.
 *
 * Upstream's defaults are K-9's and Thunderbird's, and a few of them decide how the product
 * looks the first time it is opened. They are ordinary preferences: fully implemented, exposed
 * in Settings, and the user may change any of them. All that is decided here is where they
 * start.
 *
 * A value is written only when the key is absent, so this runs once on a fresh install and never
 * again. Forcing them on every start would be a different thing entirely - a setting the user can
 * change and the application changes back.
 */
internal class CivionProductDefaults(
    private val storage: Storage,
    private val storageEditor: StorageEditor,
) {

    fun applyOnce() {
        var wrote = false

        DEFAULTS.forEach { (key, value) ->
            if (!storage.contains(key)) {
                storageEditor.putBoolean(key, value)
                wrote = true
            }
        }

        if (wrote) {
            storageEditor.commit()
        }
    }

    private companion object {
        /**
         * The message list reads sender first.
         *
         * Upstream starts with the subject on the first line and the sender folded into the
         * preview. A mail list is scanned for who wrote, then what about - the row is now in
         * that order. The engine already supports both and swaps the two lines itself; only the
         * starting value differs.
         */
        const val SENDER_ABOVE_SUBJECT = "messageListSenderAboveSubject"

        val DEFAULTS = mapOf(
            SENDER_ABOVE_SUBJECT to true,
        )
    }
}
