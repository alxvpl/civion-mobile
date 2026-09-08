package nl.civion.mobile.navigation

import android.content.Context
import android.content.SharedPreferences

/**
 * Where the user was.
 *
 * This is navigation state only. It holds no mail, touches no account storage, and is not
 * consulted by anything that syncs or talks to a server. Losing this file costs the user a
 * remembered position and nothing else, so it is deliberately a small [SharedPreferences]
 * file of its own rather than a row in the account store.
 *
 * It is a plain object rather than a Koin binding on purpose: it is read from `legacy`, which is
 * built into three applications, and a binding missing in any of them would be a runtime crash
 * for a feature this minor.
 *
 * Two rules that read alike but are not the same, and must not be merged:
 *
 *  - On a cold start the application opens the **last used account at its default folder**
 *    (its Inbox). See [lastActiveAccountUuid].
 *  - Switching between accounts **inside** a running application returns to the folder that
 *    account was last left in. See [lastFolderId].
 */
object CivionNavigationState {

    private const val PREFERENCES_NAME = "civion_navigation_state"

    private const val KEY_LAST_ACCOUNT = "last_active_account"
    private const val KEY_FOLDER_PREFIX = "last_folder."

    private fun preferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    // -------------------------------------------------------------- last used account ---

    /**
     * Records that [accountUuid] is the account the user is looking at, in folder [folderId].
     *
     * Called from every path that puts a single account's folder on screen, so it also covers
     * arriving from a notification. Writes are skipped when nothing changed, because this runs
     * on every message list that is displayed.
     */
    fun recordActiveAccount(context: Context, accountUuid: String, folderId: Long) {
        if (accountUuid.isEmpty()) return

        val preferences = preferences(context)
        val folderKey = KEY_FOLDER_PREFIX + accountUuid

        val accountUnchanged = preferences.getString(KEY_LAST_ACCOUNT, null) == accountUuid
        val folderUnchanged = preferences.getLong(folderKey, NO_FOLDER) == folderId
        if (accountUnchanged && folderUnchanged) return

        preferences.edit()
            .putString(KEY_LAST_ACCOUNT, accountUuid)
            .putLong(folderKey, folderId)
            .apply()
    }

    /**
     * The account the user last used, or `null` if none was recorded yet.
     *
     * The caller must check that the account still exists and fall back to the ordinary
     * upstream behaviour if it does not; this class cannot see the account store.
     */
    fun lastActiveAccountUuid(context: Context): String? =
        preferences(context).getString(KEY_LAST_ACCOUNT, null)

    /**
     * The folder [accountUuid] was last left in, or `null` if none was recorded.
     *
     * The caller must check that the folder still exists. A folder can disappear between two
     * runs — renamed on the server, unsubscribed, or removed — and a stale id would otherwise
     * open an empty list.
     */
    fun lastFolderId(context: Context, accountUuid: String): Long? =
        preferences(context)
            .getLong(KEY_FOLDER_PREFIX + accountUuid, NO_FOLDER)
            .takeIf { it != NO_FOLDER }

    private const val NO_FOLDER = -1L
}
