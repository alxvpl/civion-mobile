package nl.civion.mobile.provider

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.fsck.k9.ui.settings.notificationactions.NotificationActionsSettingsActivity
import nl.civion.mobile.BuildConfig
import nl.civion.mobile.ui.settings.CivionSettingsNavigator

/**
 * The upstream screens CIVION's Settings leads to. Bound here because the application is the
 * one module that may know both CIVION's screens and Thunderbird's.
 */
internal class CivionSettingsNavigatorImpl : CivionSettingsNavigator {

    override val appVersion: String = BuildConfig.VERSION_NAME

    override fun openAccountSettings(activity: Activity, accountUuid: String) {
        AccountSettingsActivity.start(activity, accountUuid)
    }

    override fun openNotificationActions(activity: Activity) {
        NotificationActionsSettingsActivity.start(activity)
    }

    override fun openUrl(activity: Activity, url: String) {
        activity.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
