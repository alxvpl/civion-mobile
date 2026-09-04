package nl.civion.mobile.provider

import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import nl.civion.mobile.R

class CivionAppNotificationIconProvider : NotificationIconResourceProvider {
    override val pushNotificationIcon: Int
        get() = R.drawable.ic_notification_civion
}
