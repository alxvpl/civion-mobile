package nl.civion.mobile.ui.settings

import android.app.Activity

/**
 * Where the Settings screen sends the user for what it does not hold itself.
 *
 * The screen keeps the product's own settings; everything deeper - an account's servers and
 * folders, the order of notification actions - stays upstream's. Those screens live in modules
 * this one must not depend on, so the application binds the way there.
 */
interface CivionSettingsNavigator {
    /** The version the About group names, as the application declares it. */
    val appVersion: String

    fun openAccountSettings(activity: Activity, accountUuid: String)

    fun openNotificationActions(activity: Activity)

    fun openUrl(activity: Activity, url: String)
}
